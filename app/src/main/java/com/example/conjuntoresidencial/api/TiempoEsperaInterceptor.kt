package com.example.conjuntoresidencial.api

import okhttp3.Interceptor
import okhttp3.Response
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Cambia los mensajes técnicos de red ("timeout", "Failed to connect to /10.0.2.2:8080",
 * "Unable to resolve host") por mensajes que el usuario entienda. Las pantallas muestran
 * e.localizedMessage, así que basta con hacerlo aquí, en un solo lugar.
 *
 * No reintenta: repetir un POST podría duplicar una reserva o un vehículo.
 */
class TiempoEsperaInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // Siempre se relanza una IOException: OkHttp no acepta otro tipo desde un interceptor
        try {
            return chain.proceed(chain.request())
        } catch (e: SocketTimeoutException) {
            throw SocketTimeoutException(MENSAJE).apply { initCause(e) }
        } catch (e: ConnectException) {
            throw ConnectException(MENSAJE_SIN_CONEXION).apply { initCause(e) }
        } catch (e: UnknownHostException) {
            throw UnknownHostException(MENSAJE_SIN_CONEXION).apply { initCause(e) }
        }
    }

    companion object {
        const val MENSAJE = "El servidor está iniciando, intenta de nuevo en unos segundos"
        const val MENSAJE_SIN_CONEXION = "No se pudo conectar con el servidor. Revisa tu conexión a internet"
    }
}
