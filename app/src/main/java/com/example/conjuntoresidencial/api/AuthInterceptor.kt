package com.example.conjuntoresidencial.api

import com.example.conjuntoresidencial.util.SesionEventos
import com.example.conjuntoresidencial.util.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Añade "Authorization: Bearer <token>" a cada petición y reacciona a las
 * respuestas 401 (sesión caducada o inválida) y 403 (sin permiso).
 * El login no lleva token y sus 401 los maneja la propia pantalla.
 */
class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val esLogin = request.url().encodedPath().startsWith("/api/auth/")
        val token = sessionManager.getToken()

        val conToken = if (!esLogin && token != null) {
            request.newBuilder().header("Authorization", "Bearer $token").build()
        } else request

        val response = chain.proceed(conToken)

        if (!esLogin) {
            when (response.code()) {
                401 -> {
                    sessionManager.clearSession()
                    SesionEventos.notificarExpirada()
                }
                403 -> SesionEventos.notificarSinPermiso()
            }
        }
        return response
    }
}
