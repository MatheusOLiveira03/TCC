package com.example.tcc

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView

class DashboardActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!AppPreferences.isLoggedIn(this)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.dashboard)

        val name = AppPreferences.name(this).ifBlank { getString(R.string.usuario) }
        findViewById<TextView>(R.id.tvNomeUsuario).text = getString(R.string.ola_usuario, name)

        findViewById<ImageButton>(R.id.btnConfig).setOnClickListener {
            startActivity(Intent(this, ConfiguracoesActivity::class.java))
        }
        findViewById<Button>(R.id.btnVerMais).setOnClickListener {
            startActivity(Intent(this, ProgressoActivity::class.java))
        }
        findViewById<Button>(R.id.btnAcompanharEquipe).setOnClickListener {
            startActivity(Intent(this, EquipeActivity::class.java))
        }
        findViewById<Button>(R.id.btnVerApontamentos).setOnClickListener {
            startActivity(Intent(this, ApontamentosActivity::class.java))
        }
    }
}
