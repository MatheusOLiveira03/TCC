package com.example.tcc

import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ApontamentosActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.apontamentos)

        findViewById<Button>(R.id.btnAdicionar1).setOnClickListener {
            editNote(findViewById(R.id.tvTextoCard1))
        }
        findViewById<Button>(R.id.btnAdicionar2).setOnClickListener {
            editNote(findViewById(R.id.tvTextoCard2))
        }
        findViewById<Button>(R.id.btnVoltar).setOnClickListener { finish() }
    }

    private fun editNote(target: TextView) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 3
            setText(if (target.text == getString(R.string.apontamento_placeholder)) "" else target.text)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.adicionar_apontamento)
            .setView(input)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.salvar) { _, _ ->
                val text = input.text.toString().trim()
                if (text.isNotBlank()) target.text = text
            }
            .show()
    }
}
