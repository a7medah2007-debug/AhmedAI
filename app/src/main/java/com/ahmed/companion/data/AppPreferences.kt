package com.ahmed.companion.data

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ahmed_companion_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_API_KEY = "deepseek_api_key"
        private const val KEY_WAKE_WORD = "wake_word"
        private const val KEY_IS_ACTIVE = "companion_active"
    }

    var deepSeekApiKey: String
        get() = prefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_API_KEY, value.trim()).apply()

    var wakeWord: String
        get() = prefs.getString(KEY_WAKE_WORD, "احمد") ?: "احمد"
        set(value) = prefs.edit().putString(KEY_WAKE_WORD, value.trim()).apply()

    var isCompanionActive: Boolean
        get() = prefs.getBoolean(KEY_IS_ACTIVE, true)
        set(value) = prefs.edit().putBoolean(KEY_IS_ACTIVE, value).apply()

    val hasApiKey: Boolean
        get() = deepSeekApiKey.isNotEmpty()
}
