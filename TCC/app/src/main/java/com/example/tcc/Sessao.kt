package com.example.tcc

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import android.util.Base64
import com.example.tcc.api.model.SessaoResponse
import com.example.tcc.api.model.UsuarioResponse
import com.google.gson.Gson
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object Sessao {
    private data class Dados(val token: String, val usuario: UsuarioResponse, val servidor: String)
    private val gson = Gson()
    private fun arquivo(c: Context) = AtomicFile(File(c.noBackupFilesDir, "oficina_sessao.bin"))
    private fun chave(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return (store.getKey("OficinaSessaoV1", null) as? SecretKey) ?: KeyGenerator
            .getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
                init(KeyGenParameterSpec.Builder("OficinaSessaoV1", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
            }.generateKey()
    }
    private fun ler(c: Context): Dados? = runCatching {
        val bytes = Base64.decode(arquivo(c).readFully(), Base64.NO_WRAP)
        require(bytes.size > 28)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, chave(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        }
        gson.fromJson(String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8), Dados::class.java)
            .takeIf { it.servidor == BuildConfig.API_BASE_URL && it.usuario.id > 0 && it.token.isNotBlank() }
    }.getOrNull()
    @Synchronized fun token(c: Context): String = ler(c)?.token.orEmpty()
    @Synchronized fun usuario(c: Context): UsuarioResponse? = ler(c)?.usuario
    @Synchronized fun salvar(c: Context, resposta: SessaoResponse) {
        val token = resposta.token.orEmpty()
        val u = requireNotNull(resposta.usuario) { "Resposta sem usuário." }
        require(token.split('.').let { it.size == 3 && it.all(String::isNotBlank) } && u.id > 0 &&
            u.tipoUsuario in setOf("PACIENTE", "PROFISSIONAL")) { "Resposta de autenticação inválida." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, chave()) }
        val texto = gson.toJson(Dados(token, u, BuildConfig.API_BASE_URL)).toByteArray(Charsets.UTF_8)
        val bytes = Base64.encode(cipher.iv + cipher.doFinal(texto), Base64.NO_WRAP)
        val f = arquivo(c); val out = f.startWrite()
        try { out.write(bytes); f.finishWrite(out) } catch (e: Exception) { f.failWrite(out); throw e }
        AppPreferences.limparDadosAntigos(c)
    }
    @Synchronized fun atualizar(c: Context, usuario: UsuarioResponse) {
        ler(c)?.let { salvar(c, SessaoResponse(it.token, usuario)) }
    }
    @Synchronized fun limpar(c: Context) { arquivo(c).delete(); AppPreferences.limparDadosAntigos(c) }
    @Synchronized fun limparSeToken(c: Context, esperado: String) {
        if (esperado.isNotBlank() && token(c) == esperado) limpar(c)
    }
}
