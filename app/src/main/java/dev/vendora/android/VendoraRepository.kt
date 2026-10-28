package dev.vendora.android

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import org.json.JSONArray
import java.io.IOException

/**
 * Opens the single on-device database. The existing SharedPreferences keys are
 * intentionally retained for the legacy importer and active-shop selection.
 */
object VendoraDatabaseProvider {
    @Volatile
    private var instance: VendoraDatabase? = null

    fun get(context: Context): VendoraDatabase =
        instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                VendoraDatabase::class.java,
                "vendora.db",
            ).build().also { instance = it }
        }
}

/**
 * Imports legacy JSON without deleting it. Invalid records stop the import rather
 * than silently turning a partial migration into a successful one.
 */
class LegacyDataImporter(context: Context) {
    private val appContext = context.applicationContext
    private val shopPreferences =
        appContext.getSharedPreferences("vendora_shops", Context.MODE_PRIVATE)
    private val productPreferences =
        appContext.getSharedPreferences("vendora_products", Context.MODE_PRIVATE)
    private val migrationPreferences =
        appContext.getSharedPreferences("vendora_migration", Context.MODE_PRIVATE)

    suspend fun importIfNeeded(database: VendoraDatabase) {
        if (migrationPreferences.getBoolean(KEY_IMPORTED, false)) return

        val shops = readLegacyShops()
        val products = readLegacyProducts()

        database.withTransaction {
            database.vendoraDao().importShops(shops)
            database.vendoraDao().importProducts(products)
        }

        // Mark only after the Room transaction commits. If this preference write
        // is interrupted, the IGNORE conflict policy makes a retry idempotent.
        if (!migrationPreferences.edit().putBoolean(KEY_IMPORTED, true).commit()) {
            throw IOException("Data was imported, but the migration marker could not be saved. Retry on next launch.")
        }
    }

    private fun readLegacyShops(): List<ShopEntity> {
        val encoded = shopPreferences.getString("shops", null) ?: return emptyList()
        val array = try {
            JSONArray(encoded)
        } catch (error: Exception) {
            throw IOException("Saved shop data could not be read. Original data was preserved.", error)
        }
        return buildList {
            for (index in 0 until array.length()) {
                val item = try {
                    array.getJSONObject(index)
                } catch (error: Exception) {
                    throw IOException("Saved shop record $index is invalid. Original data was preserved.", error)
                }
                val id = item.optString("id").trim()
                val name = item.optString("name").trim()
                val type = item.optString("businessType").trim()
                if (id.isBlank() || name.isBlank() || type.isBlank()) {
                    throw IOException("Saved shop record $index is missing required fields. Original data was preserved.")
                }
                add(ShopEntity(id = id, name = name, businessType = type))
            }
        }
    }

    private fun readLegacyProducts(): List<ProductEntity> {
        val encoded = productPreferences.getString("products", "[]") ?: "[]"
        val array = try {
            JSONArray(encoded)
        } catch (error: Exception) {
            throw IOException("Saved product data could not be read. Original data was preserved.", error)
        }
        return buildList {
            for (index in 0 until array.length()) {
                val item = try {
                    array.getJSONObject(index)
                } catch (error: Exception) {
                    throw IOException("Saved product record $index is invalid. Original data was preserved.", error)
                }
                val id = item.optString("id").trim()
                val shopId = item.optString("shopId").trim()
                val name = item.optString("name").trim()
                val sku = item.optString("sku").trim()
                val quantity = if (item.has("quantity")) item.optInt("quantity", -1) else -1
                val cost = if (item.has("purchasePricePoisha")) item.optLong("purchasePricePoisha", -1L) else -1L
                val price = if (item.has("sellingPricePoisha")) item.optLong("sellingPricePoisha", -1L) else -1L
                if (id.isBlank() || shopId.isBlank() || name.isBlank() ||
                    quantity < 0 || cost < 0L || price < 0L
                ) {
                    throw IOException("Saved product record $index is invalid. Original data was preserved.")
                }
                add(
                    ProductEntity(
                        id = id,
                        shopId = shopId,
                        name = name,
                        sku = sku,
                        quantity = quantity,
                        purchasePricePoisha = cost,
                        sellingPricePoisha = price,
                    )
                )
            }
        }
    }

    private companion object {
        const val KEY_IMPORTED = "legacy_json_imported_v1"
    }
}

/** A small shop-scoped data boundary for screens and future checkout logic. */
class VendoraRepository(
    private val database: VendoraDatabase,
) {
    private val dao get() = database.vendoraDao()

    suspend fun initializeFromLegacy(context: Context) {
        LegacyDataImporter(context).importIfNeeded(database)
    }

    suspend fun getShops(): List<ShopEntity> = dao.getShops()

    suspend fun getProducts(shopId: String): List<ProductEntity> {
        require(shopId.isNotBlank()) { "A shop is required." }
        return dao.getProducts(shopId)
    }

    suspend fun getProduct(shopId: String, productId: String): ProductEntity? {
        require(shopId.isNotBlank()) { "A shop is required." }
        require(productId.isNotBlank()) { "A product is required." }
        return dao.getProduct(shopId, productId)
    }

    suspend fun saveProduct(product: ProductEntity) {
        require(product.shopId.isNotBlank()) { "A shop is required." }
        require(product.name.isNotBlank()) { "Product name is required." }
        require(product.quantity >= 0) { "Stock cannot be negative." }
        require(product.purchasePricePoisha >= 0L && product.sellingPricePoisha >= 0L) {
            "Prices cannot be negative."
        }
        val existing = dao.getProduct(product.shopId, product.id)
        if (existing == null) {
            dao.insertProduct(product)
        } else {
            dao.updateProduct(product)
        }
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
                dao.insertStockMovements(
                    listOf(
                        StockMovementEntity(
                            id = java.util.UUID.randomUUID().toString(),
                            shopId = shopId,
                            productId = product.id,
                            productName = product.name,
                            delta = delta,
                            quantityBefore = product.quantity,
                            quantityAfter = newQuantity,
                            reason = reason.trim(),
                            timestampMillis = timestampMillis,
                        )
                    )
                )
            }
        }
    }
}
