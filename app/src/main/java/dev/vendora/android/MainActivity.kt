package dev.vendora.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import java.util.UUID
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val VendoraGreen = Color(0xFF236B55)
private val VendoraBackground = Color(0xFFF5F7F5)
private val VendoraInk = Color(0xFF17231F)
private val VendoraMuted = Color(0xFF68766F)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val shopStore = ShopStore(applicationContext)
        val repository = VendoraRepository(VendoraDatabaseProvider.get(applicationContext))
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = VendoraGreen,
                    onPrimary = Color.White,
                    background = VendoraBackground,
                    onBackground = VendoraInk,
                    surface = Color.White,
                    onSurface = VendoraInk,
                    onSurfaceVariant = VendoraMuted,
                    secondaryContainer = Color(0xFFE1F1E9),
                    onSecondaryContainer = VendoraGreen,
                )
            ) {
                ShopSelectionScreen(repository, shopStore, applicationContext)
            }
        }
    }
}

@Composable
private fun ShopSelectionScreen(
    repository: VendoraRepository,
    shopStore: ShopStore,
    context: android.content.Context,
) {
    val scope = rememberCoroutineScope()
    var shops by remember { mutableStateOf(emptyList<Shop>()) }
    var activeShopId by remember { mutableStateOf(shopStore.getActiveShopId()) }
    var openedShopId by remember { mutableStateOf<String?>(null) }
    var showCreateShop by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(repository, refreshKey) {
        loading = true
        error = null
        try {
            repository.initializeFromLegacy(context)
            shops = repository.getShops().map { it.toShop() }
            if (activeShopId != null && shops.none { it.id == activeShopId }) activeShopId = null
            if (shops.isEmpty()) showCreateShop = true
        } catch (failure: Exception) {
            error = failure.message ?: "Vendora could not load saved data."
        } finally {
            loading = false
        }
    }

    val openedShop = shops.firstOrNull { it.id == openedShopId }
    if (openedShop != null) {
        ShopDashboard(shop = openedShop, repository = repository, onBack = { openedShopId = null })
        return
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 24.dp)) {
            Text("VENDORA", style = MaterialTheme.typography.labelLarge, color = VendoraGreen, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            Text("Your businesses", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = VendoraInk)
            Spacer(Modifier.height(8.dp))
            Text("Keep every shop organized in one place.", style = MaterialTheme.typography.bodyLarge, color = VendoraMuted)
            Spacer(Modifier.height(24.dp))

            if (loading) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (error != null) {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Your saved data could not be loaded", fontWeight = FontWeight.SemiBold)
                        Text(error.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                        Text("Your old data has not been intentionally deleted. Retry after checking the saved data.", style = MaterialTheme.typography.bodySmall)
                        Button(onClick = { refreshKey++ }) { Text("Retry") }
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("YOUR SHOPS", style = MaterialTheme.typography.labelMedium, color = VendoraMuted, fontWeight = FontWeight.SemiBold)
                    Text("${shops.size} ${if (shops.size == 1) "shop" else "shops"}", style = MaterialTheme.typography.labelLarge, color = VendoraMuted)
                }
                Spacer(Modifier.height(12.dp))
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(shops, key = { it.id }) { shop ->
                        val isActive = shop.id == activeShopId
                        Card(
                            onClick = {
                                shopStore.setActiveShopId(shop.id)
                                activeShopId = shop.id
                                openedShopId = shop.id
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isActive) Color(0xFFEAF4EE) else Color.White),
                            border = if (isActive) BorderStroke(1.dp, VendoraGreen.copy(alpha = 0.45f)) else null,
                        ) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(48.dp).background(if (isActive) VendoraGreen else Color(0xFFF0F3F1), RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
                                    Text(shop.name.firstOrNull()?.uppercase() ?: "S", color = if (isActive) Color.White else VendoraGreen, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(shop.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(4.dp))
                                    Text(shop.businessType, style = MaterialTheme.typography.bodyMedium, color = VendoraMuted)
                                }
                                if (isActive) {
                                    Surface(color = VendoraGreen, shape = RoundedCornerShape(100.dp)) {
                                        Text("ACTIVE", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                } else Text("›", color = VendoraMuted, style = MaterialTheme.typography.headlineMedium)
                            }
                        }
                    }
                    item {
                        OutlinedButton(onClick = { showCreateShop = true }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(54.dp), shape = RoundedCornerShape(16.dp)) {
                            Text("+   Add another shop", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                if (activeShopId != null) {
                    Spacer(Modifier.height(12.dp))
                    Text("Your active shop is saved on this device.", Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = VendoraMuted)
                }
            }
        }
    }

    if (showCreateShop && !loading && error == null) {
        CreateShopDialog(
            onDismiss = { if (shops.isNotEmpty()) showCreateShop = false },
            onCreate = { name, businessType ->
                scope.launch {
                    runCatching {
                        val created = ShopEntity(UUID.randomUUID().toString(), name.trim(), businessType)
                        repository.saveShop(created)
                        shops = repository.getShops().map { it.toShop() }
                        activeShopId = created.id
                        shopStore.setActiveShopId(created.id)
                        showCreateShop = false
                    }.onFailure { error = it.message ?: "Could not create this shop." }
                }
            },
        )
    }
}

@Composable
private fun CreateShopDialog(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var businessType by remember { mutableStateOf("General store") }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    val businessTypes = listOf("General store", "Grocery", "Fashion", "Electronics", "Hardware", "Stationery", "Pharmacy")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create a shop", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Give this business a name and choose the closest category.", color = VendoraMuted, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Shop name") },
                    placeholder = { Text("e.g. Rahman Store") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
                Column {
                    Text("Business type", style = MaterialTheme.typography.labelMedium, color = VendoraMuted)
                    Spacer(Modifier.height(6.dp))
                    Box {
                        OutlinedButton(onClick = { typeMenuExpanded = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                            Text(businessType, modifier = Modifier.weight(1f))
                            Text("⌄")
                        }
                        DropdownMenu(expanded = typeMenuExpanded, onDismissRequest = { typeMenuExpanded = false }) {
                            businessTypes.forEach { type ->
                                DropdownMenuItem(text = { Text(type) }, onClick = {
                                    businessType = type
                                    typeMenuExpanded = false
                                })
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onCreate(name.trim(), businessType) }, enabled = name.isNotBlank(), shape = RoundedCornerShape(12.dp)) {
                Text("Create shop")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        shape = RoundedCornerShape(24.dp),
    )
}
