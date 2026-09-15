package com.example.tcc

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        val scale = AppPreferences.fontScale(newBase)
        val configuration = Configuration(newBase.resources.configuration).apply {
            fontScale = scale
        }
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppPreferences.applyStoredTheme(this)
        super.onCreate(savedInstanceState)
    }
}
