package com.example.tcc

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText

class SenhaActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.criar_senha)

        val etSenha = findViewById<EditText>(R.id.etSenha)
        val etConfirmarSenha = findViewById<EditText>(R.id.etConfirmarSenha)

        findViewById<Button>(R.id.btnCriarSenha).setOnClickListener {
            val senha = etSenha.text.toString()
            val confirmar = etConfirmarSenha.text.toString()

            when {
                !Validators.isPasswordValid(senha) -> {
                    etSenha.error = getString(R.string.erro_senha_minimo)
                    etSenha.requestFocus()
                }
                senha != confirmar -> {
                    etConfirmarSenha.error = getString(R.string.erro_senhas_diferentes)
                    etConfirmarSenha.requestFocus()
                }
                else -> {
                    AppPreferences.setLoggedIn(this, true)
                    startActivity(
                        Intent(this, DashboardActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                    finish()
                }
            }
        }

        findViewById<Button>(R.id.btnVoltar).setOnClickListener { finish() }
    }
}
