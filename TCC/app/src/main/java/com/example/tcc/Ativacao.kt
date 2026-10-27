package com.example.tcc

import java.net.URI
import java.net.URLDecoder

object Ativacao {
    fun token(entrada: String): String {
        val texto = entrada.trim()
        val candidato = if (texto.contains("://")) {
            val uri = URI(texto)
            val parametros = (uri.rawFragment ?: uri.rawQuery).orEmpty()
            val valor = parametros.split('&').map { it.split('=', limit = 2) }
                .firstOrNull { it.size == 2 && it[0] == "token" }?.get(1).orEmpty()
            URLDecoder.decode(valor, "UTF-8")
        } else texto
        require(candidato.matches(Regex("[A-Za-z0-9_-]{32,100}"))) {
            "Cole o link ou código de ativação entregue pelo profissional."
        }
        return candidato
    }
}
