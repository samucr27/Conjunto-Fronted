package com.example.conjuntoresidencial.api

import com.example.conjuntoresidencial.BuildConfig
import com.example.conjuntoresidencial.util.SesionEventos
import com.example.conjuntoresidencial.util.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Añade "Authorization: Bearer <token>" a cada petición y reacciona a las
 * respuestas 401 (sesión caducada o inválida) y 403 (sin permiso).
 * El login no lleva token y sus 401 los maneja la propia pantalla. Las demás rutas de
 * /api/auth (cambiar la contraseña) sí lo llevan.
 */
class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val ruta = request.url.encodedPath
        val esLogin = ruta.startsWith("/api/auth/") && ruta.endsWith("/login")
        val token = sessionManager.getToken()

        // X-App-Tipo: qué app hace la petición (RESIDENTE o ADMIN); el servidor lo contrasta con el rol
        val conTipo = request.newBuilder().header("X-App-Tipo", BuildConfig.TIPO_APP)
        val conToken = if (!esLogin && token != null) {
            conTipo.header("Authorization", "Bearer $token").build()
        } else conTipo.build()

        val response = chain.proceed(conToken)

        if (!esLogin) {
            when (response.code) {
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
