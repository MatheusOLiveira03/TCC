package com.example.tcc.api

import com.example.tcc.api.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {
    @POST("login") fun login(@Body dados: LoginRequest): Call<SessaoResponse>
    @GET("usuario/me") fun me(): Call<UsuarioResponse>
    @POST("auth/ativacao/consultar") fun consultar(@Body dados: AtivacaoConsulta): Call<AtivacaoResumo>
    @POST("auth/ativar") fun ativar(@Body dados: AtivacaoEntrada): Call<SessaoResponse>
    @PUT("usuario/descricao") fun perfil(@Query("id") id: Long, @Body dados: Perfil): Call<Perfil>
    @PUT("usuario/editar") fun email(@Query("id") id: Long, @Body dados: ContaEntrada): Call<UsuarioResponse>
    @GET("paciente") fun pacientes(): Call<List<UsuarioResponse>>
    @GET("paciente/{id}/equipe") fun equipe(@Path("id") id: Long): Call<List<Relacao>>
    @GET("usuario-aluno") fun vinculos(): Call<List<Relacao>>
    @PUT("usuario-aluno/{id}/aceitar") fun aceitar(@Path("id") id: Long): Call<Relacao>
    @DELETE("usuario-aluno/{id}") fun removerVinculo(@Path("id") id: Long): Call<Void>
    @GET("usuario/buscar") fun profissionais(@Query("nome") nome: String): Call<List<Pessoa>>
    @POST("paciente/{id}/equipe") fun convidar(@Path("id") id: Long, @Body dados: ConviteEquipe): Call<Relacao>
    @GET("meta") fun metas(@Query("idPaciente") id: Long): Call<List<Meta>>
    @POST("meta/criar") fun criarMeta(@Query("idPaciente") id: Long, @Body dados: MetaEntrada): Call<Meta>
    @PATCH("meta/{id}") fun editarMeta(@Path("id") id: Long, @Body dados: MetaEntrada): Call<Meta>
    @PATCH("meta/{id}/conexoes") fun conectarMetas(@Path("id") id: Long, @Body dados: ConexoesEntrada): Call<List<Meta>>
    @DELETE("meta/{id}") fun removerMeta(@Path("id") id: Long): Call<Void>
    @GET("postagem") fun postagens(@Query("idMeta") id: Long): Call<List<Postagem>>
    @POST("postagem/criar") fun publicar(@Query("idMeta") id: Long, @Body dados: PostagemEntrada): Call<Postagem>
    @Multipart @POST("postagem/publicar")
    fun publicarArquivo(@Query("idMeta") id: Long, @Part("dados") dados: RequestBody, @Part arquivo: MultipartBody.Part): Call<Postagem>
    @Streaming @GET("anexo/{id}/arquivo") fun baixar(@Path("id") id: Long): Call<ResponseBody>
    @PATCH("postagem/{id}") fun editarPostagem(@Path("id") id: Long, @Body dados: PostagemEntrada): Call<Postagem>
    @DELETE("postagem/{id}") fun removerPostagem(@Path("id") id: Long): Call<Void>
}
