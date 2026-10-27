package com.example.tcc

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.tcc.api.model.LoginRequest
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : ApiActivity() {
    private lateinit var entrar: Button
    private lateinit var cadastro: TextView
    private var ocupado = false
    private var segundoFator: AlertDialog? = null
    private var temporizador: CountDownTimer? = null
    private var bloqueadoAte = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        entrar = findViewById(R.id.btnLogin)
        val email = findViewById<EditText>(R.id.etEmail)
        val senha = findViewById<EditText>(R.id.etPassword).apply { isSaveEnabled = false }
        bloqueadoAte = savedInstanceState?.getLong("bloqueadoAte") ?: 0
        cadastro = findViewById<TextView>(R.id.tvCadastrar).apply {
            text = "Primeiro acesso do aluno"
            setOnClickListener { abrir(CadastroActivity::class.java) }
        }
        if (bloqueadoAte > System.currentTimeMillis()) aguardar()
        entrar.setOnClickListener {
            when {
                !Validators.isEmailValid(email.text.toString()) -> email.error = getString(R.string.erro_email_invalido)
                senha.text.isBlank() -> senha.error = getString(R.string.erro_senha_obrigatoria)
                else -> logar(email.text.toString().trim(), senha.text.toString())
            }
        }
        if (Sessao.token(this).isNotBlank()) {
            ocupado = true; cadastro.isEnabled = false
            chamar(api.me(), botao = if (entrar.isEnabled) entrar else null,
                aoErro = { ocupado = false; cadastro.isEnabled = true; aviso(it.mensagem) }) { u ->
                ocupado = false; cadastro.isEnabled = true
                if (u == null) aviso("Resposta inválida do servidor.")
                else { Sessao.atualizar(this, u); dashboard() }
            }
        }
    }
    private fun logar(email: String, senha: String, codigo: String? = null) {
        if (ocupado || System.currentTimeMillis() < bloqueadoAte) return
        val botao = segundoFator?.takeIf { it.isShowing }?.getButton(AlertDialog.BUTTON_POSITIVE) ?: entrar
        if (!botao.isEnabled) return
        ocupado = true; cadastro.isEnabled = false
        chamar(api.login(LoginRequest(email, senha, codigo)), publica = true, botao = botao,
            aoErro = { erro ->
                ocupado = false; cadastro.isEnabled = true
                when {
                    erro.status == 428 && erro.codigo == "MFA_REQUIRED" -> pedirCodigo(email, senha)
                    erro.status == 429 -> {
                        bloqueadoAte = System.currentTimeMillis() + (erro.espera.takeIf { it > 0 } ?: 60) * 1000
                        aviso(erro.mensagem); aguardar()
                    }
                    else -> aviso(erro.mensagem)
                }
            }) { resposta ->
                ocupado = false; cadastro.isEnabled = true
                runCatching { Sessao.salvar(this, requireNotNull(resposta)) }
                    .onSuccess { dashboard() }
                    .onFailure { aviso("Não foi possível guardar uma sessão válida. Tente entrar novamente.") }
            }
    }
    private fun pedirCodigo(email: String, senha: String) {
        if (segundoFator?.isShowing == true) return
        val input = EditText(this).apply {
            hint = "Código do autenticador ou de recuperação"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            isSaveEnabled = false
        }
        segundoFator = dialogo(MaterialAlertDialogBuilder(this)
            .setTitle("Verificação em duas etapas").setView(input)
            .setNegativeButton(R.string.cancelar, null).setPositiveButton("Confirmar", null))
        segundoFator!!.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val codigo = input.text.toString().trim()
            if (codigo.isBlank()) input.error = "Informe o código." else logar(email, senha, codigo)
        }
    }
    private fun aguardar() {
        temporizador?.cancel()
        val restante = bloqueadoAte - System.currentTimeMillis()
        if (restante <= 0) return
        entrar.isEnabled = false
        temporizador = object : CountDownTimer(restante, 1000) {
            override fun onTick(ms: Long) {
                entrar.text = "Aguarde ${ms / 1000 + 1}s"
                segundoFator?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
            }
            override fun onFinish() {
                bloqueadoAte = 0; entrar.isEnabled = true; entrar.text = "Entrar"
                segundoFator?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = true
            }
        }.start()
    }
    private fun dashboard() {
        startActivity(Intent(this, DashboardActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong("bloqueadoAte", bloqueadoAte); super.onSaveInstanceState(outState)
    }
    override fun onDestroy() { temporizador?.cancel(); super.onDestroy() }
}
