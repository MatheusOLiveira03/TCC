package com.example.tcc

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import com.example.tcc.api.model.ContaEntrada
import com.example.tcc.api.model.Perfil
import com.example.tcc.api.model.UsuarioResponse
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class UsuarioActivity : ApiActivity() {
    private var usuario: UsuarioResponse? = null
    private var editando = false
    private lateinit var nome: EditText
    private lateinit var email: EditText
    private lateinit var cpf: EditText
    private lateinit var nascimento: EditText
    private lateinit var telefone: EditText
    private lateinit var salvar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!sessaoValida()) return
        setContentView(R.layout.usuario)
        nome = findViewById(R.id.etUsuarioNome); email = findViewById(R.id.etUsuarioEmail)
        cpf = findViewById(R.id.etUsuarioCpf); nascimento = findViewById(R.id.etUsuarioDataNascimento)
        telefone = findViewById(R.id.etUsuarioCelular); salvar = findViewById(R.id.btnEditar)
        habilitar(false); salvar.isEnabled = false
        chamar(api.me()) { u ->
            if (u == null) { aviso("Resposta inválida do servidor."); return@chamar }
            usuario = u; Sessao.atualizar(this, u); preencher(u); salvar.isEnabled = true
        }
        salvar.setOnClickListener { if (!editando) habilitar(true) else gravar() }
        findViewById<Button>(R.id.btnVoltar).setOnClickListener { finish() }
    }
    private fun preencher(u: UsuarioResponse) {
        nome.setText(u.perfil?.nome); email.setText(u.email); cpf.setText(u.perfil?.cpf)
        nascimento.setText(u.perfil?.dataNascimento); telefone.setText(u.perfil?.telefone)
    }
    private fun camposHabilitados(valor: Boolean) {
        listOf(nome, email, cpf, nascimento, telefone).forEach { it.isEnabled = valor }
    }
    private fun habilitar(valor: Boolean) {
        editando = valor; camposHabilitados(valor)
        salvar.text = getString(if (valor) R.string.salvar else R.string.editar)
    }
    private fun dataISO(texto: String): String? {
        if (texto.isBlank()) return null
        val formato = if (texto.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) "yyyy-MM-dd" else "dd/MM/yyyy"
        val entrada = SimpleDateFormat(formato, Locale.ROOT).apply {
            isLenient = false; timeZone = TimeZone.getTimeZone("UTC")
        }
        val data = requireNotNull(entrada.parse(texto))
        require(entrada.format(data) == texto)
        return SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(data)
    }
    private fun gravar() {
        val u = usuario ?: return
        val n = nome.text.toString().trim(); val e = email.text.toString().trim().lowercase(Locale.ROOT)
        val c = cpf.text.toString().filter(Char::isDigit)
        if (n.length < 2) { nome.error = getString(R.string.erro_nome_invalido); return }
        if (!Validators.isEmailValid(e)) { email.error = getString(R.string.erro_email_invalido); return }
        if (c.isNotBlank() && !Validators.isCpfValid(c)) { cpf.error = getString(R.string.erro_cpf_invalido); return }
        val data = runCatching { dataISO(nascimento.text.toString().trim()) }
        if (data.isFailure) { nascimento.error = "Use dd/MM/aaaa ou aaaa-MM-dd."; return }
        val perfil = (u.perfil ?: Perfil()).copy(nome = n, cpf = c,
            telefone = telefone.text.toString().trim(), dataNascimento = data.getOrNull())
        camposHabilitados(false)
        chamar(api.perfil(u.id, perfil), botao = salvar, aoErro = {
            camposHabilitados(true); aviso(it.mensagem)
        }) { recebido ->
            if (recebido == null) {
                camposHabilitados(true); aviso("Atualize o perfil para confirmar o salvamento."); return@chamar
            }
            val atualizado = u.copy(perfil = recebido)
            usuario = atualizado; Sessao.atualizar(this, atualizado); preencher(atualizado)
            if (e == u.email) { habilitar(false); aviso("Perfil salvo no servidor."); return@chamar }
            chamar(api.email(u.id, ContaEntrada(e)), botao = salvar,
                aoErro = {
                    habilitar(true); email.setText(e)
                    aviso("O perfil foi salvo, mas o e-mail não foi alterado: ${it.mensagem}")
                }) {
                Sessao.limpar(this); aviso("E-mail atualizado. Entre novamente com o novo endereço."); entrarNovamente()
            }
        }
    }
}
