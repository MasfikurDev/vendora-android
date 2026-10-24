package dev.vendora.android

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Local, offline-first product storage.
 *
 * Every product belongs to one shop. Callers must supply the active shop ID
 * for reads and writes so products from different businesses never mix.
 */
data class Product(
    val id: String,
    val shopId: String,
    val name: String,
    val sku: String,
    val quantity: Int,
    val purchasePricePoisha: Long,
    val sellingPricePoisha: Long,
    val category: String,
)

class ProductStore(context: Context) {
    private val preferences = context.getSharedPreferences("vendora_products", Context.MODE_PRIVATE)

    fun getProducts(shopId: String): List<Product> {
        require(shopId.isNotBlank()) { "Choose a shop first." }
        val encoded = preferences.getString(KEY_PRODUCTS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(encoded)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val product = item.toProductOrNull() ?: continue
                    if (product.shopId == shopId) add(product)
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveProduct(
        shopId: String,
        name: String,
        sku: String,
        quantity: Int,
        purchasePricePoisha: Long,
        sellingPricePoisha: Long,
        category: String,
        productId: String? = null,
    ): Product {
        require(shopId.isNotBlank()) { "Choose a shop first." }
        val normalizedName = name.trim()
        val normalizedSku = sku.trim()
        val normalizedCategory = category.trim().ifEmpty { "General" }
        require(normalizedName.isNotEmpty()) { "Enter a product name." }
        require(quantity >= 0) { "Stock cannot be negative." }
        require(purchasePricePoisha >= 0L) { "Purchase price cannot be negative." }
        require(sellingPricePoisha >= 0L) { "Selling price cannot be negative." }

        val existing = getAllProducts()
        if (normalizedSku.isNotEmpty()) {
            require(existing.none {
                it.shopId == shopId &&
                    it.id != productId &&
                    it.sku.equals(normalizedSku, ignoreCase = true)
            }) { "This SKU is already used in this shop." }
        }

        val existingProduct = productId?.let { id -> existing.firstOrNull { it.id == id } }
        if (productId != null) {
            require(existingProduct != null && existingProduct.shopId == shopId) {
                "This product does not belong to the active shop."
            }
        }

        val product = Product(
            id = existingProduct?.id ?: UUID.randomUUID().toString(),
            shopId = shopId,
            name = normalizedName,
            sku = normalizedSku,
            quantity = quantity,
            purchasePricePoisha = purchasePricePoisha,
            sellingPricePoisha = sellingPricePoisha,
            category = normalizedCategory,
        )
        val updated = existing.filterNot { it.id == product.id } + product
        writeProducts(updated)
        return product
    }

    fun deleteProduct(shopId: String, productId: String) {
        require(shopId.isNotBlank()) { "Choose a shop first." }
        val existing = getAllProducts()
        val target = existing.firstOrNull { it.id == productId }
        require(target != null && target.shopId == shopId) {
            "This product does not belong to the active shop."
        }
        writeProducts(existing.filterNot { it.id == productId })
    }

    private fun getAllProducts(): List<Product> {
        val encoded = preferences.getString(KEY_PRODUCTS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(encoded)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    item.toProductOrNull()?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeProducts(products: List<Product>) {
        val array = JSONArray()
        products.forEach { product ->
            array.put(
                JSONObject()
                    .put("id", product.id)
                    .put("shopId", product.shopId)
                    .put("name", product.name)
                    .put("sku", product.sku)
                    .put("quantity", product.quantity)
                    .put("purchasePricePoisha", product.purchasePricePoisha)
                    .put("sellingPricePoisha", product.sellingPricePoisha)
                    .put("category", product.category)
            )
        }
        preferences.edit().putString(KEY_PRODUCTS, array.toString()).apply()
    }

    private fun JSONObject.toProductOrNull(): Product? {
        val id = optString("id").trim()
        val shopId = optString("shopId").trim()
        val name = optString("name").trim()
        val sku = optString("sku").trim()
        val category = optString("category").trim()
        if (id.isEmpty() || shopId.isEmpty() || name.isEmpty()) return null
        val quantity = optInt("quantity", -1)
        val purchase = optLong("purchasePricePoisha", -1L)
        val selling = optLong("sellingPricePoisha", -1L)
        if (quantity < 0 || purchase < 0L || selling < 0L) return null
        return Product(id, shopId, name, sku, quantity, purchase, selling, category.ifEmpty { "General" })
    }

    companion object {
        private const val KEY_PRODUCTS = "products"
    }
}
