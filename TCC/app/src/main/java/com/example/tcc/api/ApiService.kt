package com.example.tcc.api

import com.example.tcc.api.model.LoginRequest
import com.example.tcc.api.model.UsuarioResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("auth/login")
    fun login(
        @Body login: LoginRequest
    ): Call<UsuarioResponse>
}