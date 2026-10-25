package dev.vendora.android

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SalesScreen(
    shop: Shop,
    productStore: ProductStore,
    salesStore: SalesStore,
    onBack: () -> Unit,
) {
    var products by remember(shop.id) { mutableStateOf(productStore.getProducts(shop.id)) }
    var sales by remember(shop.id) { mutableStateOf(salesStore.getSales(shop.id)) }
    var selectedProductId by remember { mutableStateOf<String?>(null) }
    var quantityText by remember { mutableStateOf("1") }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf<String?>(null) }
    var confirmSale by remember { mutableStateOf(false) }

    val selectedProduct = products.firstOrNull { it.id == selectedProductId }
    val quantity = quantityText.toIntOrNull()
    val total = if (selectedProduct != null && quantity != null && quantity > 0) {
        runCatching { Math.multiplyExact(selectedProduct.sellingPricePoisha, quantity.toLong()) }.getOrNull()
    } else null

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
                Text("Select product", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Only products from this shop are shown.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (products.isEmpty()) {
                item {
                    Card(shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.fillMaxWidth().padding(20.dp)) {
                            Text("No products available", fontWeight = FontWeight.SemiBold)
                            Text("Add a product to inventory before recording a sale.")
                        }
                    }
                }
            }
            items(products, key = { it.id }) { product ->
                Card(
                    onClick = {
                        selectedProductId = product.id
                        error = null
                        success = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedProductId == product.id)
                            MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                    ),
                    border = if (selectedProductId == product.id)
                        androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                ) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(product.name, fontWeight = FontWeight.SemiBold)
                            if (product.sku.isNotBlank()) Text("SKU: ${product.sku}", style = MaterialTheme.typography.bodySmall)
                            Text("Available: ${product.quantity}", style = MaterialTheme.typography.bodySmall)
                        }
                        Column {
                            Text(formatBdt(product.sellingPricePoisha), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            if (product.quantity == 0) Text("Out of stock", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            if (selectedProduct != null) {
                item {
                    Card(shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Quantity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            OutlinedTextField(
                                value = quantityText,
                                onValueChange = { quantityText = it.filter(Char::isDigit).take(9); error = null },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Units to sell") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            )
                            Text("In stock: ${selectedProduct.quantity}")
                            Text("Total: ${total?.let(::formatBdt) ?: "—"}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Button(
                                onClick = { confirmSale = true },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                enabled = quantity != null && quantity > 0 && quantity <= selectedProduct.quantity && total != null && total >= 0L,
                            ) { Text("Review sale") }
                            if (quantity != null && quantity > selectedProduct.quantity) {
                                Text("Quantity exceeds available stock.", color = MaterialTheme.colorScheme.error)
                            }
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
                                Text("${sale.quantity} × ${formatBdt(sale.unitPricePoisha)}", style = MaterialTheme.typography.bodySmall)
                                Text(SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(sale.timestampMillis)),
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(formatBdt(sale.totalPoisha), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (confirmSale && selectedProduct != null && quantity != null && total != null) {
        AlertDialog(
            onDismissRequest = { confirmSale = false },
            title = { Text("Confirm sale?") },
            text = { Text("${selectedProduct.name} · ${quantity} unit(s) · ${formatBdt(total)}. This will reduce stock and record the sale.") },
            confirmButton = {
                Button(onClick = {
                    confirmSale = false
                    runCatching { salesStore.recordSale(productStore, shop.id, selectedProduct.id, quantity) }
                        .onSuccess { sale ->
                            products = productStore.getProducts(shop.id)
                            sales = salesStore.getSales(shop.id)
                            selectedProductId = null
                            quantityText = "1"
                            error = null
                            success = "Sale saved: ${formatBdt(sale.totalPoisha)}"
                        }
                        .onFailure { error = it.message ?: "Could not record sale. Check stock history before retrying." }
                }) { Text("Confirm sale") }
            },
            dismissButton = { TextButton(onClick = { confirmSale = false }) { Text("Cancel") } },
        )
    }
}
