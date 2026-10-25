package dev.vendora.android

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class SaleRecord(
    val id: String,
    val shopId: String,
    val productId: String,
    val productName: String,
    val sku: String,
    val quantity: Int,
    val unitPricePoisha: Long,
    val totalPoisha: Long,
    val timestampMillis: Long,
)

class SalesStore(context: Context) {
    private val preferences = context.getSharedPreferences("vendora_sales", Context.MODE_PRIVATE)

    fun getSales(shopId: String): List<SaleRecord> =
        readAll().filter { it.shopId == shopId }.sortedByDescending { it.timestampMillis }

    /**
     * Records a single-product sale after reducing stock. This initial version is local/offline;
     * multi-line transactional checkout and returns are intentionally deferred.
     */
    fun recordSale(productStore: ProductStore, shopId: String, productId: String, quantity: Int): SaleRecord {
        require(quantity > 0) { "Sale quantity must be greater than zero." }
        val product = productStore.getProducts(shopId).firstOrNull { it.id == productId }
            ?: throw IllegalArgumentException("Product not found in this shop.")
        require(quantity <= product.quantity) { "Not enough stock. Available: ${product.quantity}." }
        require(product.sellingPricePoisha >= 0L) { "Product price is invalid." }
        val total = Math.multiplyExact(product.sellingPricePoisha, quantity.toLong())

        // Stock adjustments already create an audit entry; sale IDs make the reason traceable.
        val saleId = UUID.randomUUID().toString()
        productStore.adjustStock(shopId, productId, -quantity, "Sale $saleId")
        val sale = SaleRecord(
            id = saleId,
            shopId = shopId,
            productId = product.id,
            productName = product.name,
            sku = product.sku,
            quantity = quantity,
            unitPricePoisha = product.sellingPricePoisha,
            totalPoisha = total,
            timestampMillis = System.currentTimeMillis(),
        )
        val updated = readAll() + sale
        check(preferences.edit().putString(KEY_SALES, encode(updated)).commit()) {
            "Stock was reduced, but the sale record could not be saved. Check inventory and retry only after reviewing stock history."
        }
        return sale
    }

    private fun readAll(): List<SaleRecord> = runCatching {
        val array = JSONArray(preferences.getString(KEY_SALES, "[]"))
        buildList {
            for (i in 0 until array.length()) {
                val x = array.getJSONObject(i)
                add(SaleRecord(
                    id = x.getString("id"),
                    shopId = x.getString("shopId"),
                    productId = x.getString("productId"),
                    productName = x.getString("productName"),
                    sku = x.optString("sku"),
                    quantity = x.getInt("quantity"),
                    unitPricePoisha = x.getLong("unitPricePoisha"),
                    totalPoisha = x.getLong("totalPoisha"),
                    timestampMillis = x.getLong("timestampMillis"),
                ))
            }
        }
    }.getOrDefault(emptyList())

    private fun encode(sales: List<SaleRecord>): String {
        val array = JSONArray()
        sales.forEach { sale ->
            array.put(JSONObject()
                .put("id", sale.id)
                .put("shopId", sale.shopId)
                .put("productId", sale.productId)
                .put("productName", sale.productName)
                .put("sku", sale.sku)
                .put("quantity", sale.quantity)
                .put("unitPricePoisha", sale.unitPricePoisha)
                .put("totalPoisha", sale.totalPoisha)
                .put("timestampMillis", sale.timestampMillis))
        }
        return array.toString()
    }

    companion object {
        private const val KEY_SALES = "sales"
    }
}
