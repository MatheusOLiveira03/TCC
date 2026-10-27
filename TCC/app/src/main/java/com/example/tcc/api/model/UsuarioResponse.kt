package com.example.tcc.api.model

import com.google.gson.annotations.SerializedName

data class Perfil(
    val nome: String? = null, val cpf: String? = null, val telefone: String? = null,
    val dataNascimento: String? = null, val ocupacao: String? = null,
    val instituicao: String? = null, val conteudoHtml: String? = null,
    val horarioDeEntrada: String? = null, val horarioDeSaida: String? = null,
    val urlFotoPerfil: String? = null,
    @SerializedName(value = "condicoes", alternate = ["condicao"]) val condicoes: String? = null
)
data class UsuarioResponse(
    val id: Long = 0, val email: String? = null, val tipoUsuario: String? = null,
    val perfil: Perfil? = null, val criadoPorId: Long? = null
) {
    fun nome() = perfil?.nome?.takeIf { it.isNotBlank() } ?: email.orEmpty()
}
data class SessaoResponse(val token: String? = null, val usuario: UsuarioResponse? = null)
data class Pessoa(val id: Long = 0, val email: String? = null, val tipoUsuario: String? = null,
    val nome: String? = null, val perfil: Perfil? = null, val ocupacao: String? = null,
    val instituicao: String? = null) {
    fun nomeExibicao() = perfil?.nome ?: nome ?: email ?: "Usuário"
}
data class Relacao(val id: Long = 0, val usuarioSolicitante: Pessoa? = null,
    val usuarioDestinatario: Pessoa? = null, val solicitacao: String? = null,
    val tipoRelacao: String? = null, val convidadoPorId: Long? = null)
data class Meta(val id: Long = 0, val idPaciente: Long = 0, val titulo: String? = null,
    val area: String? = null, val conteudoHtml: String? = null, val concluido: Boolean = false,
    val anteriores: List<Long>? = null, val versao: Long? = null,
    val responsavel: Pessoa? = null, val atualizadoPor: Pessoa? = null,
    val totalPostagens: Long = 0)
data class MetaEntrada(val titulo: String? = null, val area: String? = null,
    val conteudoHtml: String? = null, val anteriores: List<Long>? = null,
    val concluido: Boolean? = null, val versao: Long? = null)
data class ConexoesEntrada(val anteriores: List<Long>, val seguintes: List<Long>, val versoes: Map<Long, Long>)
data class Anexo(val id: Long = 0, val nome: String? = null, val tipoArquivo: String? = null)
data class Postagem(val id: Long = 0, val idMeta: Long = 0, val titulo: String? = null,
    val conteudoHtml: String? = null, val tipoPostagem: String? = null,
    val visualizacao: String? = null, val autor: Pessoa? = null,
    val dataCriacao: String? = null, val respondeA: Long? = null,
    val anexos: List<Anexo>? = null)
data class PostagemEntrada(val titulo: String, val conteudoHtml: String,
    val tipoPostagem: String = "COMUNICADO", val visualizacao: String = "TODOS",
    val respondeA: Long? = null)
data class AtivacaoConsulta(val token: String)
data class AtivacaoResumo(val nome: String? = null, val email: String? = null)
data class AtivacaoEntrada(val token: String, val senha: String)
data class ConviteEquipe(val idProfissional: Long)
data class ContaEntrada(val email: String)
