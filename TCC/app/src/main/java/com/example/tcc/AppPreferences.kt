package com.example.tcc

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object AppPreferences {
    private const val PREFS = "tcc_preferences"
    private const val KEY_LOGGED_IN = "logged_in"
    private const val KEY_NAME = "name"
    private const val KEY_EMAIL = "email"
    private const val KEY_CPF = "cpf"
    private const val KEY_BIRTH_DATE = "birth_date"
    private const val KEY_PHONE = "phone"
    private const val KEY_DARK_THEME = "dark_theme"
    private const val KEY_FONT_SCALE = "font_scale"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isLoggedIn(context: Context): Boolean = prefs(context).getBoolean(KEY_LOGGED_IN, false)

    fun setLoggedIn(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_LOGGED_IN, value).apply()
    }

    fun saveProfile(context: Context, name: String, email: String, cpf: String = "") {
        prefs(context).edit()
            .putString(KEY_NAME, name.trim())
            .putString(KEY_EMAIL, email.trim())
            .putString(KEY_CPF, cpf.filter(Char::isDigit))
            .apply()
    }

    fun saveExtraProfile(context: Context, birthDate: String, phone: String) {
        prefs(context).edit()
            .putString(KEY_BIRTH_DATE, birthDate.trim())
            .putString(KEY_PHONE, phone.trim())
            .apply()
    }

    fun name(context: Context): String = prefs(context).getString(KEY_NAME, "") ?: ""
    fun email(context: Context): String = prefs(context).getString(KEY_EMAIL, "") ?: ""
    fun cpf(context: Context): String = prefs(context).getString(KEY_CPF, "") ?: ""
    fun birthDate(context: Context): String = prefs(context).getString(KEY_BIRTH_DATE, "") ?: ""
    fun phone(context: Context): String = prefs(context).getString(KEY_PHONE, "") ?: ""

    fun logout(context: Context) {
        prefs(context).edit().putBoolean(KEY_LOGGED_IN, false).apply()
    }

    fun isDarkTheme(context: Context): Boolean = false // Forçando modo claro para seguir o design

    fun setDarkTheme(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_DARK_THEME, enabled).apply()
        // applyTheme(enabled) // Comentado para manter o design claro
        applyTheme(false)
    }

    fun applyStoredTheme(context: Context) = applyTheme(false)

    private fun applyTheme(dark: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    fun fontScale(context: Context): Float = prefs(context).getFloat(KEY_FONT_SCALE, 1.0f)

    fun setFontScale(context: Context, scale: Float) {
        prefs(context).edit().putFloat(KEY_FONT_SCALE, scale).apply()
    }
}
