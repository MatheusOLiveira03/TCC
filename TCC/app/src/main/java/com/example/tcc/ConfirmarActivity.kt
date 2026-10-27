package com.example.tcc

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.example.tcc.api.model.AtivacaoConsulta

class ConfirmarActivity : ApiActivity() {
    companion object { const val EXTRA_CODIGO = "extra_codigo_usuario" }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.confirmar_usuario)
        val token = intent.getStringExtra(EXTRA_CODIGO).orEmpty()
        val dados = findViewById<TextView>(R.id.tvDadosUsuario)
        val sim = findViewById<Button>(R.id.btnSim).apply { isEnabled = false }
        val nao = findViewById<Button>(R.id.btnNao).apply { isEnabled = false }
        dados.text = "Consultando a ativação..."
        chamar(api.consultar(AtivacaoConsulta(token)), publica = true, aoErro = { dados.text = it.mensagem }) { resumo ->
            if (resumo?.email.isNullOrBlank()) { dados.text = "Ativação inválida."; return@chamar }
            dados.text = "${resumo?.nome.orEmpty()}\n${resumo?.email.orEmpty()}"
            sim.isEnabled = true; nao.isEnabled = true
        }
        sim.setOnClickListener {
            startActivity(Intent(this, SenhaActivity::class.java).putExtra(EXTRA_CODIGO, token))
        }
        nao.setOnClickListener {
            startActivity(Intent(this, InformacoesCorretasActivity::class.java).putExtra(EXTRA_CODIGO, token))
        }
        findViewById<Button>(R.id.btnVoltar).setOnClickListener { finish() }
    }
}
