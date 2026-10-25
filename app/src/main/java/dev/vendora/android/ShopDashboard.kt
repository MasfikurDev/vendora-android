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
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopDashboard(shop: Shop, productStore: ProductStore, onBack: () -> Unit) {
    var products by remember(shop.id) { mutableStateOf(productStore.getProducts(shop.id)) }
    var query by remember { mutableStateOf("") }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Product?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var productPendingDelete by remember { mutableStateOf<Product?>(null) }
    var stockAdjustmentError by remember { mutableStateOf<String?>(null) }
    var showStockHistory by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(shop.name, fontWeight = FontWeight.Bold)
                        Text(shop.businessType, style = MaterialTheme.typography.labelMedium)
                    }
                },
                navigationIcon = { TextButton(onClick = onBack) { Text("Shops") } },
                actions = { TextButton(onClick = { showStockHistory = true }) { Text("History") } },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; error = null; showEditor = true }) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(12.dp))
            Text("Inventory", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("${products.size} products · ${products.sumOf { it.quantity }} units in stock",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val lowStockCount = products.count { it.quantity <= ProductStore.LOW_STOCK_THRESHOLD }
            if (lowStockCount > 0) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "$lowStockCount product(s) at or below ${ProductStore.LOW_STOCK_THRESHOLD} units",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search products") }, singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )
            Spacer(Modifier.height(12.dp))
            val filteredProducts = products.filter {
                it.name.contains(query, true) || it.sku.contains(query, true)
            }
            if (products.isEmpty()) {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(24.dp)) {
                        Text("Your inventory starts here", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text("Add products with stock and prices. Each shop keeps its own catalog.")
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { editing = null; error = null; showEditor = true }) { Text("Add first product") }
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 88.dp)) {
                    items(filteredProducts, key = { it.id }) { product ->
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(Modifier.weight(1f)) {
                                        Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        if (product.sku.isNotBlank()) Text("SKU: ${product.sku}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Column {
                                        Text("Stock ${product.quantity}", style = MaterialTheme.typography.labelLarge)
                                        if (product.quantity <= ProductStore.LOW_STOCK_THRESHOLD) {
                                            Text(
                                                if (product.quantity == 0) "Out of stock" else "Low stock",
                                                color = MaterialTheme.colorScheme.error,
                                                style = MaterialTheme.typography.labelSmall,
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Buy ${formatBdt(product.purchasePricePoisha)}", style = MaterialTheme.typography.bodyMedium)
                                    Text("Sell ${formatBdt(product.sellingPricePoisha)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.height(4.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Adjust stock", style = MaterialTheme.typography.labelLarge)
                                    Row {
                                        OutlinedButton(
                                            enabled = product.quantity > 0,
                                            onClick = {
                                                runCatching { productStore.adjustStock(shop.id, product.id, -1) }
                                                    .onSuccess { updated ->
                                                        products = products.map { if (it.id == updated.id) updated else it }
                                                        stockAdjustmentError = null
                                                    }
                                                    .onFailure { stockAdjustmentError = it.message ?: "Could not adjust stock." }
                                            },
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                        ) { Text("−1") }
                                        Spacer(Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                runCatching { productStore.adjustStock(shop.id, product.id, 1) }
                                                    .onSuccess { updated ->
                                                        products = products.map { if (it.id == updated.id) updated else it }
                                                        stockAdjustmentError = null
                                                    }
                                                    .onFailure { stockAdjustmentError = it.message ?: "Could not adjust stock." }
                                            },
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                        ) { Text("+1") }
                                    }
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = { editing = product; error = null; showEditor = true }) { Text("Edit") }
                                    TextButton(onClick = { productPendingDelete = product }) { Text("Delete") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showStockHistory) {
        val movements = productStore.getStockMovements(shop.id)
        AlertDialog(
            onDismissRequest = { showStockHistory = false },
            title = { Text("Stock history", fontWeight = FontWeight.Bold) },
            text = {
                if (movements.isEmpty()) {
                    Text("No stock adjustments recorded yet. Use the +1 or −1 controls to create the first entry.")
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(movements, key = { it.id }) { movement ->
                            Card(shape = RoundedCornerShape(14.dp)) {
                                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(movement.productName, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                        Text(if (movement.delta > 0) "+${movement.delta}" else "${movement.delta}",
                                            color = if (movement.delta > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold)
                                    }
                                    Text("${movement.quantityBefore} → ${movement.quantityAfter} units",
                                        style = MaterialTheme.typography.bodyMedium)
                                    Text(movement.reason, style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()).format(Date(movement.timestampMillis)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showStockHistory = false }) { Text("Done") } },
        )
    }

    stockAdjustmentError?.let { message ->
        AlertDialog(
            onDismissRequest = { stockAdjustmentError = null },
            title = { Text("Stock not changed") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { stockAdjustmentError = null }) { Text("OK") } },
        )
    }

    if (products.isNotEmpty() && query.isNotBlank() && filteredProductsForQuery(products, query).isEmpty()) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(20.dp)) {
                Text("No matching products", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text("Try a different product name or SKU.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    productPendingDelete?.let { product ->
        AlertDialog(
            onDismissRequest = { productPendingDelete = null },
            title = { Text("Delete ${product.name}?") },
            text = { Text("This removes the product from ${shop.name}'s inventory. This action cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    productStore.deleteProduct(shop.id, product.id)
                    products = productStore.getProducts(shop.id)
                    productPendingDelete = null
                }) { Text("Delete product") }
            },
            dismissButton = {
                TextButton(onClick = { productPendingDelete = null }) { Text("Cancel") }
            },
        )
    }

    if (showEditor) {
        ProductEditorDialog(
            initial = editing,
            error = error,
            onDismiss = { showEditor = false },
            onSave = { name, sku, quantity, purchase, selling ->
                runCatching {
                    val p = Product(
                        id = editing?.id ?: ProductStore.newId(),
                        shopId = shop.id,
                        name = name,
                        sku = sku,
                        quantity = quantity,
                        purchasePricePoisha = purchase,
                        sellingPricePoisha = selling,
                    )
                    productStore.saveProduct(p)
                    products = productStore.getProducts(shop.id)
                    showEditor = false
                }.onFailure { error = it.message ?: "Could not save product." }
            },
        )
    }
}

private fun filteredProductsForQuery(products: List<Product>, query: String) = products.filter {
    it.name.contains(query, true) || it.sku.contains(query, true)
}

@Composable
private fun ProductEditorDialog(
    initial: Product?,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, Long, Long) -> Unit,
) {
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var sku by remember(initial?.id) { mutableStateOf(initial?.sku.orEmpty()) }
    var quantity by remember(initial?.id) { mutableStateOf(initial?.quantity?.toString() ?: "0") }
    var purchase by remember(initial?.id) { mutableStateOf(initial?.purchasePricePoisha?.let { (it / 100.0).toString() } ?: "") }
    var selling by remember(initial?.id) { mutableStateOf(initial?.sellingPricePoisha?.let { (it / 100.0).toString() } ?: "") }
    val valid = name.isNotBlank() && (quantity.toIntOrNull() ?: -1) >= 0 &&
        parsePoisha(purchase) != null && parsePoisha(selling) != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add product" else "Edit product", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Product name *") }, singleLine = true)
                OutlinedTextField(sku, { sku = it }, label = { Text("SKU (optional)") }, singleLine = true)
                OutlinedTextField(quantity, { quantity = it.filter(Char::isDigit) }, label = { Text("Stock quantity") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(purchase, { purchase = it }, label = { Text("Purchase price (৳)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                OutlinedTextField(selling, { selling = it }, label = { Text("Selling price (৳)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = { onSave(name.trim(), sku.trim(), quantity.toInt(), parsePoisha(purchase)!!, parsePoisha(selling)!!) },
            ) { Text("Save product") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        shape = RoundedCornerShape(24.dp),
    )
}

private fun parsePoisha(value: String): Long? {
    val normalized = value.trim().replace(",", "")
    if (normalized.isEmpty()) return null
    val parts = normalized.split(".")
    if (parts.size > 2 || parts.any { part -> part.isNotEmpty() && part.any { !it.isDigit() } }) return null
    val taka = parts[0].ifEmpty { "0" }.toLongOrNull() ?: return null
    val poisha = when (parts.getOrNull(1)?.length ?: 0) {
        0 -> 0L
        1 -> (parts[1].toLongOrNull() ?: return null) * 10
        2 -> parts[1].toLongOrNull() ?: return null
        else -> return null
    }
    return taka * 100 + poisha
}

private fun formatBdt(poisha: Long): String =
    "৳" + NumberFormat.getNumberInstance(Locale("en", "BD")).format(poisha / 100.0)
