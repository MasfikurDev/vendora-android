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
