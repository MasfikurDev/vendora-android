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
import androidx.room.Update

/**
 * Room schema foundation for Vendora. SharedPreferences remains the legacy source
 * until the repository performs the safe one-time import.
 */
@Entity(tableName = "shops")
data class ShopEntity(
    @PrimaryKey val id: String,
    val name: String,
    val businessType: String,
)

@Entity(
    tableName = "products",
    indices = [Index(value = ["shopId"]), Index(value = ["shopId", "sku"]), Index(value = ["shopId", "barcode"])],
)
data class ProductEntity(
    @PrimaryKey val id: String,
    val shopId: String,
    val name: String,
    val sku: String,
    val quantity: Int,
    val purchasePricePoisha: Long,
    val sellingPricePoisha: Long,
    val brand: String = "",
    val category: String = "",
    val unit: String = "pcs",
    val barcode: String = "",
    val lowStockThreshold: Int = 5,
)

@Entity(
    tableName = "catalog_options",
    indices = [Index(value = ["shopId", "kind", "normalizedValue"], unique = true)],
)
data class CatalogOptionEntity(
    @PrimaryKey val id: String,
    val shopId: String,
    val kind: String,
    val value: String,
    val normalizedValue: String,
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

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun importProducts(products: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCatalogOptions(options: List<CatalogOptionEntity>)

    @Query("SELECT * FROM catalog_options WHERE shopId = :shopId AND kind = :kind ORDER BY value COLLATE NOCASE")
    suspend fun getCatalogOptions(shopId: String, kind: String): List<CatalogOptionEntity>

    @Query("DELETE FROM products WHERE id = :productId AND shopId = :shopId")
    suspend fun deleteProduct(shopId: String, productId: String)

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
        CatalogOptionEntity::class,
        StockMovementEntity::class,
        SaleLineEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class VendoraDatabase : RoomDatabase() {
    abstract fun vendoraDao(): VendoraDao
}
