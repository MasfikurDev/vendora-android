package dev.vendora.android

import android.content.Context

/**
 * Sales facade. ProductStore persists the sale, stock decrement, and stock-movement audit
 * entry in a single synchronous SharedPreferences commit.
 */
class SalesStore(context: Context) {
    private val productStore = ProductStore(context)

    fun getSales(shopId: String): List<SaleRecord> = productStore.getSales(shopId)

    fun recordSale(
        productStore: ProductStore,
        shopId: String,
        lines: List<SaleLineRequest>,
    ): List<SaleRecord> = productStore.recordSale(shopId, lines)

    fun recordSale(
        productStore: ProductStore,
        shopId: String,
        productId: String,
        quantity: Int,
    ): SaleRecord = productStore.recordSale(shopId, productId, quantity)
}
