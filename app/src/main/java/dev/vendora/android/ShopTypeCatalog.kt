package dev.vendora.android

import android.content.Context
import org.json.JSONObject

/**
 * Loads the bundled, offline-first shop type catalog.
 *
 * Labels are presentation values. We currently persist the selected English label in
 * the legacy ShopEntity.businessType field to avoid changing existing Room records.
 * The stable catalog IDs are reserved for a future explicit schema migration.
 */
data class ShopTypeOption(
    val id: String,
    val labelEn: String,
    val labelBn: String,
    val aliases: List<String>,
    val osmTags: List<String>,
)

object ShopTypeCatalog {
    fun load(context: Context): List<ShopTypeOption> = runCatching {
        context.assets.open("catalog/shop_types.json").bufferedReader().use { it.readText() }
            .let(::JSONObject)
            .getJSONArray("shopTypes")
            .let { items ->
                (0 until items.length()).mapNotNull { index ->
                    val item = items.optJSONObject(index) ?: return@mapNotNull null
                    val id = item.optString("id").trim()
                    val labelEn = item.optString("labelEn").trim()
                    if (id.isBlank() || labelEn.isBlank()) return@mapNotNull null
                    ShopTypeOption(
                        id = id,
                        labelEn = labelEn,
                        labelBn = item.optString("labelBn", labelEn),
                        aliases = item.optJSONArray("aliases").toStringList(),
                        osmTags = item.optJSONArray("osmTags").toStringList(),
                    )
                }
            }
    }.getOrElse {
        // Keep shop creation usable even if a future catalog asset is damaged.
        listOf(
            ShopTypeOption("general_store", "General store", "সাধারণ দোকান", emptyList(), listOf("shop=general")),
            ShopTypeOption("grocery", "Grocery / convenience", "মুদি দোকান", emptyList(), listOf("shop=convenience")),
            ShopTypeOption("other", "Other / mixed business", "অন্যান্য", emptyList(), listOf("shop=yes")),
        )
    }

    private fun org.json.JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { optString(it).trim().takeIf(String::isNotEmpty) }
    }
}
