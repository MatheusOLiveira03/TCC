package com.example.tcc

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class ConfirmarActivity : BaseActivity() {
    companion object {
        const val EXTRA_CODIGO = "extra_codigo_usuario"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.confirmar_usuario)

        val codigo = intent.getStringExtra(EXTRA_CODIGO).orEmpty()
        findViewById<TextView>(R.id.tvDadosUsuario).text =
            getString(R.string.codigo_informado, codigo.ifBlank { getString(R.string.nao_informado) })

        findViewById<Button>(R.id.btnNao).setOnClickListener {
            startActivity(
                Intent(this, InformacoesCorretasActivity::class.java)
                    .putExtra(EXTRA_CODIGO, codigo)
            )
        }

        findViewById<Button>(R.id.btnSim).setOnClickListener {
            startActivity(
                Intent(this, SenhaActivity::class.java)
                    .putExtra(EXTRA_CODIGO, codigo)
            )
        }

        findViewById<Button>(R.id.btnVoltar).setOnClickListener { finish() }
    }
}
