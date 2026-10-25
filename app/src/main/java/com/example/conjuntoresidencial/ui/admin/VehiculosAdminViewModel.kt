package com.example.conjuntoresidencial.ui.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.model.CupoVehiculoDTO
import com.example.conjuntoresidencial.model.EstadoVehiculoRequest
import com.example.conjuntoresidencial.model.VehiculoDTO
import com.example.conjuntoresidencial.util.Evento
import com.example.conjuntoresidencial.util.Resource
import kotlinx.coroutines.launch

/** Vehículos del administrador: los que esperan aprobación y los cupos por tipo. */
class VehiculosAdminViewModel : ViewModel() {

    private val _pendientes = MutableLiveData<Resource<List<VehiculoDTO>>>()
    val pendientes: LiveData<Resource<List<VehiculoDTO>>> get() = _pendientes

    // Los cupos llegan una vez por cada "⋮ → Cupos por apartamento" (abren el editor)
    private val _cupos = MutableLiveData<Evento<List<CupoVehiculoDTO>>>()
    val cupos: LiveData<Evento<List<CupoVehiculoDTO>>> get() = _cupos

    // Mensajes de una sola vez ("Vehículo aprobado", errores)
    private val _mensaje = MutableLiveData<Evento<String>>()
    val mensaje: LiveData<Evento<String>> get() = _mensaje

    fun cargar(errorPorDefecto: String) {
        _pendientes.value = Resource.Loading
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getVehiculosPorEstado(ESTADO_PENDIENTE)
                _pendientes.value = if (response.isSuccessful) Resource.Success(response.body().orEmpty())
                    else Resource.Error(RetrofitClient.mensajeDeError(response, errorPorDefecto))
            } catch (e: Exception) {
                _pendientes.value = Resource.Error(RetrofitClient.mensajeDeExcepcion(e))
            }
        }
    }

    /** Aprueba o rechaza; al terminar, el vehículo sale de la lista y el residente recibe un aviso. */
    fun revisar(vehiculo: VehiculoDTO, aprobar: Boolean, motivo: String?, hecho: String, errorPorDefecto: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.cambiarEstadoVehiculo(
                    vehiculo.id!!, EstadoVehiculoRequest(if (aprobar) "APROBADO" else "RECHAZADO", motivo)
                )
                if (response.isSuccessful) {
                    val actuales = (_pendientes.value as? Resource.Success)?.data.orEmpty()
                    _pendientes.value = Resource.Success(actuales.filter { it.id != vehiculo.id })
                    _mensaje.value = Evento(hecho)
                } else {
                    _mensaje.value = Evento(RetrofitClient.mensajeDeError(response, errorPorDefecto))
                }
            } catch (e: Exception) {
                _mensaje.value = Evento(RetrofitClient.mensajeDeExcepcion(e))
            }
        }
    }

    fun cargarCupos(errorPorDefecto: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getCuposVehiculo()
                if (response.isSuccessful) _cupos.value = Evento(response.body().orEmpty())
                else _mensaje.value = Evento(RetrofitClient.mensajeDeError(response, errorPorDefecto))
            } catch (e: Exception) {
                _mensaje.value = Evento(RetrofitClient.mensajeDeExcepcion(e))
            }
        }
    }

    fun guardarCupos(cupos: List<CupoVehiculoDTO>, hecho: String, errorPorDefecto: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.guardarCuposVehiculo(cupos)
                if (response.isSuccessful) {
                    _mensaje.value = Evento(hecho)
                } else {
                    _mensaje.value = Evento(RetrofitClient.mensajeDeError(response, errorPorDefecto))
                }
            } catch (e: Exception) {
                _mensaje.value = Evento(RetrofitClient.mensajeDeExcepcion(e))
            }
        }
    }

    private companion object {
        const val ESTADO_PENDIENTE = "PENDIENTE"
    }
}
