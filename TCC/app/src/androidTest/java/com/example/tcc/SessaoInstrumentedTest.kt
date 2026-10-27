package com.example.tcc

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.tcc.api.model.SessaoResponse
import com.example.tcc.api.model.UsuarioResponse
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SessaoInstrumentedTest {
    private val contexto get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun usuario() = UsuarioResponse(7, "teste@exemplo.invalid", "PACIENTE")
    @After fun limpar() { Sessao.limpar(contexto) }

    @Test fun flagDoPrototipoNaoAutentica() {
        Sessao.limpar(contexto)
        contexto.getSharedPreferences("tcc_preferences", 0).edit().putBoolean("logged_in", true).commit()
        assertFalse(AppPreferences.isLoggedIn(contexto))
    }
    @Test fun tokenEPerfilFicamCifradosForaDoBackup() {
        Sessao.salvar(contexto, SessaoResponse("header.payload.signature", usuario()))
        val conteudo = File(contexto.noBackupFilesDir, "oficina_sessao.bin").readText()
        assertFalse(conteudo.contains("header.payload.signature"))
        assertFalse(conteudo.contains("teste@exemplo.invalid"))
        assertEquals("header.payload.signature", Sessao.token(contexto))
        assertEquals(7L, Sessao.usuario(contexto)?.id)
    }
    @Test fun respostaAntigaNaoApagaOutraSessao() {
        Sessao.salvar(contexto, SessaoResponse("antigo.payload.signature", usuario()))
        Sessao.salvar(contexto, SessaoResponse("novo.payload.signature", usuario()))
        Sessao.limparSeToken(contexto, "antigo.payload.signature")
        assertEquals("novo.payload.signature", Sessao.token(contexto))
    }
    @Test fun logoutRemoveTokenEDados() {
        Sessao.salvar(contexto, SessaoResponse("header.payload.signature", usuario()))
        Sessao.limpar(contexto)
        assertEquals("", Sessao.token(contexto)); assertNull(Sessao.usuario(contexto))
    }
    @Test fun arquivoAlteradoNaoRestauraSessao() {
        Sessao.salvar(contexto, SessaoResponse("header.payload.signature", usuario()))
        File(contexto.noBackupFilesDir, "oficina_sessao.bin").writeText("arquivo-alterado")
        assertEquals("", Sessao.token(contexto)); assertNull(Sessao.usuario(contexto))
    }
}
