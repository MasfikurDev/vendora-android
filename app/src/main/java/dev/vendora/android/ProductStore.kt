package dev.vendora.android

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Product(
    val id: String,
    val shopId: String,
    val name: String,
    val sku: String,
    val quantity: Int,
    val purchasePricePoisha: Long,
    val sellingPricePoisha: Long,
)

data class StockMovement(
    val id: String, val shopId: String, val productId: String, val productName: String,
    val delta: Int, val quantityBefore: Int, val quantityAfter: Int, val reason: String,
    val timestampMillis: Long,
)

class ProductStore(context: Context) {
    private val preferences = context.getSharedPreferences("vendora_products", Context.MODE_PRIVATE)

    fun getProducts(shopId: String): List<Product> = readAll().filter { it.shopId == shopId }

    fun saveProduct(product: Product) {
        require(product.shopId.isNotBlank()) { "A shop is required." }
        require(product.name.isNotBlank()) { "Product name is required." }
        require(product.quantity >= 0) { "Stock cannot be negative." }
        require(product.purchasePricePoisha >= 0 && product.sellingPricePoisha >= 0) {
            "Prices cannot be negative."
        }
        val all = readAll().toMutableList()
        val duplicateSku = all.any {
            it.shopId == product.shopId &&
                it.sku.isNotBlank() &&
                it.sku.equals(product.sku.trim(), ignoreCase = true) &&
                it.id != product.id
        }
        require(!duplicateSku) { "This SKU already exists in this shop." }
        val normalized = product.copy(name = product.name.trim(), sku = product.sku.trim())
        val index = all.indexOfFirst { it.id == normalized.id }
        if (index >= 0) {
            require(all[index].shopId == normalized.shopId) { "A product cannot be moved between shops." }
            all[index] = normalized
        } else {
            all.add(normalized)
        }
        writeAll(all)
    }

    fun getStockMovements(shopId: String, productId: String? = null): List<StockMovement> =
        readMovements().filter { it.shopId == shopId && (productId == null || it.productId == productId) }
            .sortedByDescending { it.timestampMillis }

    /** Adjust stock and persist an audit entry. */
    fun adjustStock(shopId: String, productId: String, delta: Int, reason: String = "Manual adjustment"): Product {
        require(delta != 0) { "Stock adjustment cannot be zero." }
        val all = readAll().toMutableList()
        val index = all.indexOfFirst { it.shopId == shopId && it.id == productId }
        require(index >= 0) { "Product not found in this shop." }
        val current = all[index]
        val updatedQuantity = current.quantity.toLong() + delta.toLong()
        require(updatedQuantity >= 0L && updatedQuantity <= Int.MAX_VALUE) {
            "Stock adjustment would produce an invalid quantity."
        }
        val updated = current.copy(quantity = updatedQuantity.toInt())
        all[index] = updated
        val movement = StockMovement(UUID.randomUUID().toString(), shopId, productId, current.name,
            delta, current.quantity, updated.quantity, reason.trim().ifBlank { "Manual adjustment" },
            System.currentTimeMillis())
        check(preferences.edit().putString(KEY_PRODUCTS, encodeProducts(all))
            .putString(KEY_MOVEMENTS, encodeMovements(readMovements() + movement)).commit()) {
            "Could not save stock adjustment history."
        }
        return updated
    }

    fun getSales(shopId: String): List<SaleRecord> =
        readSales().filter { it.shopId == shopId }.sortedByDescending { it.timestampMillis }

    /** Atomically persist the stock decrement, movement audit entry, and sale record. */
    fun recordSale(shopId: String, productId: String, quantity: Int): SaleRecord {
        require(quantity > 0) { "Sale quantity must be greater than zero." }
        val products = readAll().toMutableList()
        val index = products.indexOfFirst { it.shopId == shopId && it.id == productId }
        require(index >= 0) { "Product not found in this shop." }
        val current = products[index]
        require(quantity <= current.quantity) { "Not enough stock. Available: ${current.quantity}." }
        val total = Math.multiplyExact(current.sellingPricePoisha, quantity.toLong())
        val saleId = UUID.randomUUID().toString()
        val updated = current.copy(quantity = current.quantity - quantity)
        products[index] = updated
        val movement = StockMovement(
            UUID.randomUUID().toString(), shopId, productId, current.name,
            -quantity, current.quantity, updated.quantity, "Sale $saleId", System.currentTimeMillis()
        )
        val sale = SaleRecord(
            id = saleId, shopId = shopId, productId = productId, productName = current.name,
            sku = current.sku, quantity = quantity, unitPricePoisha = current.sellingPricePoisha,
            totalPoisha = total, timestampMillis = System.currentTimeMillis()
        )
        check(preferences.edit()
            .putString(KEY_PRODUCTS, encodeProducts(products))
            .putString(KEY_MOVEMENTS, encodeMovements(readMovements() + movement))
            .putString(KEY_SALES, encodeSales(readSales() + sale))
            .commit()) { "Could not save sale and stock changes. No sale was confirmed." }
        return sale
    }

    private fun readSales(): List<SaleRecord> = runCatching {
        val array = JSONArray(preferences.getString(KEY_SALES, "[]"))
        buildList {
            for (i in 0 until array.length()) {
                val x = array.getJSONObject(i)
                add(SaleRecord(
                    id = x.getString("id"), shopId = x.getString("shopId"),
                    productId = x.getString("productId"), productName = x.getString("productName"),
                    sku = x.optString("sku"), quantity = x.getInt("quantity"),
                    unitPricePoisha = x.getLong("unitPricePoisha"), totalPoisha = x.getLong("totalPoisha"),
                    timestampMillis = x.getLong("timestampMillis")
                ))
            }
        }
    }.getOrDefault(emptyList())

    private fun encodeSales(sales: List<SaleRecord>): String {
        val array = JSONArray()
        sales.forEach { s -> array.put(JSONObject()
            .put("id", s.id).put("shopId", s.shopId).put("productId", s.productId)
            .put("productName", s.productName).put("sku", s.sku).put("quantity", s.quantity)
            .put("unitPricePoisha", s.unitPricePoisha).put("totalPoisha", s.totalPoisha)
            .put("timestampMillis", s.timestampMillis)) }
        return array.toString()
    }

    fun deleteProduct(shopId: String, productId: String) {
        writeAll(readAll().filterNot { it.shopId == shopId && it.id == productId })
    }

    private fun readAll(): List<Product> = runCatching {
        val array = JSONArray(preferences.getString(KEY_PRODUCTS, "[]"))
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(Product(
                    id = item.getString("id"),
                    shopId = item.getString("shopId"),
                    name = item.getString("name"),
                    sku = item.optString("sku"),
                    quantity = item.getInt("quantity"),
                    purchasePricePoisha = item.getLong("purchasePricePoisha"),
                    sellingPricePoisha = item.getLong("sellingPricePoisha"),
                ))
            }
        }
    }.getOrDefault(emptyList())

    private fun writeAll(products: List<Product>) {
        preferences.edit().putString(KEY_PRODUCTS, encodeProducts(products)).apply()
    }

    private fun encodeProducts(products: List<Product>): String {
        val array = JSONArray()
        products.forEach { p -> array.put(JSONObject().put("id", p.id).put("shopId", p.shopId)
            .put("name", p.name).put("sku", p.sku).put("quantity", p.quantity)
            .put("purchasePricePoisha", p.purchasePricePoisha).put("sellingPricePoisha", p.sellingPricePoisha)) }
        return array.toString()
    }

    private fun readMovements(): List<StockMovement> = runCatching {
        val array = JSONArray(preferences.getString(KEY_MOVEMENTS, "[]"))
        buildList { for (i in 0 until array.length()) { val x = array.getJSONObject(i)
            add(StockMovement(x.getString("id"), x.getString("shopId"), x.getString("productId"),
                x.optString("productName"), x.getInt("delta"), x.getInt("quantityBefore"),
                x.getInt("quantityAfter"), x.optString("reason", "Manual adjustment"), x.getLong("timestampMillis")))
        } }
    }.getOrDefault(emptyList())

    private fun encodeMovements(movements: List<StockMovement>): String {
        val array = JSONArray()
        movements.forEach { m -> array.put(JSONObject().put("id", m.id).put("shopId", m.shopId)
            .put("productId", m.productId).put("productName", m.productName).put("delta", m.delta)
            .put("quantityBefore", m.quantityBefore).put("quantityAfter", m.quantityAfter)
            .put("reason", m.reason).put("timestampMillis", m.timestampMillis)) }
        return array.toString()
    }

    companion object {
        private const val KEY_PRODUCTS = "products"
        private const val KEY_MOVEMENTS = "stock_movements"
        private const val KEY_SALES = "sales"
        const val LOW_STOCK_THRESHOLD = 5
        fun newId(): String = UUID.randomUUID().toString()
    }
}
