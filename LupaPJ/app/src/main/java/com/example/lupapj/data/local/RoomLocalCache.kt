package com.example.lupapj.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// [추가됨(권)] 방 상태(장난감, 사료 위치 등)를 로컬 디스크에 캐싱하기 위한 DataStore
private val Context.roomDataStore: DataStore<Preferences> by preferencesDataStore(name = "room_cache")

class RoomLocalCache(context: Context) {
    private val dataStore = context.applicationContext.roomDataStore

    private object Keys {
        val DROPPED_FOOD_U = floatPreferencesKey("dropped_food_u")
        val DROPPED_FOOD_V = floatPreferencesKey("dropped_food_v")
        val DROPPED_TOY_U = floatPreferencesKey("dropped_toy_u")
        val DROPPED_TOY_V = floatPreferencesKey("dropped_toy_v")
        val IS_TOY_KNOCKED_OVER = booleanPreferencesKey("is_toy_knocked_over")
    }

    suspend fun saveDroppedFood(u: Float?, v: Float?) {
        dataStore.edit { prefs ->
            if (u == null || v == null) {
                prefs.remove(Keys.DROPPED_FOOD_U)
                prefs.remove(Keys.DROPPED_FOOD_V)
            } else {
                prefs[Keys.DROPPED_FOOD_U] = u
                prefs[Keys.DROPPED_FOOD_V] = v
            }
        }
    }

    suspend fun saveDroppedToy(u: Float?, v: Float?, isKnockedOver: Boolean = false) {
        dataStore.edit { prefs ->
            if (u == null || v == null) {
                prefs.remove(Keys.DROPPED_TOY_U)
                prefs.remove(Keys.DROPPED_TOY_V)
                prefs.remove(Keys.IS_TOY_KNOCKED_OVER)
            } else {
                prefs[Keys.DROPPED_TOY_U] = u
                prefs[Keys.DROPPED_TOY_V] = v
                prefs[Keys.IS_TOY_KNOCKED_OVER] = isKnockedOver
            }
        }
    }

    suspend fun saveToyKnockedOver(isKnockedOver: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.IS_TOY_KNOCKED_OVER] = isKnockedOver
        }
    }

    suspend fun getDroppedFood(): Pair<Float, Float>? {
        val prefs = dataStore.data.first()
        val u = prefs[Keys.DROPPED_FOOD_U]
        val v = prefs[Keys.DROPPED_FOOD_V]
        return if (u != null && v != null) u to v else null
    }

    suspend fun getDroppedToy(): Triple<Float, Float, Boolean>? {
        val prefs = dataStore.data.first()
        val u = prefs[Keys.DROPPED_TOY_U]
        val v = prefs[Keys.DROPPED_TOY_V]
        val knocked = prefs[Keys.IS_TOY_KNOCKED_OVER] ?: false
        return if (u != null && v != null) Triple(u, v, knocked) else null
    }
}
