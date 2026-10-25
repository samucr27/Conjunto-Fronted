package com.example.conjuntoresidencial.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/** Evento que se consume una sola vez (no se repite al rotar la pantalla). */
class Evento<out T>(private val contenido: T) {
    private var manejado = false

    fun obtenerSiNoManejado(): T? =
        if (manejado) null else { manejado = true; contenido }

    /**
     * Para eventos cuyo contenido puede ser null a propósito (Evento<String?>: null = salió bien):
     * [obtenerSiNoManejado] no distingue "ya manejado" de "null", así que el éxito nunca se veía.
     */
    fun siNoManejado(accion: (T) -> Unit) {
        if (manejado) return
        manejado = true
        accion(contenido)
    }
}

/**
 * Avisos globales que llegan desde la capa de red (AuthInterceptor) y que
 * MainActivity muestra sin importar en qué pantalla esté el usuario.
 */
object SesionEventos {
    private val _expirada = MutableLiveData<Evento<Unit>>()
    val expirada: LiveData<Evento<Unit>> get() = _expirada

    private val _sinPermiso = MutableLiveData<Evento<Unit>>()
    val sinPermiso: LiveData<Evento<Unit>> get() = _sinPermiso

    // postValue: se llaman desde el hilo de OkHttp
    fun notificarExpirada() = _expirada.postValue(Evento(Unit))
    fun notificarSinPermiso() = _sinPermiso.postValue(Evento(Unit))
}
