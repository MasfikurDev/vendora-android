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
                    val item = array.getJSONObject(index)
                    add(
                        Shop(
                            id = item.getString("id"),
                            name = item.getString("name"),
                            businessType = item.getString("businessType"),
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun getActiveShopId(): String? = preferences.getString(KEY_ACTIVE_SHOP_ID, null)

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
        preferences.edit().putString(KEY_SHOPS, array.toString()).apply()
    }

    fun setActiveShopId(id: String) {
        preferences.edit().putString(KEY_ACTIVE_SHOP_ID, id).apply()
    }

    fun createShop(name: String, businessType: String): Shop {
        val shop = Shop(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            businessType = businessType,
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
