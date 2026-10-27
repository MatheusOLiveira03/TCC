package com.example.tcc

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.example.tcc.api.model.AtivacaoConsulta

class InformacoesCorretasActivity : ApiActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.informacoes_corretas)
        findViewById<TextView>(R.id.tvTitulo).text = "Solicite a correção ao profissional"
        val nome = findViewById<EditText>(R.id.etNome).apply { isEnabled = false }
        val email = findViewById<EditText>(R.id.etEmail).apply { isEnabled = false }
        findViewById<EditText>(R.id.etCpf).isEnabled = false
        val token = intent.getStringExtra(ConfirmarActivity.EXTRA_CODIGO).orEmpty()
        chamar(api.consultar(AtivacaoConsulta(token)), publica = true) { r ->
            nome.setText(r?.nome); email.setText(r?.email)
        }
        findViewById<Button>(R.id.btnContinuar).apply {
            text = "Voltar ao primeiro acesso"
            setOnClickListener {
                aviso("Peça ao profissional que corrija o cadastro e entregue um novo link. Os dados não foram alterados.")
                abrir(CadastroActivity::class.java); finish()
            }
        }
        findViewById<Button>(R.id.btnVoltar).setOnClickListener { finish() }
    }
}
