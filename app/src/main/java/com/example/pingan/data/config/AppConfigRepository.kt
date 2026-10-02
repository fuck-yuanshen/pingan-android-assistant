package com.example.pingan.data.config

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class AppConfigRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var config by mutableStateOf(loadConfig())
        private set

    fun save(nextConfig: AppConfig) {
        preferences.edit()
            .putString(KEY_CONFIG, AppConfigJson.encode(nextConfig))
            .apply()
        config = nextConfig
    }

    fun resetToDefaults() {
        preferences.edit().remove(KEY_CONFIG).apply()
        config = AppConfigDefaults.defaultConfig
    }

    private fun loadConfig(): AppConfig {
        val raw = preferences.getString(KEY_CONFIG, null) ?: return AppConfigDefaults.defaultConfig
        return runCatching { AppConfigJson.decode(raw) }.getOrElse { AppConfigDefaults.defaultConfig }
    }

    private companion object {
        const val PREFS_NAME = "pingan_app_config"
        const val KEY_CONFIG = "config_json"
    }
}
