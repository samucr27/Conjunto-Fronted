package com.example.conjuntoresidencial

import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.api.TiempoEsperaInterceptor
import com.google.gson.JsonSyntaxException
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

class MensajesErrorTest {

    @Test
    fun losMensajesDeRedTraducidosSeMuestranTalCual() {
        val e = SocketTimeoutException(TiempoEsperaInterceptor.MENSAJE)
        assertEquals(TiempoEsperaInterceptor.MENSAJE, RetrofitClient.mensajeDeExcepcion(e))
    }

    @Test
    fun otrosFallosDeRedSeMuestranComoSinConexion() {
        val e = IOException("unexpected end of stream on http://10.0.2.2:8080/...")
        assertEquals(TiempoEsperaInterceptor.MENSAJE_SIN_CONEXION, RetrofitClient.mensajeDeExcepcion(e))
    }

    @Test
    fun losErroresTecnicosNoLleganAlUsuario() {
        val e = JsonSyntaxException("Expected BEGIN_OBJECT but was STRING at line 1 column 1")
        assertEquals(RetrofitClient.MENSAJE_INESPERADO, RetrofitClient.mensajeDeExcepcion(e))
    }
}
