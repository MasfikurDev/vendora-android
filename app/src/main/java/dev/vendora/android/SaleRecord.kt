package dev.vendora.android

/**
 * Immutable record of a completed sale line.
 *
 * Monetary values are stored as integer poisha to avoid floating-point rounding.
 */
data class SaleRecord(
    val id: String,
    val shopId: String,
    val productId: String,
    val productName: String,
    val sku: String,
    val quantity: Int,
    val unitPricePoisha: Long,
    val totalPoisha: Long,
    val timestampMillis: Long,
)
