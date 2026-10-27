package com.example.tcc

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.example.tcc.api.model.ConviteEquipe
import com.example.tcc.api.model.Relacao
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class EquipeActivity : ApiActivity() {
    private lateinit var lista: LinearLayout
    private val linhas = mutableListOf<View>()
    private var equipe = emptyList<Relacao>()
    private var convites = emptyList<Relacao>()
    private var gestor = false
    private var carga = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!sessaoValida()) return
        setContentView(R.layout.acompanhar_equipe)
        lista = removerModelos(R.id.cardProfissional1, R.id.cardProfissional2)
        findViewById<Button>(R.id.btnVoltar).setOnClickListener { finish() }
        if (profissional() && pacienteId() > 0) {
            lista.addView(modelo<Button>(R.layout.progresso, R.id.btnSelecionarArea).apply {
                text = "Convidar profissional"; setOnClickListener { convidar() }
            })
        }
        lista.addView(modelo<Button>(R.layout.progresso, R.id.btnSelecionarArea).apply {
            text = "Atualizar equipe"; setOnClickListener { carregar() }
        })
        carregar()
    }
    private fun carregar() {
        val id = Sessao.usuario(this)?.id ?: return
        val geracao = ++carga
        chamar(api.vinculos(), aoErro = {
            if (geracao == carga) { convites = emptyList(); renderizar(); aviso(it.mensagem) }
        }) { r ->
            if (geracao != carga) return@chamar
            convites = r.orEmpty().filter { it.usuarioDestinatario?.id == id && it.solicitacao != "ACEITA" }
            renderizar()
        }
        if (pacienteId() > 0) {
            chamar(api.equipe(pacienteId()), aoErro = {
                if (geracao == carga) { equipe = emptyList(); renderizar(); aviso(it.mensagem) }
            }) {
                if (geracao == carga) { equipe = it.orEmpty(); renderizar() }
            }
            if (profissional()) chamar(api.pacientes()) { pacientes ->
                if (geracao == carga) gestor = pacientes.orEmpty().find { it.id == pacienteId() }?.criadoPorId == id
            }
        } else renderizar()
    }
    private fun renderizar() {
        linhas.forEach { lista.removeView(it) }; linhas.clear()
        val id = Sessao.usuario(this)?.id ?: return
        val relacoes = (equipe + convites).distinctBy { it.id }
        if (relacoes.isEmpty()) {
            val row = modelo<View>(R.layout.acompanhar_equipe, R.id.cardProfissional1)
            row.findViewById<TextView>(R.id.tvProfissional1).text = "Nenhum vínculo ou convite disponível."
            row.findViewById<Button>(R.id.btnVerMais1).visibility = View.GONE
            lista.addView(row); linhas.add(row)
        }
        relacoes.forEach { r ->
            val doPaciente = pacienteId() > 0 &&
                (r.usuarioSolicitante?.id == pacienteId() || r.usuarioDestinatario?.id == pacienteId())
            val outro = if (doPaciente) {
                if (r.usuarioSolicitante?.id == pacienteId()) r.usuarioDestinatario else r.usuarioSolicitante
            } else if (r.usuarioSolicitante?.id == id) r.usuarioDestinatario else r.usuarioSolicitante
            val row = modelo<View>(R.layout.acompanhar_equipe, R.id.cardProfissional1)
            val nome = row.findViewById<TextView>(R.id.tvProfissional1)
            nome.text = outro?.nomeExibicao() ?: "Usuário"
            ((nome.parent as LinearLayout).getChildAt(1) as? TextView)?.text =
                if (r.solicitacao == "ACEITA") "Vínculo aceito" else "Convite pendente"
            row.findViewById<Button>(R.id.btnVerMais1).setOnClickListener {
                val acoes = mutableListOf("Ver informações")
                if (r.solicitacao != "ACEITA" && r.usuarioDestinatario?.id == id) acoes.add("Aceitar convite")
                if (r.usuarioSolicitante?.id == id || r.usuarioDestinatario?.id == id || (doPaciente && gestor) ||
                    (r.solicitacao != "ACEITA" && r.convidadoPorId == id)) acoes.add("Remover vínculo")
                dialogo(MaterialAlertDialogBuilder(this).setTitle(outro?.nomeExibicao())
                    .setItems(acoes.toTypedArray()) { _, pos ->
                        when (acoes[pos]) {
                            "Aceitar convite" -> chamar(api.aceitar(r.id)) { carregar() }
                            "Remover vínculo" -> dialogo(MaterialAlertDialogBuilder(this)
                                .setTitle("Remover vínculo?").setMessage("O acesso da equipe será atualizado pelo servidor.")
                                .setNegativeButton(R.string.cancelar, null)
                                .setPositiveButton("Remover") { _, _ -> chamar(api.removerVinculo(r.id)) { carregar() } })
                            else -> dialogo(MaterialAlertDialogBuilder(this).setTitle(outro?.nomeExibicao())
                                .setMessage(listOfNotNull(outro?.email, outro?.perfil?.ocupacao ?: outro?.ocupacao,
                                    outro?.perfil?.instituicao ?: outro?.instituicao).joinToString("\n"))
                                .setPositiveButton(R.string.ok, null))
                        }
                    })
            }
            lista.addView(row); linhas.add(row)
        }
    }
    private fun convidar() {
        val f = formulario(); val busca = campo(f, "Nome do profissional")
        val d = dialogo(MaterialAlertDialogBuilder(this).setTitle("Buscar profissional").setView(f)
            .setNegativeButton(R.string.cancelar, null).setPositiveButton("Buscar", null))
        val confirmar = d.getButton(-1)
        confirmar.setOnClickListener {
            chamar(api.profissionais(busca.text.toString().trim()), botao = confirmar) { resultado ->
                val pessoas = resultado.orEmpty()
                if (pessoas.isEmpty()) { aviso("Nenhum profissional encontrado."); return@chamar }
                dialogo(MaterialAlertDialogBuilder(this).setTitle("Convidar para a equipe")
                    .setItems(pessoas.map { it.nomeExibicao() }.toTypedArray()) { _, pos ->
                        chamar(api.convidar(pacienteId(), ConviteEquipe(pessoas[pos].id)), botao = confirmar) {
                            d.dismiss(); carregar()
                        }
                    })
            }
        }
    }
}
