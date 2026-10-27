package com.example.tcc.api

import com.google.gson.JsonParser
import retrofit2.Response

data class ErroApi(val status: Int, val mensagem: String, val codigo: String? = null, val espera: Long = 0) {
    companion object {
        fun de(r: Response<*>): ErroApi {
            val json = runCatching { r.errorBody()?.use { JsonParser.parseString(it.string()).asJsonObject } }.getOrNull()
            fun texto(chave: String) = runCatching { json?.get(chave)?.asString }.getOrNull()
            val padrao = when (r.code()) {
                401 -> "Sessão inválida ou credenciais incorretas."
                403 -> "Você não tem permissão para acessar estes dados."
                409 -> "Os dados mudaram. Atualize antes de salvar novamente."
                429 -> "Muitas tentativas. Aguarde para tentar novamente."
                else -> "Não foi possível concluir a operação (${r.code()})."
            }
            return ErroApi(r.code(), texto("message") ?: texto("detail") ?: padrao,
                texto("codigo"), r.headers()["Retry-After"]?.toLongOrNull()?.coerceIn(1, 86400) ?: 0)
        }
    }
}
