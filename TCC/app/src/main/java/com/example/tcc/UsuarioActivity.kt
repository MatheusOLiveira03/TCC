package com.example.tcc

import android.os.Bundle
import android.widget.Button
import android.widget.EditText

class UsuarioActivity : BaseActivity() {
    private lateinit var etNome: EditText
    private lateinit var etEmail: EditText
    private lateinit var etCpf: EditText
    private lateinit var etDataNascimento: EditText
    private lateinit var etCelular: EditText
    private lateinit var btnEditarSalvar: Button
    private var editing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.usuario)

        etNome = findViewById(R.id.etUsuarioNome)
        etEmail = findViewById(R.id.etUsuarioEmail)
        etCpf = findViewById(R.id.etUsuarioCpf)
        etDataNascimento = findViewById(R.id.etUsuarioDataNascimento)
        etCelular = findViewById(R.id.etUsuarioCelular)
        btnEditarSalvar = findViewById(R.id.btnEditar)

        loadProfile()
        setEditing(false)

        btnEditarSalvar.setOnClickListener {
            if (editing) saveProfile() else setEditing(true)
        }
        findViewById<Button>(R.id.btnVoltar).setOnClickListener { finish() }
    }

    private fun loadProfile() {
        etNome.setText(AppPreferences.name(this))
        etEmail.setText(AppPreferences.email(this))
        etCpf.setText(AppPreferences.cpf(this))
        etDataNascimento.setText(AppPreferences.birthDate(this))
        etCelular.setText(AppPreferences.phone(this))
    }

    private fun setEditing(enabled: Boolean) {
        editing = enabled
        listOf(etNome, etEmail, etCpf, etDataNascimento, etCelular).forEach { it.isEnabled = enabled }
        btnEditarSalvar.text = getString(if (enabled) R.string.salvar else R.string.editar)
    }

    private fun saveProfile() {
        val nome = etNome.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val cpf = etCpf.text.toString().trim()

        when {
            nome.length < 2 -> etNome.error = getString(R.string.erro_nome_invalido)
            !Validators.isEmailValid(email) -> etEmail.error = getString(R.string.erro_email_invalido)
            cpf.isNotBlank() && !Validators.isCpfValid(cpf) -> etCpf.error = getString(R.string.erro_cpf_invalido)
            else -> {
                AppPreferences.saveProfile(this, nome, email, cpf)
                AppPreferences.saveExtraProfile(
                    this,
                    etDataNascimento.text.toString(),
                    etCelular.text.toString()
                )
                setEditing(false)
            }
        }
    }
}
