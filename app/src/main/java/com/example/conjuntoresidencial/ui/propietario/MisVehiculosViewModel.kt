package com.example.conjuntoresidencial.ui.propietario

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.model.CupoVehiculoDTO
import com.example.conjuntoresidencial.model.VehiculoDTO
import com.example.conjuntoresidencial.util.Evento
import com.example.conjuntoresidencial.util.Resource
import kotlinx.coroutines.launch

class MisVehiculosViewModel : ViewModel() {

    private val _vehiculos = MutableLiveData<Resource<List<VehiculoDTO>>>()
    val vehiculos: LiveData<Resource<List<VehiculoDTO>>> get() = _vehiculos

    private val _mensaje = MutableLiveData<Evento<String>>()
    val mensaje: LiveData<Evento<String>> get() = _mensaje

    /** Cupos por tipo para el diálogo; null si no cargaron (se usan los de por defecto). */
    var cupos: List<CupoVehiculoDTO>? = null
        private set

    fun cargarCupos() {
        viewModelScope.launch {
            cupos = try {
                RetrofitClient.instance.getCuposVehiculo().body()
            } catch (_: Exception) {
                null
            }
        }
    }

    fun fetchVehiculos(aptoId: Long) {
        _vehiculos.value = Resource.Loading
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getVehiculosPorApartamento(aptoId)
                if (response.isSuccessful) {
                    _vehiculos.value = Resource.Success(response.body() ?: emptyList())
                } else {
                    _vehiculos.value = Resource.Error("Error al cargar vehículos")
                }
            } catch (e: Exception) {
                _vehiculos.value = Resource.Error(RetrofitClient.mensajeDeExcepcion(e))
            }
        }
    }

    /** Lo que registra el residente queda por aprobar: [enviado] lo explica. */
    fun agregarVehiculo(vehiculo: VehiculoDTO, enviado: String, errorPorDefecto: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.createVehiculo(vehiculo)
                if (response.isSuccessful) {
                    // El backend crea la notificación para el admin
                    _mensaje.value = Evento(enviado)
                    fetchVehiculos(vehiculo.apartamentoId)
                } else {
                    _mensaje.value = Evento(RetrofitClient.mensajeDeError(response, errorPorDefecto))
                }
            } catch (e: Exception) {
                _mensaje.value = Evento(RetrofitClient.mensajeDeExcepcion(e))
            }
        }
    }

    fun eliminarVehiculo(vehiculo: VehiculoDTO, aptoId: Long, errorPorDefecto: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.deleteVehiculo(vehiculo.id!!)
                if (response.isSuccessful) fetchVehiculos(aptoId)
                else _mensaje.value = Evento(RetrofitClient.mensajeDeError(response, errorPorDefecto))
            } catch (e: Exception) {
                _mensaje.value = Evento(RetrofitClient.mensajeDeExcepcion(e))
            }
        }
    }
}