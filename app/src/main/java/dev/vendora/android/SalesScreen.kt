package dev.vendora.android

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    shop: Shop,
    productStore: ProductStore,
    salesStore: SalesStore,
    onBack: () -> Unit,
) {
    var products by remember(shop.id) { mutableStateOf(productStore.getProducts(shop.id)) }
    var sales by remember(shop.id) { mutableStateOf(salesStore.getSales(shop.id)) }
    var cart by remember(shop.id) { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf<String?>(null) }
    var confirmSale by remember { mutableStateOf(false) }

    val cartLines = cart.mapNotNull { (id, quantity) ->
        products.firstOrNull { it.id == id }?.let { it to quantity }
    }
    val cartTotal = runCatching {
        cartLines.fold(0L) { sum, (product, quantity) ->
            Math.addExact(sum, Math.multiplyExact(product.sellingPricePoisha, quantity.toLong()))
        }
    }.getOrNull()
    val cartQuantity = cart.values.sum()

    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column {
                    Text("New sale", fontWeight = FontWeight.Bold)
                    Text(shop.name, style = MaterialTheme.typography.labelMedium)
                } },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Products", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Add products to the cart. Only this shop's inventory is available.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (products.isEmpty()) {
                item {
                    Card(shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.fillMaxWidth().padding(20.dp)) {
                            Text("No products available", fontWeight = FontWeight.SemiBold)
                            Text("Add products to inventory before recording a sale.")
                        }
                    }
                }
            }
            items(products, key = { it.id }) { product ->
                val quantity = cart[product.id] ?: 0
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = if (quantity > 0) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                    colors = CardDefaults.cardColors(
                        containerColor = if (quantity > 0) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(product.name, fontWeight = FontWeight.SemiBold)
                            if (product.sku.isNotBlank()) Text("SKU: ${product.sku}", style = MaterialTheme.typography.bodySmall)
                            Text("Stock: ${product.quantity}", style = MaterialTheme.typography.bodySmall)
                            Text(formatBdt(product.sellingPricePoisha), color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold)
                        }
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = {
                                    cart = if (quantity <= 1) cart - product.id
                                    else cart + (product.id to quantity - 1)
                                    error = null
                                },
                                enabled = quantity > 0,
                                contentPadding = PaddingValues(horizontal = 12.dp),
                            ) { Text("−") }
                            Text("$quantity", modifier = Modifier.padding(horizontal = 10.dp),
                                fontWeight = FontWeight.Bold)
                            Button(
                                onClick = {
                                    cart = cart + (product.id to quantity + 1)
                                    error = null
                                    success = null
                                },
                                enabled = quantity < product.quantity && quantity < Int.MAX_VALUE,
                                contentPadding = PaddingValues(horizontal = 12.dp),
                            ) { Text("+") }
                        }
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Cart", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        if (cartLines.isEmpty()) {
                            Text("Your cart is empty.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            cartLines.forEach { (product, quantity) ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${product.name} × $quantity", modifier = Modifier.weight(1f))
                                    Text(runCatching {
                                        formatBdt(Math.multiplyExact(product.sellingPricePoisha, quantity.toLong()))
                                    }.getOrDefault("—"))
                                }
                            }
                            HorizontalDivider()
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total · $cartQuantity item(s)", fontWeight = FontWeight.Bold)
                                Text(cartTotal?.let(::formatBdt) ?: "—", style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold)
                            }
                        }
                        Button(
                            onClick = { confirmSale = true; error = null },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            enabled = cartLines.isNotEmpty() && cartTotal != null && cartTotal >= 0L,
                        ) { Text("Review checkout") }
                        if (cartTotal == null) {
                            Text("Cart total is too large to calculate.", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
            success?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) } }
            item {
                Spacer(Modifier.height(8.dp))
                Text("Recent sales", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            if (sales.isEmpty()) {
                item { Text("No sales recorded for this shop yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(sales.take(10), key = { it.id }) { sale ->
                    Card(shape = RoundedCornerShape(14.dp)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(sale.productName, fontWeight = FontWeight.SemiBold)
                                Text("${sale.quantity} × ${formatBdt(sale.unitPricePoisha)}",
                                    style = MaterialTheme.typography.bodySmall)
                                Text(SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(sale.timestampMillis)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(formatBdt(sale.totalPoisha), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (confirmSale && cartLines.isNotEmpty() && cartTotal != null) {
        AlertDialog(
            onDismissRequest = { confirmSale = false },
            title = { Text("Confirm checkout?") },
            text = { Text("${cartLines.size} product type(s), $cartQuantity item(s), total ${formatBdt(cartTotal)}. Stock and sale history will be saved together.") },
            confirmButton = {
                Button(onClick = {
                    confirmSale = false
                    val requestedLines = cartLines.map { (product, quantity) ->
                        SaleLineRequest(product.id, quantity)
                    }
                    runCatching { salesStore.recordSale(productStore, shop.id, requestedLines) }
                        .onSuccess { completedSales ->
                            products = productStore.getProducts(shop.id)
                            sales = salesStore.getSales(shop.id)
                            cart = emptyMap()
                            error = null
                            val completedTotal = completedSales.fold(0L) { sum, sale ->
                                Math.addExact(sum, sale.totalPoisha)
                            }
                            success = "Sale saved: ${formatBdt(completedTotal)}"
                        }
                        .onFailure { error = it.message ?: "Could not record sale. Check inventory before retrying." }
                }) { Text("Confirm sale") }
            },
            dismissButton = { TextButton(onClick = { confirmSale = false }) { Text("Cancel") } },
        )
    }
}
