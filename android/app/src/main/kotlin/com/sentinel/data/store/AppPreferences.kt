package com.sentinel.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sentinel_prefs")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        val BASE_URL = stringPreferencesKey("base_url")
        val ONBOARDED = booleanPreferencesKey("onboarded")
        const val DEFAULT_URL = ""
    }

    val baseUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[BASE_URL] ?: DEFAULT_URL
    }

    val onboarded: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[ONBOARDED] ?: false
    }

    suspend fun setBaseUrl(url: String) {
        context.dataStore.edit {
            it[BASE_URL] = url.trimEnd('/')
            it[ONBOARDED] = true
        }
    }
}
