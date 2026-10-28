package dev.vendora.android

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VendoraCheckoutInstrumentedTest {
    private lateinit var database: VendoraDatabase
    private lateinit var repository: VendoraRepository
    private val dao get() = database.vendoraDao()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
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
    fun savedBrandCategoryAndUnitRemainReusableAfterProductDeletion() = runBlocking {
        dao.insertShop(ShopEntity("shop-a", "Shop A", "General"))
        repository.saveProduct(
            ProductEntity(
                id = "product-a",
                shopId = "shop-a",
                name = "Rice",
                sku = "RICE-01",
                quantity = 0,
                purchasePricePoisha = 5000L,
                sellingPricePoisha = 6000L,
                brand = "Fresh Fields",
                category = "Grocery",
                unit = "kg",
                barcode = "123456789",
                lowStockThreshold = 8,
            ),
        )

        repository.deleteProduct("shop-a", "product-a")

        assertEquals(listOf("Fresh Fields"), repository.getCatalogOptions("shop-a", CatalogOptionKind.BRAND))
        assertEquals(listOf("Grocery"), repository.getCatalogOptions("shop-a", CatalogOptionKind.CATEGORY))
        assertEquals(listOf("kg"), repository.getCatalogOptions("shop-a", CatalogOptionKind.UNIT))
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
}
