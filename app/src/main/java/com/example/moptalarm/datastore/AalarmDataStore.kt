package com.example.moptalarm.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.moptalarm.model.AlarmData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private const val DATASTORE_NAME = "alarms"
val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)

object AlarmDataStore {
    private val ALARM_LIST_KEY = stringPreferencesKey("alarm_list")

    fun getAlarms(context: Context): Flow<List<AlarmData>> {
        return context.dataStore.data.map { preferences ->
            val json = preferences[ALARM_LIST_KEY] ?: "[]"
            try {
                Json.decodeFromString(json)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun saveAlarms(context: Context, alarms: List<AlarmData>) {
        val json = Json.encodeToString(alarms)
        context.dataStore.edit { preferences ->
            preferences[ALARM_LIST_KEY] = json
        }
    }
}
