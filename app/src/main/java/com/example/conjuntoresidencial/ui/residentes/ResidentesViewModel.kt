package com.example.conjuntoresidencial.ui.residentes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.model.ResidenteDTO
import com.example.conjuntoresidencial.util.Evento
import com.example.conjuntoresidencial.util.Resource
import kotlinx.coroutines.launch
import retrofit2.Response

/** Residentes del apartamento (Mi hogar) o, para el administrador, los arrendatarios por aprobar. */
class ResidentesViewModel : ViewModel() {
    private val _residentes = MutableLiveData<Resource<List<ResidenteDTO>>>()
    val residentes: LiveData<Resource<List<ResidenteDTO>>> get() = _residentes

    private val _mensaje = MutableLiveData<Evento<String>>()
    val mensaje: LiveData<Evento<String>> get() = _mensaje

    private var pendientes = false
    private var aptoId = -1L

    fun cargar(pendientes: Boolean, aptoId: Long) {
        this.pendientes = pendientes
        this.aptoId = aptoId
        recargar()
    }

    fun recargar() {
        _residentes.value = Resource.Loading
        viewModelScope.launch {
            try {
                val response = if (pendientes) RetrofitClient.instance.getResidentesPendientes()
                else RetrofitClient.instance.getResidentesPorApartamento(aptoId)
                _residentes.value = if (response.isSuccessful) Resource.Success(response.body().orEmpty())
                else Resource.Error(RetrofitClient.mensajeDeError(response, "No se pudieron cargar los residentes"))
            } catch (e: Exception) {
                _residentes.value = Resource.Error(RetrofitClient.mensajeDeExcepcion(e))
            }
        }
    }

    /** Agregar, quitar, aprobar o rechazar: avisa con [exito] y recarga la lista. */
    fun ejecutar(exito: String, error: String, peticion: suspend () -> Response<*>) {
        viewModelScope.launch {
            try {
                val response = peticion()
                if (response.isSuccessful) {
                    _mensaje.value = Evento(exito)
                    recargar()
                } else {
                    _mensaje.value = Evento(RetrofitClient.mensajeDeError(response, error))
                }
            } catch (e: Exception) {
                _mensaje.value = Evento(RetrofitClient.mensajeDeExcepcion(e))
            }
        }
    }
}
