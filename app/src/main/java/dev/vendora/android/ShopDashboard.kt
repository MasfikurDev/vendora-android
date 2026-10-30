package dev.vendora.android

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

private val DashboardGreen = Color(0xFF145945)
private val DashboardMint = Color(0xFFE0F0E3)
private val DashboardAmber = Color(0xFFFFF2D4)
private val DashboardInk = Color(0xFF172621)
private val DashboardMuted = Color(0xFF66756E)
private val DashboardOutline = Color(0xFFE0E5DE)

private enum class ShopSection(val label: String) {
    Overview("Overview"),
    Products("Products"),
    Selling("Selling"),
    Insights("Insights"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopDashboard(shop: Shop, repository: VendoraRepository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var products by remember(shop.id) { mutableStateOf(emptyList<Product>()) }
    LaunchedEffect(shop.id) {
        products = repository.getProducts(shop.id).map { it.toProduct() }
    }
    var section by remember { mutableStateOf(ShopSection.Overview) }
    var query by remember { mutableStateOf("") }
    var sellQuery by remember(shop.id) { mutableStateOf("") }
    var inventoryFilter by remember(shop.id) { mutableStateOf("All") }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Product?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var productPendingDelete by remember { mutableStateOf<Product?>(null) }
    var cartQuantities by remember(shop.id) { mutableStateOf(emptyMap<String, Int>()) }
    var checkoutBusy by remember { mutableStateOf(false) }
    var checkoutError by remember { mutableStateOf<String?>(null) }
    var checkoutSuccess by remember { mutableStateOf<String?>(null) }

    val lowStock = products.filter { it.quantity <= 5 }
    val units = products.sumOf { it.quantity }
    val stockCost = products.sumOf { it.quantity.toLong() * it.purchasePricePoisha }
    val potentialSales = products.sumOf { it.quantity.toLong() * it.sellingPricePoisha }
    val potentialMargin = potentialSales - stockCost

    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(shop.name, fontWeight = FontWeight.Bold)
                        Text(shop.businessType, style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = { TextButton(onClick = onBack) { Text("Shops") } },
                actions = {
                    TextButton(onClick = { section = ShopSection.Products }) { Text("Inventory") }
                },
            )
        },
        floatingActionButton = {
            if (section == ShopSection.Products || products.isEmpty()) {
                FloatingActionButton(
                    onClick = { editing = null; error = null; showEditor = true },
                    containerColor = DashboardGreen,
                    contentColor = Color.White,
                ) { Text("+", style = MaterialTheme.typography.headlineMedium) }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                ShopSection.entries.forEach { item ->
                    NavigationBarItem(
                        selected = section == item,
                        onClick = { section = item },
                        icon = {
                            Text(
                                when (item) {
                                    ShopSection.Overview -> "⌂"
                                    ShopSection.Products -> "▦"
                                    ShopSection.Selling -> "＋"
                                    ShopSection.Insights -> "↗"
                                },
                                style = MaterialTheme.typography.titleLarge,
                            )
                        },
                        label = { Text(item.label) },
                        alwaysShowLabel = true,
                    )
                }
            }
        },
    ) { padding ->
        when (section) {
            ShopSection.Overview -> OverviewContent(
                modifier = Modifier.fillMaxSize().padding(padding),
                shopName = shop.name,
                products = products,
                units = units,
                stockCost = stockCost,
                potentialSales = potentialSales,
                lowStock = lowStock,
                onViewInventory = { section = ShopSection.Products },
                onStartSelling = { section = ShopSection.Selling },
                onAddProduct = { editing = null; error = null; showEditor = true },
                onSelectProduct = { editing = it; error = null; showEditor = true },
            )
            ShopSection.Products -> InventoryContent(
                modifier = Modifier.fillMaxSize().padding(padding),
                products = products,
                query = query,
                filter = inventoryFilter,
                onQueryChange = { query = it },
                onFilterChange = { inventoryFilter = it },
                onAddProduct = { editing = null; error = null; showEditor = true },
                onEdit = { editing = it; error = null; showEditor = true },
                onDelete = { productPendingDelete = it },
            )
            ShopSection.Selling -> SellingContent(
                modifier = Modifier.fillMaxSize().padding(padding),
                products = products,
                query = sellQuery,
                onQueryChange = { sellQuery = it },
                quantities = cartQuantities,
                busy = checkoutBusy,
                error = checkoutError,
                success = checkoutSuccess,
                onAddProduct = { section = ShopSection.Products; editing = null; error = null; showEditor = true },
                onQuantityChange = { product, quantity ->
                    cartQuantities = if (quantity <= 0) cartQuantities - product.id else cartQuantities + (product.id to quantity)
                    checkoutError = null
                    checkoutSuccess = null
                },
                onCheckout = {
                    scope.launch {
                        checkoutBusy = true
                        checkoutError = null
                        checkoutSuccess = null
                        try {
                            val lines = cartQuantities.filterValues { it > 0 }.map { (productId, quantity) ->
                                CheckoutLine(productId = productId, quantity = quantity)
                            }
                            val result = repository.checkout(shop.id, lines)
                            products = repository.getProducts(shop.id).map { it.toProduct() }
                            cartQuantities = emptyMap()
                            checkoutSuccess = "Sale recorded. Total: ${formatBdt(result.totalPoisha)}"
                        } catch (failure: Exception) {
                            checkoutError = failure.message ?: "Checkout failed. Stock was not changed."
                        } finally {
                            checkoutBusy = false
                        }
                    }
                },
            )
            ShopSection.Insights -> InsightsContent(
                modifier = Modifier.fillMaxSize().padding(padding),
                products = products,
                units = units,
                stockCost = stockCost,
                potentialSales = potentialSales,
                potentialMargin = potentialMargin,
                lowStock = lowStock,
            )
        }
    }

    productPendingDelete?.let { product ->
        AlertDialog(
            onDismissRequest = { productPendingDelete = null },
            title = { Text("Delete ${product.name}?") },
            text = { Text("This removes the product from ${shop.name}'s inventory. This action cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        runCatching {
                            repository.deleteProduct(shop.id, product.id)
                            products = repository.getProducts(shop.id).map { it.toProduct() }
                            cartQuantities = cartQuantities - product.id
                            productPendingDelete = null
                        }.onFailure { error = it.message ?: "Could not delete product." }
                    }
                }) { Text("Delete product") }
            },
            dismissButton = { TextButton(onClick = { productPendingDelete = null }) { Text("Cancel") } },
        )
    }

    if (showEditor) {
        ProductEditorDialog(
            initial = editing,
            error = error,
            onDismiss = { showEditor = false },
            onSave = { name, sku, quantity, purchase, selling ->
                scope.launch {
                    runCatching {
                        val product = Product(
                            id = editing?.id ?: ProductStore.newId(),
                            shopId = shop.id,
                            name = name,
                            sku = sku,
                            quantity = quantity,
                            purchasePricePoisha = purchase,
                            sellingPricePoisha = selling,
                        )
                        repository.saveProduct(product.toEntity())
                        products = repository.getProducts(shop.id).map { it.toProduct() }
                        showEditor = false
                    }.onFailure { error = it.message ?: "Could not save product." }
                }
            },
        )
    }
}

@Composable
private fun SellingContent(
    modifier: Modifier,
    products: List<Product>,
    query: String,
    onQueryChange: (String) -> Unit,
    quantities: Map<String, Int>,
    busy: Boolean,
    error: String?,
    success: String?,
    onAddProduct: () -> Unit,
    onQuantityChange: (Product, Int) -> Unit,
    onCheckout: () -> Unit,
) {
    val selected = products.filter { (quantities[it.id] ?: 0) > 0 }
    val filteredProducts = products.filter { it.name.contains(query, true) || it.sku.contains(query, true) }
    val total = selected.sumOf { product -> product.sellingPricePoisha * (quantities[product.id] ?: 0).toLong() }

    Column(modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text("New sale", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Find products and build the cart.", color = DashboardMuted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search name or SKU") },
            leadingIcon = { Text("⌕", style = MaterialTheme.typography.titleLarge, color = DashboardMuted) },
            trailingIcon = {
                if (query.isNotBlank()) TextButton(onClick = { onQueryChange("") }) { Text("Clear") }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
        )
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Available products", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("${filteredProducts.size} items", style = MaterialTheme.typography.labelMedium, color = DashboardMuted)
        }
        Spacer(Modifier.height(8.dp))
        if (products.isEmpty()) {
            EmptyInventoryCard(onAddProduct = onAddProduct)
        } else if (filteredProducts.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("No matching products. Try another search.", color = DashboardMuted)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 12.dp),
            ) {
                items(filteredProducts, key = { it.id }) { product ->
                    val quantity = quantities[product.id] ?: 0
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(product.name, fontWeight = FontWeight.SemiBold)
                                    Text("${formatBdt(product.sellingPricePoisha)} each · ${product.quantity} available",
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { onQuantityChange(product, quantity - 1) }, enabled = quantity > 0, contentPadding = PaddingValues(horizontal = 12.dp)) { Text("−") }
                                    Text(quantity.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    OutlinedButton(onClick = { onQuantityChange(product, quantity + 1) }, enabled = quantity < product.quantity, contentPadding = PaddingValues(horizontal = 12.dp)) { Text("+") }
                                }
                            }
                            if (quantity > 0) {
                                Text("Line total: ${formatBdt(product.sellingPricePoisha * quantity.toLong())}", color = DashboardGreen, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        if (success != null) Text(success, color = DashboardGreen, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DashboardOutline),
        ) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Cart total", style = MaterialTheme.typography.labelMedium, color = DashboardMuted)
                    Text(formatBdt(total), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = DashboardGreen)
                    Text("${selected.size} product line(s)", style = MaterialTheme.typography.bodySmall, color = DashboardMuted)
                }
                Button(
                    onClick = onCheckout,
                    enabled = selected.isNotEmpty() && !busy,
                    modifier = Modifier.heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Complete sale")
                }
            }
        }
    }
}

@Composable
private fun OverviewContent(
    modifier: Modifier,
    shopName: String,
    products: List<Product>,
    units: Int,
    stockCost: Long,
    potentialSales: Long,
    lowStock: List<Product>,
    onViewInventory: () -> Unit,
    onStartSelling: () -> Unit,
    onAddProduct: () -> Unit,
    onSelectProduct: (Product) -> Unit,
) {
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Business overview", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("A clear view of $shopName at a glance.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DashboardGreen),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).background(Color.White.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                        Text("V", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(shopName, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("SHOP WORKSPACE", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelSmall)
                    }
                    Surface(color = Color.White.copy(alpha = 0.16f), shape = RoundedCornerShape(100.dp)) {
                        Text("YOUR BUSINESS", color = Color.White, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    }
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.22f))
                Text("Inventory cost value", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium)
                Text(formatBdt(stockCost), color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("Based on current stock and purchase prices", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("Products", products.size.toString(), "In your catalog", Modifier.weight(1f), DashboardMint)
            MetricCard("Units in stock", units.toString(), "Across all products", Modifier.weight(1f), Color(0xFFEAF0FB))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Stock attention", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Products with 5 or fewer units", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onViewInventory) { Text("View all") }
        }

        if (lowStock.isEmpty()) {
            Surface(shape = RoundedCornerShape(18.dp), color = DashboardMint) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("✓", color = DashboardGreen, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("No low-stock alerts", fontWeight = FontWeight.SemiBold)
                        Text("Products with low quantities will appear here.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else {
            lowStock.take(4).forEach { product ->
                Card(onClick = { onSelectProduct(product) }, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = DashboardAmber)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(product.name, fontWeight = FontWeight.SemiBold)
                            Text("Restock soon", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${product.quantity} left", color = Color(0xFF8A5700), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Button(
            onClick = onStartSelling,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text("Start selling", fontWeight = FontWeight.SemiBold)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onAddProduct, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp)) {
                Text("+  Add product")
            }
            OutlinedButton(onClick = onViewInventory, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp)) {
                Text("Open inventory")
            }
        }

        Text("Potential sales value: ${formatBdt(potentialSales)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InventoryContent(
    modifier: Modifier,
    products: List<Product>,
    query: String,
    filter: String,
    onQueryChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    onAddProduct: () -> Unit,
    onEdit: (Product) -> Unit,
    onDelete: (Product) -> Unit,
) {
    val filteredProducts = products.filter { product ->
        (product.name.contains(query, true) || product.sku.contains(query, true)) &&
            when (filter) {
                "Low stock" -> product.quantity <= 5
                "In stock" -> product.quantity > 5
                else -> true
            }
    }
    Column(modifier.padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Inventory", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("${products.size} products · ${products.sumOf { it.quantity }} units in stock",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilledTonalButton(onClick = onAddProduct) { Text("+ Add") }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = query, onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search products or SKU") },
            leadingIcon = { Text("⌕", style = MaterialTheme.typography.titleLarge, color = DashboardMuted) },
            trailingIcon = {
                if (query.isNotBlank()) TextButton(onClick = { onQueryChange("") }) { Text("Clear") }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "In stock", "Low stock").forEach { option ->
                FilterChip(
                    selected = filter == option,
                    onClick = { onFilterChange(option) },
                    label = { Text(option) },
                    shape = RoundedCornerShape(14.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        if (products.isEmpty()) {
            EmptyInventoryCard(onAddProduct)
        } else if (filteredProducts.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("No matching products. Try another search or filter.", color = DashboardMuted)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(filteredProducts, key = { it.id }) { product ->
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                                Box(Modifier.size(46.dp).background(DashboardMint, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                                    Text(product.name.firstOrNull()?.uppercase() ?: "P", color = DashboardGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    if (product.sku.isNotBlank()) Text("SKU: ${product.sku}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Surface(
                                    color = if (product.quantity <= 5) DashboardAmber else DashboardMint,
                                    shape = RoundedCornerShape(10.dp),
                                ) {
                                    Text("${product.quantity} in stock", modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Purchase", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatBdt(product.purchasePricePoisha), fontWeight = FontWeight.Medium)
                                }
                                Column {
                                    Text("Selling price", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatBdt(product.sellingPricePoisha), fontWeight = FontWeight.SemiBold, color = DashboardGreen)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Unit margin", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatBdt(product.sellingPricePoisha - product.purchasePricePoisha), fontWeight = FontWeight.Medium)
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { onEdit(product) }) { Text("Edit") }
                                TextButton(onClick = { onDelete(product) }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightsContent(
    modifier: Modifier,
    products: List<Product>,
    units: Int,
    stockCost: Long,
    potentialSales: Long,
    potentialMargin: Long,
    lowStock: List<Product>,
) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Insights", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Inventory-based estimates, not recorded sales.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        MetricCard("Stock cost value", formatBdt(stockCost), "Purchase cost of current stock", Modifier.fillMaxWidth(), DashboardMint)
        MetricCard("Potential sales value", formatBdt(potentialSales), "If all current stock sells at listed prices", Modifier.fillMaxWidth(), Color(0xFFEAF0FB))
        MetricCard("Potential gross margin", formatBdt(potentialMargin), "Before expenses, discounts, and losses", Modifier.fillMaxWidth(), Color(0xFFF4EAFE))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("Catalog items", products.size.toString(), "Unique products", Modifier.weight(1f), Color(0xFFF0F3F1))
            MetricCard("Stock units", units.toString(), "Current quantity", Modifier.weight(1f), Color(0xFFF0F3F1))
        }
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = if (lowStock.isNotEmpty()) DashboardAmber else DashboardMint)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Restock watch", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${lowStock.size} product(s) currently have 5 or fewer units.")
                lowStock.take(5).forEach { Text("• ${it.name}: ${it.quantity} left", style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, detail: String, modifier: Modifier, tint: Color) {
    Card(modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = tint)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = DashboardInk)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyInventoryCard(onAddProduct: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(54.dp).background(DashboardMint, CircleShape), contentAlignment = Alignment.Center) {
                Text("+", color = DashboardGreen, style = MaterialTheme.typography.headlineMedium)
            }
            Text("Your inventory starts here", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("Add products with stock and prices. Each shop keeps its own catalog.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onAddProduct) { Text("Add first product") }
        }
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
            Column(
                Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(name, { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Product name *") }, singleLine = true)
                OutlinedTextField(sku, { sku = it }, modifier = Modifier.fillMaxWidth(), label = { Text("SKU (optional)") }, singleLine = true)
                OutlinedTextField(quantity, { quantity = it.filter(Char::isDigit) }, modifier = Modifier.fillMaxWidth(), label = { Text("Stock quantity") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(purchase, { purchase = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Purchase price (৳)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                OutlinedTextField(selling, { selling = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Selling price (৳)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
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
    return try { Math.addExact(Math.multiplyExact(taka, 100L), poisha) } catch (_: ArithmeticException) { null }
}

private fun formatBdt(poisha: Long): String =
    "৳" + NumberFormat.getNumberInstance(Locale("en", "BD")).format(poisha / 100.0)
