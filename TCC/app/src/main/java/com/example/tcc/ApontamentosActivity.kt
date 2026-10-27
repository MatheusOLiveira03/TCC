package com.example.tcc

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.example.tcc.api.model.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

class ApontamentosActivity : ApiActivity() {
    private lateinit var lista: LinearLayout
    private lateinit var seletor: Button
    private val linhas = mutableListOf<View>()
    private var metas = emptyList<Meta>()
    private var metaId = 0L
    private var carga = 0
    private var cargaMetas = 0
    private var aoEscolherArquivo: ((Uri) -> Unit)? = null
    private val executor = Executors.newSingleThreadExecutor()
    private var anexoParaExportar: Anexo? = null
    private var download: Call<ResponseBody>? = null
    @Volatile private var corpoDownload: ResponseBody? = null

    private val escolherArquivo = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && uri.scheme == "content") aoEscolherArquivo?.invoke(uri)
    }
    private val exportar = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val anexo = anexoParaExportar
        anexoParaExportar = null
        if (uri != null && anexo != null) baixar(anexo, uri)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!sessaoValida()) return
        setContentView(R.layout.apontamentos)
        metaId = savedInstanceState?.getLong("meta") ?: intent.getLongExtra("meta", 0)
        lista = removerModelos(R.id.cardItem1, R.id.cardItem2)
        findViewById<Button>(R.id.btnVoltar).setOnClickListener { finish() }
        seletor = modelo<Button>(R.layout.progresso, R.id.btnSelecionarArea).apply {
            text = "Selecionar meta"
            setOnClickListener {
                dialogo(MaterialAlertDialogBuilder(this@ApontamentosActivity).setTitle("Selecione a meta")
                    .setItems(metas.map { it.titulo.orEmpty() }.toTypedArray()) { _, i ->
                        metaId = metas[i].id; carregar()
                    })
            }
        }
        lista.addView(seletor)
        lista.addView(modelo<Button>(R.layout.progresso, R.id.btnSelecionarArea).apply {
            text = "Atualizar apontamentos"; setOnClickListener { carregarMetas() }
        })
        carregarMetas()
    }
    private fun carregarMetas() {
        if (pacienteId() <= 0) { aviso("Selecione primeiro o aluno no painel."); finish(); return }
        val geracao = ++cargaMetas
        chamar(api.metas(pacienteId()), aoErro = {
            if (geracao != cargaMetas) return@chamar
            if (it.status == 403) { metas = emptyList(); metaId = 0; carregar() }
            aviso(it.mensagem)
        }) { dados ->
            if (geracao != cargaMetas) return@chamar
            metas = dados.orEmpty()
            if (metas.none { it.id == metaId }) metaId = metas.firstOrNull()?.id ?: 0
            carregar()
        }
    }
    private fun limparLinhas() { linhas.forEach { lista.removeView(it) }; linhas.clear() }
    private fun carregar() {
        val geracao = ++carga
        limparLinhas()
        seletor.text = metas.find { it.id == metaId }?.titulo ?: "Nenhuma meta cadastrada"
        if (metaId == 0L) return
        val nova = modelo<View>(R.layout.apontamentos, R.id.cardItem2)
        nova.findViewById<TextView>(R.id.tvTextoCard2).text = "Novo apontamento para esta meta"
        nova.findViewById<Button>(R.id.btnAdicionar2).setOnClickListener { editar(null) }
        lista.addView(nova); linhas.add(nova)
        val alvo = metaId
        chamar(api.postagens(alvo), aoErro = {
            if (geracao != carga) return@chamar
            if (it.status == 403) limparLinhas()
            aviso(it.mensagem)
        }) { registros ->
            if (metaId != alvo || geracao != carga) return@chamar
            registros.orEmpty().forEach { p ->
                val row = modelo<View>(R.layout.apontamentos, R.id.cardItem1)
                val resposta = p.respondeA?.let { "Resposta ao apontamento #$it\n" }.orEmpty()
                row.findViewById<TextView>(R.id.tvTextoCard1).text =
                    "$resposta${p.titulo.orEmpty()}\n${textoHtml(p.conteudoHtml)}\n" +
                    "${p.autor?.nomeExibicao().orEmpty()} · ${p.visualizacao.orEmpty()}\n" +
                    "${p.anexos.orEmpty().size} anexo(s)"
                row.findViewById<Button>(R.id.btnAdicionar1).apply {
                    contentDescription = "Ver apontamento e ações"
                    setOnClickListener { acoes(p) }
                }
                lista.addView(row); linhas.add(row)
            }
        }
    }
    private fun acoes(p: Postagem) {
        val opcoes = mutableListOf("Responder")
        if (!p.anexos.isNullOrEmpty()) opcoes.add("Baixar anexo")
        if (p.autor?.id == Sessao.usuario(this)?.id) opcoes.addAll(listOf("Editar", "Excluir"))
        dialogo(MaterialAlertDialogBuilder(this).setTitle(p.titulo)
            .setItems(opcoes.toTypedArray()) { _, i ->
                when (opcoes[i]) {
                    "Responder" -> editar(null, p)
                    "Editar" -> editar(p)
                    "Excluir" -> dialogo(MaterialAlertDialogBuilder(this).setTitle("Excluir apontamento?")
                        .setNegativeButton(R.string.cancelar, null).setPositiveButton("Excluir") { _, _ ->
                            chamar(api.removerPostagem(p.id)) { carregar() }
                        })
                    "Baixar anexo" -> {
                        val anexos = p.anexos.orEmpty()
                        dialogo(MaterialAlertDialogBuilder(this).setTitle("Anexos")
                            .setItems(anexos.map { it.nome ?: "arquivo" }.toTypedArray()) { _, pos ->
                                anexoParaExportar = anexos[pos]
                                exportar.launch(anexos[pos].nome ?: "arquivo")
                            })
                    }
                }
            })
    }
    private fun editar(postagem: Postagem?, resposta: Postagem? = null) {
        val alvo = metaId
        val f = formulario()
        val titulo = campo(f, "Título", postagem?.titulo ?: resposta?.titulo?.let { "Resposta: $it" }.orEmpty())
        val textoOriginal = textoHtml(postagem?.conteudoHtml).toString()
        val conteudo = campo(f, "Apontamento", textoOriginal, true)
        val tipos = arrayOf("REGISTRO_SESSAO", "ORIENTACAO_FAMILIA", "ORIENTACAO_ESCOLA", "EVOLUCAO",
            "ATIVIDADE", "RELATORIO", "COMUNICADO", "DESCRICAO")
        var tipo = postagem?.tipoPostagem ?: "COMUNICADO"
        f.addView(modelo<Button>(R.layout.progresso, R.id.btnSelecionarArea).apply {
            text = tipo
            setOnClickListener {
                dialogo(MaterialAlertDialogBuilder(this@ApontamentosActivity).setTitle("Tipo de apontamento")
                    .setItems(tipos) { _, i -> tipo = tipos[i]; text = tipo })
            }
        })
        val privado = CheckBox(this).apply {
            text = "Visível apenas aos profissionais"
            isChecked = (postagem?.visualizacao ?: resposta?.visualizacao) == "PROFISSIONAIS"
            isEnabled = profissional() && postagem == null && resposta == null
            visibility = if (profissional()) View.VISIBLE else View.GONE
        }
        f.addView(privado)
        var arquivo: Uri? = null
        var janela: AlertDialog? = null
        if (postagem == null) {
            f.addView(modelo<Button>(R.layout.progresso, R.id.btnSelecionarArea).apply {
                text = "Anexar arquivo (até 10 MB)"
                setOnClickListener {
                    aoEscolherArquivo = { uri ->
                        if (janela?.isShowing == true) { arquivo = uri; text = "Arquivo selecionado · segure para remover" }
                    }
                    escolherArquivo.launch(arrayOf("*/*"))
                }
                setOnLongClickListener { arquivo = null; text = "Anexar arquivo (até 10 MB)"; true }
            })
        }
        val d = dialogo(MaterialAlertDialogBuilder(this).setTitle(if (postagem == null) "Novo apontamento" else "Editar apontamento")
            .setView(f).setNegativeButton(R.string.cancelar, null).setPositiveButton(R.string.salvar, null))
        janela = d
        val salvar = d.getButton(-1)
        salvar.setOnClickListener {
            if (titulo.text.isBlank()) { titulo.error = "Informe o título."; return@setOnClickListener }
            if (conteudo.text.isBlank()) { conteudo.error = "Escreva o apontamento."; return@setOnClickListener }
            val html = if (postagem != null && conteudo.text.toString() == textoOriginal) postagem.conteudoHtml.orEmpty()
                else comoHtml(conteudo.text.toString())
            val dto = PostagemEntrada(titulo.text.toString().trim(), html, tipo,
                if (privado.isChecked) "PROFISSIONAIS" else "TODOS", postagem?.respondeA ?: resposta?.id)
            fun enviar(call: Call<Postagem>) {
                chamar(call, botao = salvar) { d.dismiss(); aoEscolherArquivo = null; carregar() }
            }
            val uri = arquivo
            if (postagem != null) enviar(api.editarPostagem(postagem.id, dto))
            else if (uri == null) enviar(api.publicar(alvo, dto))
            else {
                salvar.isEnabled = false
                val token = Sessao.token(this)
                executor.execute {
                    runCatching { parte(uri) }.onSuccess { parte ->
                        runOnUiThread {
                            if (!isDestroyed && d.isShowing && Sessao.token(this) == token) {
                                salvar.isEnabled = true
                                enviar(api.publicarArquivo(alvo,
                                    Gson().toJson(dto).toRequestBody("application/json".toMediaType()), parte))
                            }
                        }
                    }.onFailure {
                        runOnUiThread {
                            if (!isDestroyed && d.isShowing) {
                                salvar.isEnabled = true; aviso("Não foi possível ler o arquivo. Limite: 10 MB.")
                            }
                        }
                    }
                }
            }
        }
    }
    private fun parte(uri: Uri): MultipartBody.Part {
        var nome = "anexo"
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) nome = cursor.getString(0) ?: nome
        }
        nome = nome.replace('/', '_').replace('\\', '_').replace('\r', '_').replace('\n', '_').take(150)
        val out = ByteArrayOutputStream()
        requireNotNull(contentResolver.openInputStream(uri)).use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                check(!Thread.currentThread().isInterrupted)
                val n = input.read(buffer); if (n < 0) break
                require(out.size() + n <= 10 * 1024 * 1024); out.write(buffer, 0, n)
            }
        }
        val tipo = contentResolver.getType(uri)?.toMediaTypeOrNull()
        return MultipartBody.Part.createFormData("arquivos", nome, out.toByteArray().toRequestBody(tipo))
    }
    private fun baixar(anexo: Anexo, destino: Uri) {
        download?.cancel(); corpoDownload?.close()
        val call = api.baixar(anexo.id); download = call
        chamar(call) { corpo ->
            if (corpo == null) return@chamar
            corpoDownload = corpo
            executor.execute {
                val ok = runCatching {
                    corpo.use { body ->
                        requireNotNull(contentResolver.openOutputStream(destino, "w")).use { out ->
                            body.byteStream().use { input ->
                                val buffer = ByteArray(8192)
                                while (true) {
                                    check(!Thread.currentThread().isInterrupted)
                                    val n = input.read(buffer); if (n < 0) break
                                    out.write(buffer, 0, n)
                                }
                            }
                        }
                    }
                }.isSuccess
                runOnUiThread {
                    if (!isDestroyed) aviso(if (ok) "Arquivo salvo." else "Não foi possível concluir o download.")
                }
            }
        }
    }
    override fun onSaveInstanceState(outState: Bundle) { outState.putLong("meta", metaId); super.onSaveInstanceState(outState) }
    override fun onDestroy() {
        aoEscolherArquivo = null; download?.cancel(); corpoDownload?.close(); executor.shutdownNow()
        super.onDestroy()
    }
}
