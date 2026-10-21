package com.example.conjuntoresidencial.ui.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.model.EstadoRequest
import com.example.conjuntoresidencial.model.ObraDTO
import com.example.conjuntoresidencial.model.PersonalObraDTO
import com.example.conjuntoresidencial.util.Evento
import com.example.conjuntoresidencial.util.Resource
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class ControlObrasViewModel : ViewModel() {
    private val _obras = MutableLiveData<Resource<List<ObraDTO>>>()
    val obras: LiveData<Resource<List<ObraDTO>>> get() = _obras

    // obraId -> personal; se carga junto con las obras (una sola petición para todo el personal)
    var personalPorObra: Map<Long, List<PersonalObraDTO>> = emptyMap()
        private set

    private val _mensaje = MutableLiveData<Evento<String>>()
    val mensaje: LiveData<Evento<String>> get() = _mensaje

    fun fetchObras() {
        _obras.value = Resource.Loading
        viewModelScope.launch {
            try {
                // Trae TODAS las obras (en proceso y finalizadas) y todo el personal en paralelo.
                // coroutineScope: si una petición falla, el error llega al catch en vez de cerrar la app
                val (response, personal) = coroutineScope {
                    val obrasAsync = async { RetrofitClient.instance.getAllObras() }
                    val personalAsync = async { RetrofitClient.instance.getTodoElPersonal() }
                    obrasAsync.await() to personalAsync.await()
                }
                personalPorObra = if (personal.isSuccessful) personal.body().orEmpty().groupBy { it.obraId } else emptyMap()
                if (response.isSuccessful && response.body() != null)
                    _obras.value = Resource.Success(response.body()!!)
                else _obras.value = Resource.Error("Error al cargar obras")
            } catch (e: Exception) {
                _obras.value = Resource.Error("Error de red: ${e.localizedMessage}")
            }
        }
    }

    fun cambiarEstado(obraId: Long, nuevoEstado: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.cambiarEstadoObra(obraId, EstadoRequest(nuevoEstado))
                if (response.isSuccessful) {
                    // Actualizar solo ese item sin recargar toda la lista
                    val listaActual = (_obras.value as? Resource.Success)?.data?.toMutableList() ?: return@launch
                    val idx = listaActual.indexOfFirst { it.id == obraId }
                    if (idx != -1) {
                        listaActual[idx] = listaActual[idx].copy(estadoObra = nuevoEstado)
                        _obras.value = Resource.Success(listaActual)
                    }
                } else {
                    _mensaje.value = Evento(RetrofitClient.mensajeDeError(response, "No se pudo cambiar el estado de la obra"))
                }
            } catch (e: Exception) {
                _mensaje.value = Evento("Error de red: ${e.localizedMessage}")
            }
        }
    }
}
