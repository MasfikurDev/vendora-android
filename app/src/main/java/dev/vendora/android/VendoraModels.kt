package dev.vendora.android

fun ShopEntity.toShop(): Shop = Shop(id = id, name = name, businessType = businessType)

fun ProductEntity.toProduct(): Product = Product(
    id = id,
    shopId = shopId,
    name = name,
    sku = sku,
    quantity = quantity,
    purchasePricePoisha = purchasePricePoisha,
    sellingPricePoisha = sellingPricePoisha,
)

fun Product.toEntity(): ProductEntity = ProductEntity(
    id = id,
    shopId = shopId,
    name = name.trim(),
    sku = sku.trim(),
    quantity = quantity,
    purchasePricePoisha = purchasePricePoisha,
    sellingPricePoisha = sellingPricePoisha,
)
