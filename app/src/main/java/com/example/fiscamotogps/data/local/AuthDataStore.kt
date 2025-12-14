package com.example.fiscamotogps.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.fiscamotogps.domain.model.AuthSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.authPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "auth_preferences"
)

class AuthDataStore(private val context: Context) {

    private object Keys {
        val TOKEN = stringPreferencesKey("token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_DATA = stringPreferencesKey("user_data")
        val USERNAME = stringPreferencesKey("username")
        val PASSWORD = stringPreferencesKey("password")
        val USER_ID = stringPreferencesKey("user_id")
    }

    private val dataStore = context.authPreferencesDataStore

    val authSessionFlow: Flow<AuthSession?> = dataStore.data.map { preferences ->
        val token = preferences[Keys.TOKEN] ?: return@map null
        AuthSession(
            token = token,
            refreshToken = preferences[Keys.REFRESH_TOKEN],
            userName = preferences[Keys.USER_NAME].orEmpty(),
            rawUserData = preferences[Keys.USER_DATA].orEmpty(),
            username = preferences[Keys.USERNAME].orEmpty(),
            password = preferences[Keys.PASSWORD],
            userId = preferences[Keys.USER_ID]
        )
    }

    suspend fun saveSession(session: AuthSession) {
        dataStore.edit { preferences ->
            preferences[Keys.TOKEN] = session.token
            session.refreshToken?.let { preferences[Keys.REFRESH_TOKEN] = it }
            preferences[Keys.USER_NAME] = session.userName
            preferences[Keys.USER_DATA] = session.rawUserData
            preferences[Keys.USERNAME] = session.username
            session.password?.let { preferences[Keys.PASSWORD] = it }
            session.userId?.let { preferences[Keys.USER_ID] = it }
        }
    }

    suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(Keys.TOKEN)
            preferences.remove(Keys.REFRESH_TOKEN)
            preferences.remove(Keys.USER_NAME)
            preferences.remove(Keys.USER_DATA)
            preferences.remove(Keys.USERNAME)
            preferences.remove(Keys.PASSWORD)
            preferences.remove(Keys.USER_ID)
        }
    }
}


