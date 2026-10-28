package dev.vendora.android

import androidx.room.withTransaction
import java.util.UUID

data class CheckoutLine(
    val productId: String,
    val quantity: Int,
    val unitPricePoisha: Long? = null,
)

data class CheckoutResult(
    val shopId: String,
    val saleId: String,
    val lineCount: Int,
    val totalPoisha: Long,
    val timestampMillis: Long,
)

/**
 * Atomic multi-product checkout. Sale snapshots, stock deductions and audit
 * movements either commit together or roll back together.
 */
suspend fun VendoraRepository.checkout(
    shopId: String,
    cart: List<CheckoutLine>,
    timestampMillis: Long = System.currentTimeMillis(),
): CheckoutResult {
    require(shopId.isNotBlank()) { "A shop is required." }
    require(cart.isNotEmpty()) { "The cart is empty." }
    require(cart.all { it.productId.isNotBlank() && it.quantity > 0 }) {
        "Every cart line must have a product and a positive quantity."
    }
    require(cart.map { it.productId }.distinct().size == cart.size) {
        "Each product should appear only once in the cart."
    }

    val database = this.database
    val dao = database.vendoraDao()

    return database.withTransaction {
        // Validate the entire cart before making any changes.
        val prepared = cart.map { line ->
            val product = dao.getProduct(shopId, line.productId)
                ?: throw IllegalArgumentException("A cart product was not found in this shop.")
            require(line.quantity <= product.quantity) {
                "Not enough stock for " + product.name + ". Available: " + product.quantity + "."
            }
            val price = line.unitPricePoisha ?: product.sellingPricePoisha
            require(price >= 0L) { "Selling price cannot be negative." }
            val lineTotal = try {
                Math.multiplyExact(price, line.quantity.toLong())
            } catch (error: ArithmeticException) {
                throw IllegalArgumentException("The line total is too large.", error)
            }
            Triple(line, product, price to lineTotal)
        }

        val saleId = UUID.randomUUID().toString()
        val sales = prepared.map { (line, product, amounts) ->
            SaleLineEntity(
                id = UUID.randomUUID().toString(),
                saleId = saleId,
                shopId = shopId,
                productId = product.id,
                productName = product.name,
                sku = product.sku,
                quantity = line.quantity,
                unitPricePoisha = amounts.first,
                unitCostPoisha = product.purchasePricePoisha,
                totalPoisha = amounts.second,
                timestampMillis = timestampMillis,
            )
        }
        val movements = prepared.map { (line, product, _) ->
            StockMovementEntity(
                id = UUID.randomUUID().toString(),
                shopId = shopId,
                productId = product.id,
                productName = product.name,
                delta = -line.quantity,
                quantityBefore = product.quantity,
                quantityAfter = product.quantity - line.quantity,
                reason = "Sale",
                timestampMillis = timestampMillis,
            )
        }
        val total = try {
            sales.fold(0L) { sum, sale -> Math.addExact(sum, sale.totalPoisha) }
        } catch (error: ArithmeticException) {
            throw IllegalArgumentException("The cart total is too large.", error)
        }

        dao.insertSale(
            SaleEntity(
                id = saleId,
                shopId = shopId,
                timestampMillis = timestampMillis,
                totalPoisha = total,
                itemCount = sales.sumOf { it.quantity },
            ),
        )
        dao.insertSaleLines(sales)
        sales.forEachIndexed { index, sale ->
            val product = prepared[index].second
            dao.updateProduct(product.copy(quantity = product.quantity - sale.quantity))
        }
        dao.insertStockMovements(movements)

        CheckoutResult(shopId, saleId, sales.size, total, timestampMillis)
    }
}
