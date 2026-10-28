package dev.vendora.android

import android.content.Context
import org.json.JSONObject
import java.util.Locale

data class ProductCatalogSuggestions(
    val brands: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val exampleProducts: List<String> = emptyList(),
)

/** Loads offline, editable suggestions; it never seeds inventory or pricing. */
object HomeApplianceCatalog {
    fun load(context: Context): HomeApplianceCatalogData = runCatching {
        val root = context.assets.open("catalog/home_appliances.json")
            .bufferedReader().use { it.readText() }.let(::JSONObject)
        fun readList(key: String): List<String> {
            val array = root.getJSONArray(key)
            return (0 until array.length()).map { array.getString(it) }
        }
        HomeApplianceCatalogData(
            shopTypeIds = readList("shopTypeIds"),
            brands = readList("brands"),
            categories = readList("categories"),
            exampleProducts = readList("exampleProducts"),
        )
    }.getOrElse {
        HomeApplianceCatalogData(emptyList(), emptyList(), emptyList(), emptyList())
    }
}

data class HomeApplianceCatalogData(
    val shopTypeIds: List<String>,
    val brands: List<String>,
    val categories: List<String>,
    val exampleProducts: List<String>,
) {
    fun suggestionsFor(businessType: String): ProductCatalogSuggestions {
        val normalized = businessType.trim().lowercase(Locale.ROOT)
        val applianceShop = normalized.contains("electronics") ||
            normalized.contains("home appliance") ||
            normalized.contains("appliance store") ||
            normalized in shopTypeIds.map { it.lowercase(Locale.ROOT) }
        if (!applianceShop) return ProductCatalogSuggestions()
        return ProductCatalogSuggestions(brands, categories, exampleProducts)
    }
}
