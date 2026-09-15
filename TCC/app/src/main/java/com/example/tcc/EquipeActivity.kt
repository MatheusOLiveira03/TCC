package com.example.tcc

import android.os.Bundle
import android.widget.Button
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class EquipeActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.acompanhar_equipe)

        findViewById<Button>(R.id.btnVerMais1).setOnClickListener {
            showProfessional(getString(R.string.profissional_1))
        }
        findViewById<Button>(R.id.btnVerMais2).setOnClickListener {
            showProfessional(getString(R.string.profissional_2))
        }
        findViewById<Button>(R.id.btnVoltar).setOnClickListener { finish() }
    }

    private fun showProfessional(name: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(name)
            .setMessage(R.string.dados_equipe_api_pendente)
            .setPositiveButton(R.string.ok, null)
            .show()
    }
}
