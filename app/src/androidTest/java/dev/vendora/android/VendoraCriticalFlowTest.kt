package dev.vendora.android

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VendoraCriticalFlowTest {
    private lateinit var database: VendoraDatabase
    private lateinit var repository: VendoraRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, VendoraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = VendoraRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun checkoutDeductsStockAndRecordsSaleAndMovementAtomically() = runBlocking {
        repository.saveShop(ShopEntity("shop-a", "Shop A", "General"))
        repository.saveProduct(product(shopId = "shop-a", quantity = 5), timestampMillis = 100L)

        val result = repository.checkout(
            shopId = "shop-a",
            cart = listOf(CheckoutLine(productId = "product-a", quantity = 2)),
            timestampMillis = 200L,
        )

        assertEquals(1, result.lineCount)
        assertEquals(2_000L, result.totalPoisha)
        assertEquals(3, repository.getProduct("shop-a", "product-a")?.quantity)
        assertEquals(1, repository.getSaleLines("shop-a").size)

        val movements = repository.getStockMovements("shop-a")
        assertEquals(2, movements.size)
        assertTrue(movements.any {
            it.reason == "Sale" && it.delta == -2 && it.quantityBefore == 5 && it.quantityAfter == 3
        })
    }

    @Test
    fun insufficientStockLeavesInventorySalesAndLedgerUnchanged() = runBlocking {
        repository.saveShop(ShopEntity("shop-a", "Shop A", "General"))
        repository.saveProduct(product(shopId = "shop-a", quantity = 1), timestampMillis = 100L)

        val error = runCatching {
            repository.checkout(
                shopId = "shop-a",
                cart = listOf(CheckoutLine(productId = "product-a", quantity = 2)),
                timestampMillis = 200L,
            )
        }.exceptionOrNull()

        assertNotNull(error)
        assertEquals(1, repository.getProduct("shop-a", "product-a")?.quantity)
        assertTrue(repository.getSaleLines("shop-a").isEmpty())
        assertEquals(1, repository.getStockMovements("shop-a").size)
    }

    @Test
    fun productQueriesAndSkuUniquenessAreScopedToEachShop() = runBlocking {
        repository.saveShop(ShopEntity("shop-a", "Shop A", "General"))
        repository.saveShop(ShopEntity("shop-b", "Shop B", "General"))
        repository.saveProduct(product(shopId = "shop-a", sku = "SKU-1"))
        repository.saveProduct(product(id = "product-b", shopId = "shop-b", sku = "sku-1"))

        assertNotNull(repository.getProduct("shop-a", "product-a"))
        assertNull(repository.getProduct("shop-b", "product-a"))
        assertEquals(1, repository.getProducts("shop-a").size)
        assertEquals(1, repository.getProducts("shop-b").size)

        val duplicateError = runCatching {
            repository.saveProduct(product(id = "product-a2", shopId = "shop-a", sku = "sku-1"))
        }.exceptionOrNull()
        assertNotNull(duplicateError)
        assertEquals(1, repository.getProducts("shop-a").size)
    }

    private fun product(
        id: String = "product-a",
        shopId: String,
        sku: String = "SKU-1",
        quantity: Int = 5,
    ) = ProductEntity(
        id = id,
        shopId = shopId,
        name = "Sample product",
        sku = sku,
        quantity = quantity,
        purchasePricePoisha = 500L,
        sellingPricePoisha = 1_000L,
    )
}
