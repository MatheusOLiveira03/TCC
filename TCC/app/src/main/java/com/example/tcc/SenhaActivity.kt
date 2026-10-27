package com.example.tcc

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import com.example.tcc.api.model.AtivacaoEntrada

class SenhaActivity : ApiActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.criar_senha)
        val token = intent.getStringExtra(ConfirmarActivity.EXTRA_CODIGO).orEmpty()
        if (token.isBlank()) { aviso("Informe primeiro o link de ativação."); finish(); return }
        val senha = findViewById<EditText>(R.id.etSenha).apply { isSaveEnabled = false }
        val confirmar = findViewById<EditText>(R.id.etConfirmarSenha).apply { isSaveEnabled = false }
        val salvar = findViewById<Button>(R.id.btnCriarSenha)
        salvar.setOnClickListener {
            val valor = senha.text.toString()
            when {
                !Validators.isPasswordValid(valor) -> senha.error = "Use pelo menos 8 caracteres e no máximo 72 bytes."
                valor != confirmar.text.toString() -> confirmar.error = getString(R.string.erro_senhas_diferentes)
                else -> chamar(api.ativar(AtivacaoEntrada(token, valor)), publica = true, botao = salvar,
                    aoErro = { erro -> aviso(if (erro.status == 0)
                        "Não foi possível confirmar a ativação. Tente entrar com a senha que acabou de definir."
                        else erro.mensagem) }) { resposta ->
                    runCatching { Sessao.salvar(this, requireNotNull(resposta)) }
                        .onSuccess {
                            startActivity(Intent(this, DashboardActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)); finish()
                        }.onFailure { aviso("Não foi possível guardar a sessão. Tente entrar com sua senha.") }
                }
            }
        }
        findViewById<Button>(R.id.btnVoltar).setOnClickListener { finish() }
    }
}
