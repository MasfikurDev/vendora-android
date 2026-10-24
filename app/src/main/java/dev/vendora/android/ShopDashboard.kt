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
import java.util.Locale

@Composable
fun ShopDashboard(shop: Shop, productStore: ProductStore, onBack: () -> Unit) {
    var products by remember(shop.id) { mutableStateOf(productStore.getProducts(shop.id)) }
    var query by remember { mutableStateOf("") }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Product?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

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
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search products") }, singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )
            Spacer(Modifier.height(12.dp))
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
                    items(products.filter {
                        it.name.contains(query, true) || it.sku.contains(query, true)
                    }, key = { it.id }) { product ->
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(Modifier.weight(1f)) {
                                        Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        if (product.sku.isNotBlank()) Text("SKU: ${product.sku}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text("Stock ${product.quantity}", style = MaterialTheme.typography.labelLarge)
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Buy ${formatBdt(product.purchasePricePoisha)}", style = MaterialTheme.typography.bodyMedium)
                                    Text("Sell ${formatBdt(product.sellingPricePoisha)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = { editing = product; error = null; showEditor = true }) { Text("Edit") }
                                    TextButton(onClick = {
                                        productStore.deleteProduct(shop.id, product.id)
                                        products = productStore.getProducts(shop.id)
                                    }) { Text("Delete") }
                                }
                            }
                        }
                    }
                }
            }
        }
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
