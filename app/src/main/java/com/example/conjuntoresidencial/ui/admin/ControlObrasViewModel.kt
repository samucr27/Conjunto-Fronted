package com.example.conjuntoresidencial.ui.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.model.ObraDTO
import com.example.conjuntoresidencial.util.Resource
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.util.Locale

/**
 * Obras de la administración (P5): de los apartamentos o del conjunto, en curso o finalizadas.
 * La búsqueda por torre, apto o descripción filtra lo ya cargado, sin otra petición por cada letra.
 */
class ControlObrasViewModel : ViewModel() {
    private val _obras = MutableLiveData<Resource<List<ObraDTO>>>()
    val obras: LiveData<Resource<List<ObraDTO>>> get() = _obras

    var alcance = APARTAMENTOS
    var texto = ""
    private var cargadas: List<ObraDTO> = emptyList()

    fun fetchObras(finalizadas: Boolean) {
        _obras.value = Resource.Loading
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.buscarObras(alcance, if (finalizadas) "FINALIZADA" else "EN_PROCESO")
                if (response.isSuccessful && response.body() != null) {
                    // Primero las que esperan aprobación (de la obra o de su personal)
                    cargadas = response.body()!!.sortedByDescending {
                        it.aprobacion == "PENDIENTE" || (it.personalPendiente ?: 0) > 0
                    }
                    filtrar()
                } else _obras.value = Resource.Error(RetrofitClient.mensajeDeError(response, "Error al cargar obras"))
            } catch (e: Exception) {
                _obras.value = Resource.Error(RetrofitClient.mensajeDeExcepcion(e))
            }
        }
    }

    /** "1 101", "torre 2", "cocina": todas las palabras deben aparecer en la torre, el apto o la descripción. */
    fun filtrar() {
        val palabras = normal(texto).split(Regex("\\s+")).filter { it.isNotBlank() }
        _obras.value = Resource.Success(cargadas.filter { o ->
            val en = normal("torre ${o.torre.orEmpty()} apto ${o.apto.orEmpty()} ${o.descripcionAdecuacion}")
            palabras.all { en.contains(it) }
        })
    }

    val hayBusqueda get() = texto.isNotBlank()

    private fun normal(s: String) =
        Normalizer.normalize(s.lowercase(Locale.ROOT), Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")

    companion object {
        const val APARTAMENTOS = "APARTAMENTOS"
        const val CONJUNTO = "CONJUNTO"
    }
}
