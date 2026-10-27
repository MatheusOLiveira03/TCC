package com.example.tcc

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.example.tcc.api.model.ConexoesEntrada
import com.example.tcc.api.model.Meta
import com.example.tcc.api.model.MetaEntrada
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ProgressoActivity : ApiActivity() {
    private lateinit var lista: LinearLayout
    private val linhas = mutableListOf<View>()
    private var metas = emptyList<Meta>()
    private var area: String? = null
    private var carga = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!sessaoValida()) return
        setContentView(R.layout.progresso)
        lista = removerModelos(R.id.cardArea)
        findViewById<Button>(R.id.btnVoltar).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnSelecionarArea).setOnClickListener {
            val areas = listOf("Todas as áreas") + metas.map { it.area.orEmpty() }.distinct().sorted()
            dialogo(MaterialAlertDialogBuilder(this).setTitle(R.string.selecionar_area)
                .setItems(areas.toTypedArray()) { _, pos -> area = if (pos == 0) null else areas[pos]; renderizar() })
        }
        if (profissional()) lista.addView(modelo<Button>(R.layout.progresso, R.id.btnSelecionarArea).apply {
            text = "Nova meta"; setOnClickListener { editar(null) }
        })
        lista.addView(modelo<Button>(R.layout.progresso, R.id.btnSelecionarArea).apply {
            text = "Atualizar metas"; setOnClickListener { carregar() }
        })
        carregar()
    }
    private fun carregar() {
        if (pacienteId() <= 0) { aviso("Selecione primeiro o aluno no painel."); finish(); return }
        val geracao = ++carga
        chamar(api.metas(pacienteId()), aoErro = {
            if (geracao != carga) return@chamar
            if (it.status == 403) { metas = emptyList(); renderizar() }
            aviso(it.mensagem)
        }) { if (geracao == carga) { metas = it.orEmpty(); renderizar() } }
    }
    private fun renderizar() {
        linhas.forEach { lista.removeView(it) }; linhas.clear()
        val visiveis = metas.filter { area == null || it.area == area }
        findViewById<TextView>(R.id.tvRotuloArea).text =
            "${area ?: "Todas as áreas"} · ${visiveis.count { it.concluido }}/${visiveis.size} concluídas"
        if (visiveis.isEmpty()) {
            val row = modelo<View>(R.layout.progresso, R.id.cardArea)
            row.findViewById<TextView>(R.id.tvTituloDescricao).text = "Nenhuma meta nesta área."
            row.findViewById<Button>(R.id.btnVerMais).visibility = View.GONE
            lista.addView(row); linhas.add(row)
        }
        visiveis.forEach { m ->
            val row = modelo<View>(R.layout.progresso, R.id.cardArea)
            val anteriores = m.anteriores.orEmpty().map { id -> metas.find { it.id == id }?.titulo ?: "#$id" }
            row.findViewById<TextView>(R.id.tvTituloDescricao).text =
                "${m.titulo.orEmpty()}\n${m.area.orEmpty()} · ${if (m.concluido) "Concluída" else "Em andamento"}\n" +
                "${textoHtml(m.conteudoHtml)}\nResponsável: ${m.responsavel?.nomeExibicao().orEmpty()}\n" +
                "Anteriores: ${anteriores.joinToString().ifBlank { "nenhuma" }}"
            row.findViewById<Button>(R.id.btnVerMais).setOnClickListener { acoes(m) }
            lista.addView(row); linhas.add(row)
        }
    }
    private fun acoes(m: Meta) {
        val opcoes = mutableListOf("Ver apontamentos")
        if (profissional()) opcoes.addAll(listOf("Editar meta",
            if (m.concluido) "Reabrir meta" else "Concluir meta", "Excluir meta", "Relacionar metas"))
        dialogo(MaterialAlertDialogBuilder(this).setTitle(m.titulo).setItems(opcoes.toTypedArray()) { _, pos ->
            when (pos) {
                0 -> abrir(ApontamentosActivity::class.java, pacienteId(), m.id)
                1 -> editar(m)
                2 -> {
                    if (m.versao == null) { aviso("Atualize as metas antes de editar."); return@setItems }
                    chamar(api.editarMeta(m.id, MetaEntrada(concluido = !m.concluido, versao = m.versao)),
                        aoErro = { aviso(it.mensagem); if (it.status == 409) carregar() }) { carregar() }
                }
                3 -> dialogo(MaterialAlertDialogBuilder(this).setTitle("Excluir esta meta?")
                    .setMessage("O servidor verificará se existem registros ou dependências.")
                    .setNegativeButton(R.string.cancelar, null)
                    .setPositiveButton("Excluir") { _, _ -> chamar(api.removerMeta(m.id)) { carregar() } })
                4 -> relacionar(m)
            }
        })
    }
    private fun selecaoMetas(rotulo: String, candidatas: List<Meta>, ids: MutableSet<Long>): Button =
        modelo<Button>(R.layout.progresso, R.id.btnSelecionarArea).apply {
            fun atualizar() { text = "$rotulo (${ids.size})" }
            atualizar()
            setOnClickListener {
                if (candidatas.isEmpty()) { aviso("Cadastre outra meta para criar uma relação."); return@setOnClickListener }
                val rascunho = ids.toMutableSet()
                dialogo(MaterialAlertDialogBuilder(this@ProgressoActivity).setTitle(rotulo)
                    .setMultiChoiceItems(candidatas.map { it.titulo.orEmpty() }.toTypedArray(),
                        candidatas.map { it.id in rascunho }.toBooleanArray()) { _, i, marcada ->
                        if (marcada) rascunho.add(candidatas[i].id) else rascunho.remove(candidatas[i].id)
                    }.setNegativeButton(R.string.cancelar, null)
                    .setPositiveButton(R.string.ok) { _, _ -> ids.clear(); ids.addAll(rascunho); atualizar() })
            }
        }
    private fun editar(m: Meta?) {
        if (!profissional()) return
        if (m != null && m.versao == null) { aviso("Atualize as metas antes de editar."); return }
        val f = formulario()
        val titulo = campo(f, "Título", m?.titulo.orEmpty())
        val areaCampo = campo(f, "Área", m?.area.orEmpty())
        val textoOriginal = textoHtml(m?.conteudoHtml).toString()
        val descricao = campo(f, "Descrição", textoOriginal, true)
        val anteriores = m?.anteriores.orEmpty().toMutableSet()
        f.addView(selecaoMetas("Metas anteriores", metas.filter { it.id != m?.id }, anteriores))
        val d = dialogo(MaterialAlertDialogBuilder(this).setTitle(if (m == null) "Nova meta" else "Editar meta")
            .setView(f).setNegativeButton(R.string.cancelar, null).setPositiveButton(R.string.salvar, null))
        val salvar = d.getButton(-1)
        salvar.setOnClickListener {
            if (titulo.text.isBlank()) { titulo.error = "Informe o título."; return@setOnClickListener }
            if (areaCampo.text.isBlank()) { areaCampo.error = "Informe a área."; return@setOnClickListener }
            val html = if (m != null && descricao.text.toString() == textoOriginal) m.conteudoHtml.orEmpty()
                else comoHtml(descricao.text.toString())
            val dto = MetaEntrada(titulo.text.toString().trim(), areaCampo.text.toString().trim(),
                html, anteriores.toList(), versao = m?.versao)
            val call = if (m == null) api.criarMeta(pacienteId(), dto) else api.editarMeta(m.id, dto)
            chamar(call, botao = salvar, aoErro = {
                aviso(it.mensagem)
                if (it.status == 409) {
                    salvar.isEnabled = false; carregar()
                    aviso("O rascunho foi preservado. Reabra a meta atualizada antes de salvar.")
                }
            }) { d.dismiss(); carregar() }
        }
    }
    private fun relacionar(meta: Meta) {
        if (!profissional()) return
        val fotografia = metas.toList()
        if (fotografia.none { it.id == meta.id } || fotografia.any { it.versao == null }) {
            aviso("Atualize as metas antes de alterar as conexões."); carregar(); return
        }
        val candidatas = fotografia.filter { it.id != meta.id }
        val anteriores = meta.anteriores.orEmpty().toMutableSet()
        val seguintes = fotografia.filter { meta.id in it.anteriores.orEmpty() }.map { it.id }.toMutableSet()
        val f = formulario()
        f.addView(selecaoMetas("Metas anteriores", candidatas, anteriores))
        f.addView(selecaoMetas("Metas seguintes", candidatas, seguintes))
        val d = dialogo(MaterialAlertDialogBuilder(this).setTitle("Relações: ${meta.titulo.orEmpty()}")
            .setView(f).setNegativeButton(R.string.cancelar, null).setPositiveButton(R.string.salvar, null))
        val salvar = d.getButton(-1)
        salvar.setOnClickListener {
            val dto = ConexoesEntrada(anteriores.toList(), seguintes.toList(),
                fotografia.associate { it.id to requireNotNull(it.versao) })
            chamar(api.conectarMetas(meta.id, dto), botao = salvar, aoErro = {
                aviso(it.mensagem)
                if (it.status == 409) {
                    salvar.isEnabled = false; carregar()
                    aviso("Feche esta janela e abra as relações atualizadas novamente.")
                }
            }) { recebidas ->
                if (recebidas == null) {
                    salvar.isEnabled = false; carregar()
                    aviso("Atualize as metas para confirmar o salvamento."); return@chamar
                }
                carga++; metas = recebidas; d.dismiss(); renderizar(); aviso("Relações salvas no servidor.")
            }
        }
    }
}
