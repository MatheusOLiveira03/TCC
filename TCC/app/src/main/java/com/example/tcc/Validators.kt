package com.example.tcc

import android.util.Patterns

object Validators {
    fun isEmailValid(email: String): Boolean =
        email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()

    fun isPasswordValid(password: String): Boolean = password.length >= 8

    fun isCpfValid(rawCpf: String): Boolean {
        val cpf = rawCpf.filter(Char::isDigit)
        if (cpf.length != 11 || cpf.all { it == cpf[0] }) return false

        fun digit(length: Int): Int {
            var sum = 0
            var weight = length + 1
            for (index in 0 until length) {
                sum += (cpf[index] - '0') * weight--
            }
            val remainder = (sum * 10) % 11
            return if (remainder == 10) 0 else remainder
        }

        return digit(9) == (cpf[9] - '0') && digit(10) == (cpf[10] - '0')
    }
}
