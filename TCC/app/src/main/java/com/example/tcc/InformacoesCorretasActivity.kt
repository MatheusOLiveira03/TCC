package com.example.tcc

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText

class InformacoesCorretasActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.informacoes_corretas)

        val etNome = findViewById<EditText>(R.id.etNome)
        val etCpf = findViewById<EditText>(R.id.etCpf)
        val etEmail = findViewById<EditText>(R.id.etEmail)

        findViewById<Button>(R.id.btnContinuar).setOnClickListener {
            val nome = etNome.text.toString().trim()
            val cpf = etCpf.text.toString().trim()
            val email = etEmail.text.toString().trim()

            when {
                nome.length < 2 -> {
                    etNome.error = getString(R.string.erro_nome_invalido)
                    etNome.requestFocus()
                }
                !Validators.isCpfValid(cpf) -> {
                    etCpf.error = getString(R.string.erro_cpf_invalido)
                    etCpf.requestFocus()
                }
                !Validators.isEmailValid(email) -> {
                    etEmail.error = getString(R.string.erro_email_invalido)
                    etEmail.requestFocus()
                }
                else -> {
                    AppPreferences.saveProfile(this, nome, email, cpf)
                    startActivity(Intent(this, SenhaActivity::class.java))
                }
            }
        }

        findViewById<Button>(R.id.btnVoltar).setOnClickListener { finish() }
    }
}
