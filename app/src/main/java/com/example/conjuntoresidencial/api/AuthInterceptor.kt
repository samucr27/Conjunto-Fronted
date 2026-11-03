package com.example.conjuntoresidencial.api

import com.example.conjuntoresidencial.BuildConfig
import android.content.Context
import com.example.conjuntoresidencial.util.ConjuntoElegido
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
class AuthInterceptor(private val sessionManager: SessionManager, private val context: Context) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val ruta = request.url.encodedPath
        // P7: la app de plataforma entra por su propio login
        val esLogin = ruta.startsWith("/api/auth/") && ruta.endsWith("/login") || ruta == "/api/plataforma/login"
        val token = sessionManager.getToken()

        // X-App-Tipo: qué app hace la petición (RESIDENTE o ADMIN); el servidor lo contrasta con el rol
        val conTipo = request.newBuilder().header("X-App-Tipo", BuildConfig.TIPO_APP)
        // P7: el conjunto elegido; con sesión el servidor usa el del token
        ConjuntoElegido.id(context)?.let { conTipo.header("X-Conjunto", it.toString()) }
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
