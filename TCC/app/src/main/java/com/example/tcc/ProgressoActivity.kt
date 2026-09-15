package com.example.tcc

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ProgressoActivity : BaseActivity() {
    private var expanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.progresso)

        val tvArea = findViewById<TextView>(R.id.tvRotuloArea)
        val tvDescricao = findViewById<TextView>(R.id.tvTituloDescricao)
        val btnVerMais = findViewById<Button>(R.id.btnVerMais)

        findViewById<Button>(R.id.btnSelecionarArea).setOnClickListener {
            val areas = resources.getStringArray(R.array.areas_progresso)
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.selecionar_area)
                .setItems(areas) { _, which -> tvArea.text = areas[which] }
                .show()
        }

        btnVerMais.setOnClickListener {
            expanded = !expanded
            tvDescricao.text = getString(if (expanded) R.string.progresso_descricao_expandida else R.string.progresso_descricao_resumida)
            btnVerMais.text = getString(if (expanded) R.string.ver_menos else R.string.ver_mais)
        }

        findViewById<Button>(R.id.btnVoltar).setOnClickListener { finish() }
    }
}
