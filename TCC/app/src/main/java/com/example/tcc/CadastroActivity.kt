package com.example.tcc

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText

class CadastroActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.cadastro)

        val etCodigoUsuario = findViewById<EditText>(R.id.etCodigoUsuario)
        findViewById<Button>(R.id.btnCadastrar).setOnClickListener {
            val codigo = etCodigoUsuario.text.toString().trim()
            if (codigo.isBlank()) {
                etCodigoUsuario.error = getString(R.string.erro_codigo_obrigatorio)
                etCodigoUsuario.requestFocus()
                return@setOnClickListener
            }

            startActivity(
                Intent(this, ConfirmarActivity::class.java)
                    .putExtra(ConfirmarActivity.EXTRA_CODIGO, codigo)
            )
        }

        findViewById<Button>(R.id.btnVoltar).setOnClickListener { finish() }
    }
}
