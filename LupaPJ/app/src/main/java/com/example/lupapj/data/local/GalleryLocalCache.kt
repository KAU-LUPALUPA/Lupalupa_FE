package com.example.lupapj.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.lupapj.data.model.GalleryEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.galleryDataStore: DataStore<Preferences> by preferencesDataStore(name = "gallery_cache")

interface GalleryCache {
    fun getGalleryItems(): Flow<List<GalleryEntity>>
    suspend fun saveGalleryItem(item: GalleryEntity)
    suspend fun deleteGalleryItem(id: String)
    suspend fun updateGalleryItem(item: GalleryEntity)
    suspend fun getUnbackedUpItems(): List<GalleryEntity>
    suspend fun markAsBackedUp(localId: String, serverImageId: String)
}

class GalleryLocalCache(private val context: Context) : GalleryCache {
    private val dataStore = context.applicationContext.galleryDataStore
    private val gson = Gson()

    private object Keys {
        val GALLERY_ITEMS_JSON = stringPreferencesKey("gallery_items_json")
    }

    override fun getGalleryItems(): Flow<List<GalleryEntity>> {
        return dataStore.data.map { prefs ->
            val json = prefs[Keys.GALLERY_ITEMS_JSON]
            if (json.isNullOrBlank()) {
                emptyList()
            } else {
                val type = object : TypeToken<List<GalleryEntity>>() {}.type
                gson.fromJson(json, type)
            }
        }
    }

    override suspend fun saveGalleryItem(item: GalleryEntity) {
        dataStore.edit { prefs ->
            val json = prefs[Keys.GALLERY_ITEMS_JSON]
            val items: MutableList<GalleryEntity> = if (json.isNullOrBlank()) {
                mutableListOf()
            } else {
                val type = object : TypeToken<MutableList<GalleryEntity>>() {}.type
                gson.fromJson(json, type)
            }
            // Add to front (newest first)
            items.add(0, item)
            prefs[Keys.GALLERY_ITEMS_JSON] = gson.toJson(items)
        }
    }

    override suspend fun deleteGalleryItem(id: String) {
        dataStore.edit { prefs ->
            val json = prefs[Keys.GALLERY_ITEMS_JSON]
            if (!json.isNullOrBlank()) {
                val type = object : TypeToken<MutableList<GalleryEntity>>() {}.type
                val items: MutableList<GalleryEntity> = gson.fromJson(json, type)
                items.removeAll { it.id == id }
                prefs[Keys.GALLERY_ITEMS_JSON] = gson.toJson(items)
            }
        }
    }

    override suspend fun updateGalleryItem(item: GalleryEntity) {
        dataStore.edit { prefs ->
            val json = prefs[Keys.GALLERY_ITEMS_JSON]
            if (!json.isNullOrBlank()) {
                val type = object : TypeToken<MutableList<GalleryEntity>>() {}.type
                val items: MutableList<GalleryEntity> = gson.fromJson(json, type)
                val index = items.indexOfFirst { it.id == item.id }
                if (index != -1) {
                    items[index] = item
                    prefs[Keys.GALLERY_ITEMS_JSON] = gson.toJson(items)
                }
            }
        }
    }

    override suspend fun getUnbackedUpItems(): List<GalleryEntity> {
        var unbackedUpList = emptyList<GalleryEntity>()
        dataStore.edit { prefs ->
            val json = prefs[Keys.GALLERY_ITEMS_JSON]
            if (!json.isNullOrBlank()) {
                val type = object : TypeToken<List<GalleryEntity>>() {}.type
                val items: List<GalleryEntity> = gson.fromJson(json, type)
                unbackedUpList = items.filter { !it.isBackedUp }
            }
        }
        return unbackedUpList
    }

    override suspend fun markAsBackedUp(localId: String, serverImageId: String) {
        dataStore.edit { prefs ->
            val json = prefs[Keys.GALLERY_ITEMS_JSON]
            if (!json.isNullOrBlank()) {
                val type = object : TypeToken<MutableList<GalleryEntity>>() {}.type
                val items: MutableList<GalleryEntity> = gson.fromJson(json, type)
                val index = items.indexOfFirst { it.id == localId }
                if (index != -1) {
                    items[index] = items[index].copy(
                        isBackedUp = true,
                        serverImageId = serverImageId
                    )
                    prefs[Keys.GALLERY_ITEMS_JSON] = gson.toJson(items)
                }
            }
        }
    }
}
