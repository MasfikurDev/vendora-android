package dev.vendora.android

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

/**
 * Room schema for Vendora's offline-first local database.
 * Legacy SharedPreferences data is imported before the app switches to Room-backed stores.
 */
@Entity(tableName = "shops")
data class ShopEntity(
    @PrimaryKey val id: String,
    val name: String,
    val businessType: String,
)

@Entity(
    tableName = "products",
    indices = [Index(value = ["shopId"]), Index(value = ["shopId", "sku"])],
)
data class ProductEntity(
    @PrimaryKey val id: String,
    val shopId: String,
    val name: String,
    val sku: String,
    val quantity: Int,
    val purchasePricePoisha: Long,
    val sellingPricePoisha: Long,
)

@Entity(
    tableName = "stock_movements",
    indices = [
        Index(value = ["shopId", "timestampMillis"]),
        Index(value = ["shopId", "productId", "timestampMillis"]),
    ],
)
data class StockMovementEntity(
    @PrimaryKey val id: String,
    val shopId: String,
    val productId: String,
    val productName: String,
    val delta: Int,
    val quantityBefore: Int,
    val quantityAfter: Int,
    val reason: String,
    val timestampMillis: Long,
)

@Entity(
    tableName = "sale_lines",
    indices = [
        Index(value = ["shopId", "timestampMillis"]),
        Index(value = ["shopId", "productId", "timestampMillis"]),
    ],
)
data class SaleLineEntity(
    @PrimaryKey val id: String,
    val shopId: String,
    val productId: String,
    val productName: String,
    val sku: String,
    val quantity: Int,
    val unitPricePoisha: Long,
    val unitCostPoisha: Long,
    val totalPoisha: Long,
    val timestampMillis: Long,
)

@Dao
interface VendoraDao {
    @Query("SELECT * FROM shops ORDER BY name COLLATE NOCASE")
    suspend fun getShops(): List<ShopEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertShop(shop: ShopEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertShop(shop: ShopEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun importShops(shops: List<ShopEntity>)

    @Query("SELECT * FROM products WHERE shopId = :shopId ORDER BY name COLLATE NOCASE")
    suspend fun getProducts(shopId: String): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :productId AND shopId = :shopId LIMIT 1")
    suspend fun getProduct(shopId: String, productId: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun importProducts(products: List<ProductEntity>)

    @Query("SELECT * FROM stock_movements WHERE shopId = :shopId ORDER BY timestampMillis DESC")
    suspend fun getStockMovements(shopId: String): List<StockMovementEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStockMovements(movements: List<StockMovementEntity>)

    @Query("SELECT * FROM sale_lines WHERE shopId = :shopId ORDER BY timestampMillis DESC")
    suspend fun getSaleLines(shopId: String): List<SaleLineEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSaleLines(sales: List<SaleLineEntity>)
}

@Database(
    entities = [
        ShopEntity::class,
        ProductEntity::class,
        StockMovementEntity::class,
        SaleLineEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class VendoraDatabase : RoomDatabase() {
    abstract fun vendoraDao(): VendoraDao
}
