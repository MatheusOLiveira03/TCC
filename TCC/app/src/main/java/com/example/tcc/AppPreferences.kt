package com.example.tcc

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate

object AppPreferences {
    private fun prefs(c: Context) = c.getSharedPreferences("tcc_preferences", Context.MODE_PRIVATE)
    fun isLoggedIn(c: Context) = Sessao.token(c).isNotBlank()
    fun logout(c: Context) = Sessao.limpar(c)
    internal fun limparDadosAntigos(c: Context) {
        val editor = prefs(c).edit()
        listOf("logged_in", "name", "email", "cpf", "birth_date", "phone").forEach { editor.remove(it) }
        editor.apply()
    }
    fun isDarkTheme(c: Context): Boolean {
        val p = prefs(c)
        return if (p.contains("dark_theme")) p.getBoolean("dark_theme", false)
        else (c.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }
    fun setDarkTheme(c: Context, enabled: Boolean) {
        prefs(c).edit().putBoolean("dark_theme", enabled).apply()
        AppCompatDelegate.setDefaultNightMode(if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
    }
    fun applyStoredTheme(c: Context) {
        val p = prefs(c)
        AppCompatDelegate.setDefaultNightMode(if (!p.contains("dark_theme")) AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else if (p.getBoolean("dark_theme", false)) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
    }
    fun fontScale(c: Context) = prefs(c).getFloat("font_scale", 1f)
    fun setFontScale(c: Context, scale: Float) { prefs(c).edit().putFloat("font_scale", scale).apply() }
}
