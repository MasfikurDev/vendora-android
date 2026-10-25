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

    /** Adjust stock by a non-zero delta without allowing negative inventory. */
    fun adjustStock(shopId: String, productId: String, delta: Int): Product {
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
        writeAll(all)
        return updated
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
        val array = JSONArray()
        products.forEach { p ->
            array.put(JSONObject()
                .put("id", p.id)
                .put("shopId", p.shopId)
                .put("name", p.name)
                .put("sku", p.sku)
                .put("quantity", p.quantity)
                .put("purchasePricePoisha", p.purchasePricePoisha)
                .put("sellingPricePoisha", p.sellingPricePoisha))
        }
        preferences.edit().putString(KEY_PRODUCTS, array.toString()).apply()
    }

    companion object {
        private const val KEY_PRODUCTS = "products"
        const val LOW_STOCK_THRESHOLD = 5
        fun newId(): String = UUID.randomUUID().toString()
    }
}
