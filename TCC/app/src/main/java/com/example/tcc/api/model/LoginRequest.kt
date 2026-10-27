package com.example.tcc.api.model

data class LoginRequest(val email: String, val senha: String, val codigo: String? = null)
