package dev.vendora.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ProductStoreTest {
    private lateinit var store: ProductStore

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("vendora_products", android.content.Context.MODE_PRIVATE)
            .edit().clear().commit()
        store = ProductStore(context)
    }

    @Test
    fun productsAreIsolatedByShop() {
        store.saveProduct(product(id = "p1", shopId = "shop-a", sku = "A-1"))
        store.saveProduct(product(id = "p2", shopId = "shop-b", sku = "B-1"))

        assertEquals(listOf("p1"), store.getProducts("shop-a").map { it.id })
        assertEquals(listOf("p2"), store.getProducts("shop-b").map { it.id })
    }

    @Test
    fun duplicateSkuIsRejectedWithinSameShopIgnoringCase() {
        store.saveProduct(product(id = "p1", shopId = "shop-a", sku = "abc-1"))

        assertThrows(IllegalArgumentException::class.java) {
            store.saveProduct(product(id = "p2", shopId = "shop-a", sku = " ABC-1 "))
        }
    }

    @Test
    fun sameSkuCanBeUsedInDifferentShops() {
        store.saveProduct(product(id = "p1", shopId = "shop-a", sku = "SKU-1"))
        store.saveProduct(product(id = "p2", shopId = "shop-b", sku = "SKU-1"))

        assertEquals(1, store.getProducts("shop-a").size)
        assertEquals(1, store.getProducts("shop-b").size)
    }

    @Test
    fun negativeStockIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            store.saveProduct(product(quantity = -1))
        }
    }

    @Test
    fun negativePricesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            store.saveProduct(product(purchasePricePoisha = -1L))
        }
        assertThrows(IllegalArgumentException::class.java) {
            store.saveProduct(product(sellingPricePoisha = -1L))
        }
    }

    @Test
    fun savingExistingIdUpdatesProductInsteadOfDuplicatingIt() {
        store.saveProduct(product(id = "p1", name = "Tea"))
        store.saveProduct(product(id = "p1", name = " Green Tea ", quantity = 7))

        val products = store.getProducts("shop-a")
        assertEquals(1, products.size)
        assertEquals("Green Tea", products.single().name)
        assertEquals(7, products.single().quantity)
    }

    @Test
    fun deleteOnlyRemovesProductFromRequestedShop() {
        store.saveProduct(product(id = "p1", shopId = "shop-a"))
        store.saveProduct(product(id = "p2", shopId = "shop-b"))

        store.deleteProduct(shopId = "shop-a", productId = "p1")

        assertEquals(emptyList<String>(), store.getProducts("shop-a").map { it.id })
        assertEquals(listOf("p2"), store.getProducts("shop-b").map { it.id })
    }


    @Test
    fun stockCanBeAdjustedUpAndDown() {
        store.saveProduct(product(id = "p1", quantity = 3))

        assertEquals(4, store.adjustStock("shop-a", "p1", 1).quantity)
        assertEquals(2, store.adjustStock("shop-a", "p1", -2).quantity)
        assertEquals(2, store.getProducts("shop-a").single().quantity)
    }

    @Test
    fun stockAdjustmentCannotMakeQuantityNegative() {
        store.saveProduct(product(id = "p1", quantity = 0))

        assertThrows(IllegalArgumentException::class.java) {
            store.adjustStock("shop-a", "p1", -1)
        }
        assertEquals(0, store.getProducts("shop-a").single().quantity)
    }

    @Test
    fun stockAdjustmentMustTargetProductInRequestedShop() {
        store.saveProduct(product(id = "p1", shopId = "shop-a", quantity = 2))

        assertThrows(IllegalArgumentException::class.java) {
            store.adjustStock("shop-b", "p1", 1)
        }
        assertEquals(2, store.getProducts("shop-a").single().quantity)
    }

    @Test
    fun zeroStockAdjustmentIsRejected() {
        store.saveProduct(product(id = "p1", quantity = 2))

        assertThrows(IllegalArgumentException::class.java) {
            store.adjustStock("shop-a", "p1", 0)
        }
        assertEquals(2, store.getProducts("shop-a").single().quantity)
    }

    @Test
    fun savingProductCannotMoveExistingIdToAnotherShop() {
        store.saveProduct(product(id = "p1", shopId = "shop-a"))

        assertThrows(IllegalArgumentException::class.java) {
            store.saveProduct(product(id = "p1", shopId = "shop-b"))
        }
        assertEquals(listOf("p1"), store.getProducts("shop-a").map { it.id })
        assertEquals(emptyList<String>(), store.getProducts("shop-b").map { it.id })
    }


    @Test
    fun stockAdjustmentsAreAuditedAndScopedToShop() {
        store.saveProduct(product(id = "p1", shopId = "shop-a", quantity = 3))
        store.saveProduct(product(id = "p2", shopId = "shop-b", quantity = 8))

        store.adjustStock("shop-a", "p1", 2, "Received delivery")
        store.adjustStock("shop-a", "p1", -1, "Damaged item")

        val history = store.getStockMovements("shop-a", "p1")
        assertEquals(2, history.size)
        assertEquals(-1, history.first().delta)
        assertEquals(5, history.first().quantityBefore)
        assertEquals(4, history.first().quantityAfter)
        assertEquals("Damaged item", history.first().reason)
        assertEquals(emptyList<StockMovement>(), store.getStockMovements("shop-b"))
    }

    @Test
    fun stockHistorySurvivesStoreRecreation() {
        store.saveProduct(product(id = "p1", quantity = 3))
        store.adjustStock("shop-a", "p1", 1, "Count correction")
        store = ProductStore(RuntimeEnvironment.getApplication())

        assertEquals(1, store.getStockMovements("shop-a").size)
        assertEquals("Count correction", store.getStockMovements("shop-a").single().reason)
    }

    @Test
    fun recordingSaleAtomicallyUpdatesStockSaleAndMovementHistory() {
        store.saveProduct(product(id = "p-sale", quantity = 5, sellingPricePoisha = 250L))

        val sale = store.recordSale("shop-a", "p-sale", 2)

        assertEquals(3, store.getProducts("shop-a").single().quantity)
        assertEquals(500L, sale.totalPoisha)
        assertEquals(listOf(sale.id), store.getSales("shop-a").map { it.id })
        val movement = store.getStockMovements("shop-a", "p-sale").single()
        assertEquals(-2, movement.delta)
        assertEquals(5, movement.quantityBefore)
        assertEquals(3, movement.quantityAfter)
        assertEquals("Sale ${sale.id}", movement.reason)
    }

    @Test
    fun saleRejectsOversellingAndDoesNotCreateRecords() {
        store.saveProduct(product(id = "p-sale", quantity = 1))

        assertThrows(IllegalArgumentException::class.java) {
            store.recordSale("shop-a", "p-sale", 2)
        }

        assertEquals(1, store.getProducts("shop-a").single().quantity)
        assertEquals(emptyList<SaleRecord>(), store.getSales("shop-a"))
        assertEquals(emptyList<StockMovement>(), store.getStockMovements("shop-a"))
    }

    @Test
    fun salesAreIsolatedByShopAndPersistAfterStoreRecreation() {
        store.saveProduct(product(id = "p-a", shopId = "shop-a", quantity = 2))
        store.saveProduct(product(id = "p-b", shopId = "shop-b", quantity = 2))
        store.recordSale("shop-a", "p-a", 1)
        store = ProductStore(RuntimeEnvironment.getApplication())

        assertEquals(1, store.getSales("shop-a").size)
        assertEquals(emptyList<SaleRecord>(), store.getSales("shop-b"))
        assertEquals(2, store.getProducts("shop-b").single().quantity)
    }

    private fun product(
        id: String = "product-1",
        shopId: String = "shop-a",
        name: String = "Test product",
        sku: String = "",
        quantity: Int = 3,
        purchasePricePoisha: Long = 1000L,
        sellingPricePoisha: Long = 1500L,
    ) = Product(
        id = id,
        shopId = shopId,
        name = name,
        sku = sku,
        quantity = quantity,
        purchasePricePoisha = purchasePricePoisha,
        sellingPricePoisha = sellingPricePoisha,
    )
}
