package com.example.tcc

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.example.tcc.api.model.UsuarioResponse
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DashboardActivity : ApiActivity() {
    private var alunos = emptyList<UsuarioResponse>()
    private var selecionado = 0L
    private var cargaPainel = 0
    private var cargaMetas = 0
    private var seletor: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!sessaoValida()) return
        setContentView(R.layout.dashboard)
        selecionado = savedInstanceState?.getLong("selecionado") ?: 0
        findViewById<ImageButton>(R.id.btnConfig).setOnClickListener { abrir(ConfiguracoesActivity::class.java) }
        findViewById<Button>(R.id.btnVerMais).isEnabled = false
        findViewById<Button>(R.id.btnVerApontamentos).isEnabled = false
        findViewById<TextView>(R.id.tvProgresso).text = "Carregando metas..."
        findViewById<Button>(R.id.btnVerMais).setOnClickListener { abrir(ProgressoActivity::class.java, selecionado) }
        findViewById<Button>(R.id.btnVerApontamentos).setOnClickListener { abrir(ApontamentosActivity::class.java, selecionado) }
        findViewById<Button>(R.id.btnAcompanharEquipe).setOnClickListener { abrir(EquipeActivity::class.java, selecionado) }
        if (profissional()) {
            val nome = findViewById<TextView>(R.id.tvNomeUsuario)
            val pai = nome.parent as LinearLayout
            seletor = modelo<Button>(R.layout.progresso, R.id.btnSelecionarArea).apply {
                text = "Selecionar aluno"
                setOnClickListener {
                    if (alunos.isEmpty()) { aviso("Nenhum aluno vinculado."); return@setOnClickListener }
                    dialogo(MaterialAlertDialogBuilder(this@DashboardActivity).setTitle("Selecione o aluno")
                        .setItems(alunos.map { it.nome() }.toTypedArray()) { _, pos ->
                            selecionado = alunos[pos].id; carregarMetas()
                        })
                }
            }
            pai.addView(seletor, pai.indexOfChild(nome) + 1)
        }
    }
    override fun onResume() {
        super.onResume()
        if (isFinishing || Sessao.token(this).isBlank()) return
        val geracao = ++cargaPainel
        chamar(api.me()) { u ->
            if (geracao != cargaPainel) return@chamar
            if (u == null) { aviso("Resposta inválida do servidor."); return@chamar }
            Sessao.atualizar(this, u)
            findViewById<TextView>(R.id.tvNomeUsuario).text = getString(R.string.ola_usuario, u.nome())
            chamar(api.pacientes(), aoErro = {
                if (geracao == cargaPainel) {
                    alunos = emptyList(); selecionado = 0; carregarMetas(); aviso(it.mensagem)
                }
            }) { lista ->
                if (geracao == cargaPainel) {
                    alunos = lista.orEmpty()
                    if (alunos.none { it.id == selecionado }) selecionado = alunos.firstOrNull()?.id ?: 0
                    carregarMetas()
                }
            }
        }
    }
    private fun carregarMetas() {
        val id = selecionado
        val geracao = ++cargaMetas
        val resumo = findViewById<TextView>(R.id.tvProgresso)
        seletor?.text = alunos.find { it.id == id }?.nome() ?: "Selecionar aluno"
        findViewById<Button>(R.id.btnVerMais).isEnabled = id > 0
        findViewById<Button>(R.id.btnVerApontamentos).isEnabled = id > 0
        if (id == 0L) { resumo.text = "Nenhum aluno vinculado. Consulte os convites em Equipe."; return }
        resumo.text = "Carregando metas..."
        chamar(api.metas(id), aoErro = { if (geracao == cargaMetas) resumo.text = it.mensagem }) { lista ->
            if (selecionado != id || geracao != cargaMetas) return@chamar
            val metas = lista.orEmpty(); val feitas = metas.count { it.concluido }
            resumo.text = if (metas.isEmpty()) "Nenhuma meta cadastrada."
                else "$feitas de ${metas.size} metas concluídas (${feitas * 100 / metas.size}%)."
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong("selecionado", selecionado); super.onSaveInstanceState(outState)
    }
}
