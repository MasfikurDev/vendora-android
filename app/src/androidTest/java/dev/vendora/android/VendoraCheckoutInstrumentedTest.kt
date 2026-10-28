package dev.vendora.android

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import java.io.IOException
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VendoraCheckoutInstrumentedTest {
    private lateinit var database: VendoraDatabase
    private lateinit var repository: VendoraRepository
    private lateinit var context: Context
    private val dao get() = database.vendoraDao()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("vendora_shops", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("vendora_products", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("vendora_migration", Context.MODE_PRIVATE).edit().clear().commit()
        database = Room.inMemoryDatabaseBuilder(context, VendoraDatabase::class.java).build()
        repository = VendoraRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun successfulCheckoutRecordsSaleAndDeductsStockAtomically() = runBlocking {
        seedShopAndProduct(shopId = "shop-a", productId = "product-a", quantity = 5)

        val result = repository.checkout(
            shopId = "shop-a",
            cart = listOf(CheckoutLine(productId = "product-a", quantity = 2)),
            timestampMillis = 100L,
        )

        assertEquals(1, result.lineCount)
        assertEquals(300L, result.totalPoisha)
        assertEquals(3, dao.getProduct("shop-a", "product-a")?.quantity)
        val sale = dao.getSaleLines("shop-a").single()
        assertEquals(2, sale.quantity)
        assertEquals(150L, sale.unitPricePoisha)
        val movement = dao.getStockMovements("shop-a").single()
        assertEquals(-2, movement.delta)
        assertEquals(5, movement.quantityBefore)
        assertEquals(3, movement.quantityAfter)
    }

    @Test
    fun insufficientStockLeavesEveryCartItemUnchanged() = runBlocking {
        seedShopAndProduct(shopId = "shop-a", productId = "product-a", quantity = 5)
        seedShopAndProduct(shopId = "shop-a", productId = "product-b", quantity = 1)

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.checkout(
                    shopId = "shop-a",
                    cart = listOf(
                        CheckoutLine(productId = "product-a", quantity = 2),
                        CheckoutLine(productId = "product-b", quantity = 2),
                    ),
                )
            }
        }

        assertEquals(5, dao.getProduct("shop-a", "product-a")?.quantity)
        assertEquals(1, dao.getProduct("shop-a", "product-b")?.quantity)
        assertEquals(0, dao.getSaleLines("shop-a").size)
        assertEquals(0, dao.getStockMovements("shop-a").size)
    }

    @Test
    fun checkoutCannotSellAnotherShopsProduct() = runBlocking {
        seedShopAndProduct(shopId = "shop-a", productId = "shared-product", quantity = 5)
        seedShopAndProduct(shopId = "shop-b", productId = "other-product", quantity = 7)

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.checkout(
                    shopId = "shop-b",
                    cart = listOf(CheckoutLine(productId = "shared-product", quantity = 1)),
                )
            }
        }

        assertEquals(5, dao.getProduct("shop-a", "shared-product")?.quantity)
        assertEquals(7, dao.getProduct("shop-b", "other-product")?.quantity)
        assertEquals(0, dao.getSaleLines("shop-a").size)
        assertEquals(0, dao.getSaleLines("shop-b").size)
    }

    @Test
    fun productSkuUniquenessIsCaseInsensitiveWithinShopButAllowsOtherShops() = runBlocking {
        dao.insertShop(ShopEntity("shop-a", "Shop A", "General"))
        dao.insertShop(ShopEntity("shop-b", "Shop B", "General"))
        repository.saveProduct(product("sku-a", "shop-a", "SKU-1"))
        repository.saveProduct(product("sku-b", "shop-b", "sku-1"))

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.saveProduct(product("sku-duplicate", "shop-a", "sku-1")) }
        }

        assertEquals(1, repository.getProducts("shop-a").size)
        assertEquals(1, repository.getProducts("shop-b").size)
        assertNotNull(repository.getProduct("shop-a", "sku-a"))
        assertNull(repository.getProduct("shop-b", "sku-a"))
    }

    @Test
    fun legacyImportIsIdempotentAndPreservesOriginalJson() = runBlocking {
        val shopsJson = """[{"id":"legacy-shop","name":"Legacy Shop","businessType":"General"}]"""
        val productsJson = """[{"id":"legacy-product","shopId":"legacy-shop","name":"Legacy Product","sku":"LEG-1","quantity":2,"purchasePricePoisha":100,"sellingPricePoisha":200}]"""
        context.getSharedPreferences("vendora_shops", Context.MODE_PRIVATE).edit().putString("shops", shopsJson).commit()
        context.getSharedPreferences("vendora_products", Context.MODE_PRIVATE).edit().putString("products", productsJson).commit()

        val importer = LegacyDataImporter(context)
        importer.importIfNeeded(database)
        importer.importIfNeeded(database)

        assertEquals(1, repository.getShops().size)
        assertEquals(1, repository.getProducts("legacy-shop").size)
        assertEquals(shopsJson, context.getSharedPreferences("vendora_shops", Context.MODE_PRIVATE).getString("shops", null))
        assertEquals(productsJson, context.getSharedPreferences("vendora_products", Context.MODE_PRIVATE).getString("products", null))
    }

    @Test
    fun malformedLegacyJsonFailsWithoutDeletingSourceAndCanBeRetried() = runBlocking {
        val shopsPreferences = context.getSharedPreferences("vendora_shops", Context.MODE_PRIVATE)
        shopsPreferences.edit().putString("shops", "{malformed").commit()

        val firstAttempt = runCatching { LegacyDataImporter(context).importIfNeeded(database) }
        assertTrue(firstAttempt.exceptionOrNull() is IOException)
        assertEquals("{malformed", shopsPreferences.getString("shops", null))
        assertTrue(repository.getShops().isEmpty())

        val validJson = """[{"id":"recovered-shop","name":"Recovered Shop","businessType":"General"}]"""
        shopsPreferences.edit().putString("shops", validJson).commit()
        LegacyDataImporter(context).importIfNeeded(database)
        assertEquals("recovered-shop", repository.getShops().single().id)
    }

    private suspend fun seedShopAndProduct(shopId: String, productId: String, quantity: Int) {
        if (dao.getShops().none { it.id == shopId }) {
            dao.insertShop(ShopEntity(shopId, shopId, "General"))
        }
        dao.insertProduct(
            ProductEntity(
                id = productId,
                shopId = shopId,
                name = productId,
                sku = productId,
                quantity = quantity,
                purchasePricePoisha = 80L,
                sellingPricePoisha = 150L,
            ),
        )
    }

    private fun product(id: String, shopId: String, sku: String) = ProductEntity(
        id = id,
        shopId = shopId,
        name = id,
        sku = sku,
        quantity = 0,
        purchasePricePoisha = 80L,
        sellingPricePoisha = 150L,
    )
}
