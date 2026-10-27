package com.example.tcc.api

import com.example.tcc.Ativacao
import com.example.tcc.api.model.*
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

class ContratoApiTest {
    private lateinit var servidor: MockWebServer
    private lateinit var api: ApiService
    private val gson = Gson()
    @Before fun preparar() {
        servidor = MockWebServer(); servidor.start()
        api = RetrofitClient.criar(servidor.url("/").toString(), { "jwt-protegido" }, true)
    }
    @After fun encerrar() { servidor.shutdown() }
    private fun dados(texto: String): Map<*, *> = gson.fromJson(texto, Map::class.java)

    @Test fun loginUsaSenhaECodigoSemBearer() {
        servidor.enqueue(MockResponse().setBody("""{"token":"header.payload.signature","usuario":{"id":7,"tipoUsuario":"PACIENTE","perfil":{"nome":"Aluno"}}}"""))
        val r = api.login(LoginRequest("aluno@teste.invalid", "Senha123!", "123456")).execute()
        assertEquals(7L, r.body()!!.usuario!!.id)
        val req = servidor.takeRequest()
        assertEquals("/login", req.path); assertNull(req.getHeader("Authorization"))
        val dto = dados(req.body.readUtf8())
        assertEquals("Senha123!", dto["senha"]); assertEquals("123456", dto["codigo"])
        assertFalse(dto.containsKey("password"))
    }
    @Test fun usuarioMeRecebeBearer() {
        servidor.enqueue(MockResponse().setBody("""{"id":7,"tipoUsuario":"PACIENTE"}"""))
        api.me().execute()
        val req = servidor.takeRequest()
        assertEquals("/usuario/me", req.path); assertEquals("Bearer jwt-protegido", req.getHeader("Authorization"))
    }
    @Test fun mfaObrigatorioNaoProduzSessao() {
        servidor.enqueue(MockResponse().setResponseCode(428).setBody("""{"message":"Informe o segundo fator.","codigo":"MFA_REQUIRED"}"""))
        val r = api.login(LoginRequest("p@teste.invalid", "Senha123!")).execute()
        assertNull(r.body()); assertEquals("MFA_REQUIRED", ErroApi.de(r).codigo)
    }
    @Test fun limitePreservaStatusEEspera() {
        servidor.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "42").setBody("""{"message":"Muitas tentativas."}"""))
        val erro = ErroApi.de(api.login(LoginRequest("p@teste.invalid", "errada")).execute())
        assertEquals(429, erro.status); assertEquals(42L, erro.espera)
    }
    @Test fun ativacaoEnviaSomenteTokenESenha() {
        servidor.enqueue(MockResponse().setBody("""{"token":"a.b.c","usuario":{"id":9,"tipoUsuario":"PACIENTE"}}"""))
        api.ativar(AtivacaoEntrada("codigo-entregue-pelo-profissional", "Senha123!")).execute()
        val req = servidor.takeRequest()
        assertEquals("/auth/ativar", req.path); assertNull(req.getHeader("Authorization"))
        assertEquals(setOf("token", "senha"), dados(req.body.readUtf8()).keys)
    }
    @Test fun consultaDeAtivacaoNaoEnviaSessaoAnterior() {
        servidor.enqueue(MockResponse().setBody("""{"nome":"Aluno","email":"aluno@teste.invalid"}"""))
        api.consultar(AtivacaoConsulta("codigo")).execute()
        val req = servidor.takeRequest()
        assertEquals("/auth/ativacao/consultar", req.path); assertNull(req.getHeader("Authorization"))
    }
    @Test fun edicaoEnviaVersaoEVariasAnterioresSemResponsavel() {
        servidor.enqueue(MockResponse().setBody("""{"id":11,"versao":8}"""))
        api.editarMeta(11, MetaEntrada(anteriores = listOf(3, 9), versao = 7)).execute()
        val req = servidor.takeRequest()
        assertEquals("PATCH", req.method); assertEquals("/meta/11", req.path)
        val dto = dados(req.body.readUtf8())
        assertEquals(listOf(3.0, 9.0), dto["anteriores"]); assertEquals(7.0, dto["versao"])
        assertFalse(dto.containsKey("responsavel")); assertFalse(dto.containsKey("idPaciente"))
    }
    @Test fun conclusaoUsaCampoDoBackend() {
        servidor.enqueue(MockResponse().setBody("""{"id":11,"concluido":true,"versao":8}"""))
        api.editarMeta(11, MetaEntrada(concluido = true, versao = 7)).execute()
        assertEquals(setOf("concluido", "versao"), dados(servidor.takeRequest().body.readUtf8()).keys)
    }
    @Test fun equipeMantemEnumLegadoSemInventarAceite() {
        servidor.enqueue(MockResponse().setBody("""[{"id":3,"solicitacao":"PRENDENTE","usuarioDestinatario":{"id":7}}]"""))
        val r = api.equipe(9).execute()
        assertEquals("PRENDENTE", r.body()!!.first().solicitacao)
        assertEquals("/paciente/9/equipe", servidor.takeRequest().path)
    }
    @Test fun conviteNaoEnviaEstadoOuDono() {
        servidor.enqueue(MockResponse().setBody("""{"id":3,"solicitacao":"PRENDENTE"}"""))
        api.convidar(9, ConviteEquipe(7)).execute()
        assertEquals(setOf("idProfissional"), dados(servidor.takeRequest().body.readUtf8()).keys)
    }
    @Test fun perfilPreservaCamposForaDoFormulario() {
        val p = gson.fromJson("""{"nome":"Aluno","condicao":"Condição","ocupacao":"Psicólogo","horarioDeEntrada":"08:00:00"}""", Perfil::class.java)
        servidor.enqueue(MockResponse().setBody("{}"))
        api.perfil(7, p.copy(nome = "Nome atualizado")).execute()
        val dto = dados(servidor.takeRequest().body.readUtf8())
        assertEquals("Condição", dto["condicoes"]); assertEquals("Psicólogo", dto["ocupacao"])
        assertEquals("08:00:00", dto["horarioDeEntrada"])
    }
    @Test fun apontamentoNaoDefineAutor() {
        servidor.enqueue(MockResponse().setBody("""{"id":2}"""))
        api.publicar(11, PostagemEntrada("Título", "<p>Texto</p>", respondeA = 1)).execute()
        val req = servidor.takeRequest()
        assertEquals("/postagem/criar?idMeta=11", req.path)
        val dto = dados(req.body.readUtf8())
        assertEquals(1.0, dto["respondeA"]); assertFalse(dto.containsKey("autor"))
    }
    @Test fun httpPublicoERecusadoMesmoEmDebug() {
        for (url in listOf("http://exemplo.com/", "http://10.a.0.2.2/")) {
            try { RetrofitClient.criar(url, { "" }, true); fail("HTTP público") }
            catch (_: IllegalArgumentException) {}
        }
        try { RetrofitClient.criar("http://10.0.2.2:8181/", { "" }, false); fail("Release HTTP") }
        catch (_: IllegalArgumentException) {}
    }
    @Test fun redirecionamentoNaoEncaminhaCredenciais() {
        servidor.enqueue(MockResponse().setResponseCode(307).setHeader("Location", "https://outro.invalid/login"))
        assertEquals(307, api.login(LoginRequest("p@teste.invalid", "Senha123!")).execute().code())
        assertEquals(1, servidor.requestCount)
    }
    @Test fun postNaoRepeteAutomaticamenteAposPerdaDaResposta() {
        servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
        try { api.publicar(11, PostagemEntrada("Título", "Texto")).execute(); fail("Esperava falha") }
        catch (_: IOException) {}
        assertEquals(1, servidor.requestCount)
    }
    @Test fun linkDeAtivacaoNaoPrecisaSerAberto() {
        val codigo = "a".repeat(43)
        assertEquals(codigo, Ativacao.token("https://site.invalid/ativar.html#token=$codigo"))
        assertEquals(codigo, Ativacao.token("https://site.invalid/ativar.html?token=$codigo"))
        assertEquals(codigo, Ativacao.token(codigo))
    }
    @Test fun resposta403ContinuaComoErroSemDados() {
        servidor.enqueue(MockResponse().setResponseCode(403).setBody("""{"message":"Sem acesso ao paciente."}"""))
        val r = api.equipe(999).execute()
        assertNull(r.body()); assertEquals(403, ErroApi.de(r).status)
    }
    @Test fun anexoUsaDadosEArquivosComJwt() {
        servidor.enqueue(MockResponse().setBody("""{"id":2}"""))
        val json = gson.toJson(PostagemEntrada("Título", "Texto")).toRequestBody("application/json".toMediaType())
        val parte = MultipartBody.Part.createFormData("arquivos", "teste.txt", "arquivo".toRequestBody("text/plain".toMediaType()))
        api.publicarArquivo(11, json, parte).execute()
        val req = servidor.takeRequest(); val body = req.body.readUtf8()
        assertEquals("/postagem/publicar?idMeta=11", req.path)
        assertTrue(body.contains("name=\"dados\"")); assertTrue(body.contains("name=\"arquivos\""))
        assertEquals("Bearer jwt-protegido", req.getHeader("Authorization"))
    }
    @Test fun conexoesEnviamEntradasSaidasEVersoes() {
        servidor.enqueue(MockResponse().setBody("""[{"id":11,"anteriores":[3,9],"versao":8}]"""))
        val r = api.conectarMetas(11, ConexoesEntrada(listOf(3, 9), listOf(15), mapOf(3L to 2L, 9L to 4L, 11L to 7L, 15L to 1L))).execute()
        assertEquals(listOf(3L, 9L), r.body()!!.first().anteriores)
        val req = servidor.takeRequest()
        assertEquals("PATCH", req.method); assertEquals("/meta/11/conexoes", req.path)
        val dto = dados(req.body.readUtf8())
        assertEquals(listOf(3.0, 9.0), dto["anteriores"]); assertEquals(listOf(15.0), dto["seguintes"])
        assertEquals(7.0, (dto["versoes"] as Map<*, *>)["11"])
        assertFalse(dto.containsKey("autor"))
    }
    @Test fun conflitoDeConexaoNaoRetornaSucesso() {
        servidor.enqueue(MockResponse().setResponseCode(409).setBody("""{"message":"O mapa mudou."}"""))
        val r = api.conectarMetas(11, ConexoesEntrada(emptyList(), emptyList(), mapOf(11L to 1L))).execute()
        assertNull(r.body()); assertEquals(409, ErroApi.de(r).status)
    }
    @Test fun downloadUsaRotaProtegida() {
        servidor.enqueue(MockResponse().setBody("conteudo"))
        val r = api.baixar(2).execute()
        r.body()!!.use { assertEquals("conteudo", it.string()) }
        val req = servidor.takeRequest()
        assertEquals("/anexo/2/arquivo", req.path); assertEquals("Bearer jwt-protegido", req.getHeader("Authorization"))
    }
    @Test fun urlComCredenciaisOuParametrosERecusada() {
        for (url in listOf("https://usuario:senha@api.invalid/", "https://api.invalid/?token=segredo", "https://api.invalid/#fragmento")) {
            try { RetrofitClient.criar(url, { "" }, true); fail("URL inválida: $url") }
            catch (_: IllegalArgumentException) {}
        }
    }
    @Test fun aceitarConviteUsaPutSemForjarEstado() {
        servidor.enqueue(MockResponse().setBody("""{"id":3,"solicitacao":"ACEITA"}"""))
        api.aceitar(3).execute()
        val req = servidor.takeRequest()
        assertEquals("PUT", req.method); assertEquals("/usuario-aluno/3/aceitar", req.path)
        assertEquals(0L, req.bodySize)
    }
    @Test fun ativacaoRecusaCpfComoToken() {
        try { Ativacao.token("12345678900"); fail("CPF não é token") }
        catch (_: IllegalArgumentException) {}
    }
}
