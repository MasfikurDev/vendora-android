package dev.vendora.android

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import org.json.JSONArray
import java.io.IOException
import java.util.UUID

object VendoraDatabaseProvider {
    @Volatile private var instance: VendoraDatabase? = null

    fun get(context: Context): VendoraDatabase =
        instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                VendoraDatabase::class.java,
                "vendora.db",
            ).build().also { instance = it }
        }
}

class LegacyDataImporter(context: Context) {
    private val appContext = context.applicationContext
    private val shopPreferences = appContext.getSharedPreferences("vendora_shops", Context.MODE_PRIVATE)
    private val productPreferences = appContext.getSharedPreferences("vendora_products", Context.MODE_PRIVATE)
    private val migrationPreferences = appContext.getSharedPreferences("vendora_migration", Context.MODE_PRIVATE)

    suspend fun importIfNeeded(database: VendoraDatabase) {
        if (migrationPreferences.getBoolean(KEY_IMPORTED, false)) return
        val shops = readLegacyShops()
        val products = readLegacyProducts()
        database.withTransaction {
            database.vendoraDao().importShops(shops)
            database.vendoraDao().importProducts(products)
        }
        if (!migrationPreferences.edit().putBoolean(KEY_IMPORTED, true).commit()) {
            throw IOException("Data was imported, but the migration marker could not be saved. Retry on next launch.")
        }
    }

    private fun readLegacyShops(): List<ShopEntity> {
        val encoded = shopPreferences.getString("shops", null) ?: return emptyList()
        val array = try { JSONArray(encoded) } catch (error: Exception) {
            throw IOException("Saved shop data could not be read. Original data was preserved.", error)
        }
        return buildList {
            for (index in 0 until array.length()) {
                val item = try { array.getJSONObject(index) } catch (error: Exception) {
                    throw IOException("Saved shop record $index is invalid. Original data was preserved.", error)
                }
                val id = item.optString("id").trim()
                val name = item.optString("name").trim()
                val type = item.optString("businessType").trim()
                if (id.isBlank() || name.isBlank() || type.isBlank()) {
                    throw IOException("Saved shop record $index is missing required fields. Original data was preserved.")
                }
                add(ShopEntity(id, name, type))
            }
        }
    }

    private fun readLegacyProducts(): List<ProductEntity> {
        val encoded = productPreferences.getString("products", "[]") ?: "[]"
        val array = try { JSONArray(encoded) } catch (error: Exception) {
            throw IOException("Saved product data could not be read. Original data was preserved.", error)
        }
        return buildList {
            for (index in 0 until array.length()) {
                val item = try { array.getJSONObject(index) } catch (error: Exception) {
                    throw IOException("Saved product record $index is invalid. Original data was preserved.", error)
                }
                val id = item.optString("id").trim()
                val shopId = item.optString("shopId").trim()
                val name = item.optString("name").trim()
                val sku = item.optString("sku").trim()
                val quantity = if (item.has("quantity")) item.optInt("quantity", -1) else -1
                val cost = if (item.has("purchasePricePoisha")) item.optLong("purchasePricePoisha", -1L) else -1L
                val price = if (item.has("sellingPricePoisha")) item.optLong("sellingPricePoisha", -1L) else -1L
                if (id.isBlank() || shopId.isBlank() || name.isBlank() || quantity < 0 || cost < 0L || price < 0L) {
                    throw IOException("Saved product record $index is invalid. Original data was preserved.")
                }
                add(ProductEntity(id, shopId, name, sku, quantity, cost, price))
            }
        }
    }

    private companion object { const val KEY_IMPORTED = "legacy_json_imported_v1" }
}

class VendoraRepository(
    internal val database: VendoraDatabase,
) {
    private val dao get() = database.vendoraDao()

    suspend fun initializeFromLegacy(context: Context) = LegacyDataImporter(context).importIfNeeded(database)
    suspend fun getShops(): List<ShopEntity> = dao.getShops()

    suspend fun saveShop(shop: ShopEntity) {
        require(shop.id.isNotBlank() && shop.name.isNotBlank() && shop.businessType.isNotBlank()) {
            "A shop name and business type are required."
        }
        dao.upsertShop(shop.copy(name = shop.name.trim(), businessType = shop.businessType.trim()))
    }

    suspend fun getSaleLines(shopId: String): List<SaleLineEntity> {
        require(shopId.isNotBlank()) { "A shop is required." }
        return dao.getSaleLines(shopId)
    }

    suspend fun getStockMovements(shopId: String): List<StockMovementEntity> {
        require(shopId.isNotBlank()) { "A shop is required." }
        return dao.getStockMovements(shopId)
    }

    suspend fun getProducts(shopId: String): List<ProductEntity> {
        require(shopId.isNotBlank()) { "A shop is required." }
        return dao.getProducts(shopId)
    }

    suspend fun getProduct(shopId: String, productId: String): ProductEntity? {
        require(shopId.isNotBlank() && productId.isNotBlank()) { "A shop and product are required." }
        return dao.getProduct(shopId, productId)
    }

    suspend fun saveProduct(product: ProductEntity, timestampMillis: Long = System.currentTimeMillis()) {
        require(product.shopId.isNotBlank() && product.name.isNotBlank()) { "A shop and product name are required." }
        require(product.quantity >= 0) { "Stock cannot be negative." }
        require(product.purchasePricePoisha >= 0L && product.sellingPricePoisha >= 0L) { "Prices cannot be negative." }
        val normalized = product.copy(name = product.name.trim(), sku = product.sku.trim())
        database.withTransaction {
            val duplicateSku = dao.getProducts(normalized.shopId).any {
                it.id != normalized.id && normalized.sku.isNotBlank() &&
                    it.sku.equals(normalized.sku, ignoreCase = true)
            }
            require(!duplicateSku) { "This SKU already exists in this shop." }
            val existing = dao.getProduct(normalized.shopId, normalized.id)
            if (existing == null) {
                dao.insertProduct(normalized)
                if (normalized.quantity > 0) {
                    dao.insertStockMovements(listOf(StockMovementEntity(
                        id = UUID.randomUUID().toString(),
                        shopId = normalized.shopId,
                        productId = normalized.id,
                        productName = normalized.name,
                        delta = normalized.quantity,
                        quantityBefore = 0,
                        quantityAfter = normalized.quantity,
                        reason = "Initial stock",
                        timestampMillis = timestampMillis,
                    )))
                }
            } else {
                if (existing.quantity != normalized.quantity) {
                    dao.insertStockMovements(listOf(StockMovementEntity(
                        id = UUID.randomUUID().toString(),
                        shopId = normalized.shopId,
                        productId = normalized.id,
                        productName = normalized.name,
                        delta = normalized.quantity - existing.quantity,
                        quantityBefore = existing.quantity,
                        quantityAfter = normalized.quantity,
                        reason = "Inventory edit",
                        timestampMillis = timestampMillis,
                    )))
                }
                dao.updateProduct(normalized)
            }
        }
    }

    suspend fun deleteProduct(shopId: String, productId: String) {
        require(shopId.isNotBlank() && productId.isNotBlank()) { "A shop and product are required." }
        dao.deleteProduct(shopId, productId)
    }

    suspend fun recordStockAdjustment(
        shopId: String,
        productId: String,
        newQuantity: Int,
        reason: String,
        timestampMillis: Long = System.currentTimeMillis(),
    ) {
        require(shopId.isNotBlank() && productId.isNotBlank()) { "A shop and product are required." }
        require(newQuantity >= 0) { "Stock cannot be negative." }
        require(reason.isNotBlank()) { "A reason is required for a stock adjustment." }
        database.withTransaction {
            val product = dao.getProduct(shopId, productId)
                ?: throw IllegalArgumentException("Product was not found in this shop.")
            val delta = newQuantity - product.quantity
            if (delta != 0) {
                dao.updateProduct(product.copy(quantity = newQuantity))
                dao.insertStockMovements(listOf(StockMovementEntity(
                    id = UUID.randomUUID().toString(), shopId = shopId, productId = product.id,
                    productName = product.name, delta = delta, quantityBefore = product.quantity,
                    quantityAfter = newQuantity, reason = reason.trim(), timestampMillis = timestampMillis,
                )))
            }
        }
    }
}
