package com.example.conjuntoresidencial.ui.feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.model.PublicacionDTO
import com.example.conjuntoresidencial.util.Resource
import kotlinx.coroutines.launch

/** Feed de Inicio: las publicaciones cargadas hasta ahora; se piden 20 más al llegar al final. */
class FeedViewModel : ViewModel() {
    private val _publicaciones = MutableLiveData<Resource<List<PublicacionDTO>>>()
    val publicaciones: LiveData<Resource<List<PublicacionDTO>>> get() = _publicaciones

    private val cargadas = mutableListOf<PublicacionDTO>()
    private var pagina = 0
    private var hayMas = true
    private var cargando = false

    fun recargar() {
        pagina = 0
        hayMas = true
        cargadas.clear()
        _publicaciones.value = Resource.Loading
        cargarPagina()
    }

    /** Llegó al final de la lista: la siguiente página, si hay. */
    fun cargarMas() {
        if (hayMas && !cargando) cargarPagina()
    }

    /** Se abrió la publicación: se pinta como leída de inmediato y se avisa al servidor (si falla, se queda leída aquí). */
    fun marcarLeida(id: Long) {
        val i = cargadas.indexOfFirst { it.id == id }
        if (i < 0 || cargadas[i].leida) return
        cargadas[i] = cargadas[i].copy(leida = true)
        _publicaciones.value = Resource.Success(cargadas.toList())
        viewModelScope.launch {
            try { RetrofitClient.comun.marcarPublicacionLeida(id) } catch (_: Exception) { }
        }
    }

    private fun cargarPagina() {
        cargando = true
        viewModelScope.launch {
            try {
                val r = RetrofitClient.comun.getPublicaciones(pagina)
                val cuerpo = r.body()
                if (r.isSuccessful && cuerpo != null) {
                    cargadas += cuerpo.publicaciones
                    hayMas = cuerpo.hayMas
                    pagina++
                    _publicaciones.value = Resource.Success(cargadas.toList())
                } else if (cargadas.isEmpty()) {
                    _publicaciones.value = Resource.Error(RetrofitClient.mensajeDeError(r, "No se pudieron cargar las publicaciones"))
                }
            } catch (e: Exception) {
                if (cargadas.isEmpty()) _publicaciones.value = Resource.Error(RetrofitClient.mensajeDeExcepcion(e))
            }
            cargando = false
        }
    }
}
