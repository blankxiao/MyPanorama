package cn.szu.blankxiao.panoramaview.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "auth")

/**
 * 基于 DataStore 的 token 与用户信息持久化，全局单例通过 companion 持有。
 */
class TokenManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: TokenManager? = null

        fun getInstance(context: Context): TokenManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TokenManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        private val KEY_TOKEN = stringPreferencesKey("token")
        private val KEY_USER_ID = longPreferencesKey("user_id")
        private val KEY_EMAIL = stringPreferencesKey("email")
        private val KEY_USERNAME = stringPreferencesKey("username")
    }

    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[KEY_TOKEN] }
    val userIdFlow: Flow<Long?> = context.dataStore.data.map { it[KEY_USER_ID] }
    val emailFlow: Flow<String?> = context.dataStore.data.map { it[KEY_EMAIL] }
    val usernameFlow: Flow<String?> = context.dataStore.data.map { it[KEY_USERNAME] }

    val isLoggedInFlow: Flow<Boolean> = tokenFlow.map { !it.isNullOrBlank() }

    suspend fun getToken(): String? = tokenFlow.first()

    suspend fun saveLogin(token: String, userId: Long?, email: String?, username: String?) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TOKEN] = token
            userId?.let { prefs[KEY_USER_ID] = it }
            email?.let { prefs[KEY_EMAIL] = it }
            username?.let { prefs[KEY_USERNAME] = it }
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
