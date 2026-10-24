package dev.vendora.android

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Shop(
    val id: String,
    val name: String,
    val businessType: String,
)

class ShopStore(context: Context) {
    private val preferences = context.getSharedPreferences("vendora_shops", Context.MODE_PRIVATE)

    fun getShops(): List<Shop> {
        val encoded = preferences.getString(KEY_SHOPS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(encoded)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString("id").trim()
                    val name = item.optString("name").trim()
                    val businessType = item.optString("businessType").trim()
                    if (id.isNotEmpty() && name.isNotEmpty() && businessType.isNotEmpty()) {
                        add(Shop(id = id, name = name, businessType = businessType))
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    /** Return a valid selection, repairing a stale saved ID when necessary. */
    fun getActiveShopId(): String? {
        val shops = getShops()
        if (shops.isEmpty()) {
            preferences.edit().remove(KEY_ACTIVE_SHOP_ID).apply()
            return null
        }

        val savedId = preferences.getString(KEY_ACTIVE_SHOP_ID, null)
        if (savedId != null && shops.any { it.id == savedId }) return savedId
        return shops.first().id.also(::setActiveShopId)
    }

    fun saveShops(shops: List<Shop>) {
        val array = JSONArray()
        shops.forEach { shop ->
            array.put(
                JSONObject()
                    .put("id", shop.id)
                    .put("name", shop.name)
                    .put("businessType", shop.businessType)
            )
        }

        val editor = preferences.edit().putString(KEY_SHOPS, array.toString())
        val activeId = preferences.getString(KEY_ACTIVE_SHOP_ID, null)
        if (activeId != null && shops.none { it.id == activeId }) {
            editor.remove(KEY_ACTIVE_SHOP_ID)
        }
        editor.apply()
    }

    fun setActiveShopId(id: String) {
        require(getShops().any { it.id == id }) { "Choose an existing shop." }
        preferences.edit().putString(KEY_ACTIVE_SHOP_ID, id).apply()
    }

    fun createShop(name: String, businessType: String): Shop {
        val normalizedName = name.trim()
        val normalizedType = businessType.trim()
        require(normalizedName.isNotEmpty()) { "Enter a shop name." }
        require(normalizedType.isNotEmpty()) { "Choose a business type." }
        require(getShops().none { it.name.equals(normalizedName, ignoreCase = true) }) {
            "A shop with this name already exists."
        }

        val shop = Shop(
            id = UUID.randomUUID().toString(),
            name = normalizedName,
            businessType = normalizedType,
        )
        saveShops(getShops() + shop)
        setActiveShopId(shop.id)
        return shop
    }

    companion object {
        private const val KEY_SHOPS = "shops"
        private const val KEY_ACTIVE_SHOP_ID = "active_shop_id"
    }
}
