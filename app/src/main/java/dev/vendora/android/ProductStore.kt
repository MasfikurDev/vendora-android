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

    fun getProducts(shopId: String): List<Product> = runCatching {
        val array = JSONArray(preferences.getString(KEY_PRODUCTS, "[]"))
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                if (item.optString("shopId") == shopId) {
                    add(
                        Product(
                            id = item.getString("id"),
                            shopId = item.getString("shopId"),
                            name = item.getString("name"),
                            sku = item.getString("sku"),
                            quantity = item.getInt("quantity"),
                            purchasePricePoisha = item.getLong("purchasePricePoisha"),
                            sellingPricePoisha = item.getLong("sellingPricePoisha"),
                        )
                    )
                }
            }
        }
    }.getOrDefault(emptyList())

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
        if (index >= 0) all[index] = normalized else all.add(normalized)
        writeAll(all)
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
        fun newId(): String = UUID.randomUUID().toString()
    }
}
