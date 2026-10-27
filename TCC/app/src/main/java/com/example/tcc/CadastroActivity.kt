package com.example.tcc

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import com.google.android.material.textfield.TextInputLayout

class CadastroActivity : ApiActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.cadastro)
        val codigo = findViewById<EditText>(R.id.etCodigoUsuario).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            isSaveEnabled = false
        }
        (codigo.parent.parent as? TextInputLayout)?.hint = "Link ou código de ativação"
        findViewById<Button>(R.id.btnCadastrar).setOnClickListener {
            runCatching { Ativacao.token(codigo.text.toString()) }
                .onSuccess { token ->
                    startActivity(Intent(this, ConfirmarActivity::class.java)
                        .putExtra(ConfirmarActivity.EXTRA_CODIGO, token))
                }.onFailure { codigo.error = it.message }
        }
        findViewById<Button>(R.id.btnVoltar).setOnClickListener { finish() }
    }
}
