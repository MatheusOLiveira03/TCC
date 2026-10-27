package com.example.tcc.api

import android.content.Context
import com.example.tcc.BuildConfig
import com.example.tcc.Sessao
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object RetrofitClient {
    @Volatile private var instancia: ApiService? = null

    fun api(context: Context): ApiService = instancia ?: synchronized(this) {
        instancia ?: criar(BuildConfig.API_BASE_URL, { Sessao.token(context.applicationContext) })
            .also { instancia = it }
    }

    internal fun criar(endereco: String, token: () -> String, debug: Boolean = BuildConfig.DEBUG): ApiService {
        val base = endereco.trim().toHttpUrl()
        require(base.username.isBlank() && base.password.isBlank() && base.query == null && base.fragment == null)
        val partes = base.host.split('.')
        val numeros = partes.mapNotNull { it.toIntOrNull() }
        val privado = base.host == "localhost" || (partes.size == 4 && numeros.size == 4 && numeros.all { it in 0..255 } &&
            (numeros[0] == 10 || numeros[0] == 127 || (numeros[0] == 192 && numeros[1] == 168) ||
                (numeros[0] == 172 && numeros[1] in 16..31)))
        require(base.isHttps || (debug && privado)) { "Use HTTPS ou um servidor local no build debug." }
        val publicas = setOf("login", "auth/ativacao/consultar", "auth/ativar", "health")
        val cliente = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS).readTimeout(120, TimeUnit.SECONDS)
            .callTimeout(130, TimeUnit.SECONDS).retryOnConnectionFailure(false)
            .followRedirects(false).followSslRedirects(false)
            .addInterceptor { chain ->
                val original = chain.request()
                if (original.url.host != base.host || original.url.port != base.port || original.url.scheme != base.scheme)
                    throw IOException("Destino diferente da API configurada.")
                val rota = original.url.encodedPath.removePrefix(base.encodedPath).trimStart('/')
                val jwt = if (rota in publicas) "" else token()
                val request = original.newBuilder().removeHeader("Authorization").apply {
                    if (jwt.isNotBlank()) header("Authorization", "Bearer $jwt")
                }.build()
                chain.proceed(request)
            }.build()
        return Retrofit.Builder().baseUrl(base).client(cliente)
            .addConverterFactory(GsonConverterFactory.create()).build().create(ApiService::class.java)
    }
}
