package com.example.conjuntoresidencial.api

import okhttp3.Interceptor
import okhttp3.Response
import java.net.SocketTimeoutException

/**
 * Cambia el mensaje técnico de un tiempo de espera agotado ("timeout", "Read timed out")
 * por uno que el usuario entienda. Las pantallas muestran e.localizedMessage, así que
 * basta con hacerlo aquí, en un solo lugar.
 *
 * No reintenta: repetir un POST podría duplicar una reserva o un vehículo.
 */
class TiempoEsperaInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        try {
            return chain.proceed(chain.request())
        } catch (e: SocketTimeoutException) {
            // Debe seguir siendo una IOException: OkHttp no acepta otro tipo desde un interceptor
            throw SocketTimeoutException(MENSAJE).apply { initCause(e) }
        }
    }

    companion object {
        const val MENSAJE = "El servidor está iniciando, intenta de nuevo en unos segundos"
    }
}
