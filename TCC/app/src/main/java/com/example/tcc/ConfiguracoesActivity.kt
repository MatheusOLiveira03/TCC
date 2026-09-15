package com.example.tcc

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ConfiguracoesActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.configuracoes)

        findViewById<Button>(R.id.btnMudarTamanhoFonte).setOnClickListener { showFontSizeDialog() }

        findViewById<Button>(R.id.btnMudarTema).setOnClickListener {
            AppPreferences.setDarkTheme(this, !AppPreferences.isDarkTheme(this))
        }

        findViewById<Button>(R.id.btnVerInfoUsuario).setOnClickListener {
            startActivity(Intent(this, UsuarioActivity::class.java))
        }

        findViewById<Button>(R.id.btnSairPerfil).setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.sair_do_perfil)
                .setMessage(R.string.confirmar_saida)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.sair) { _, _ -> logout() }
                .show()
        }

        findViewById<Button>(R.id.btnVoltar).setOnClickListener { finish() }
    }

    private fun showFontSizeDialog() {
        val labels = arrayOf(
            getString(R.string.fonte_pequena),
            getString(R.string.fonte_padrao),
            getString(R.string.fonte_grande)
        )
        val scales = floatArrayOf(0.9f, 1.0f, 1.15f)
        val current = AppPreferences.fontScale(this)
        val checked = scales.indices.minByOrNull { kotlin.math.abs(scales[it] - current) } ?: 1

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.tamanho_da_fonte)
            .setSingleChoiceItems(labels, checked) { dialog, which ->
                AppPreferences.setFontScale(this, scales[which])
                dialog.dismiss()
                recreate()
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    private fun logout() {
        AppPreferences.logout(this)
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }
}
