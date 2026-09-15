package com.example.tcc

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvCadastrar = findViewById<TextView>(R.id.tvCadastrar)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            when {
                !Validators.isEmailValid(email) -> {
                    etEmail.error = getString(R.string.erro_email_invalido)
                    etEmail.requestFocus()
                }
                password.isBlank() -> {
                    etPassword.error = getString(R.string.erro_senha_obrigatoria)
                    etPassword.requestFocus()
                }
                else -> {
                    // Fluxo local de protótipo. A autenticação real deve substituir este bloco
                    // quando o contrato do backend Spring/JWT estiver definido.
                    val currentName = AppPreferences.name(this)
                    val fallbackName = email.substringBefore('@').replaceFirstChar { it.uppercase() }
                    AppPreferences.saveProfile(this, currentName.ifBlank { fallbackName }, email, AppPreferences.cpf(this))
                    AppPreferences.setLoggedIn(this, true)
                    openDashboard()
                }
            }
        }

        tvCadastrar.setOnClickListener {
            startActivity(Intent(this, CadastroActivity::class.java))
        }
    }

    private fun openDashboard() {
        startActivity(
            Intent(this, DashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }
}
