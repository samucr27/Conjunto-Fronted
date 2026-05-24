package com.example.conjuntoresidencial.ui.propietario

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.model.VehiculoDTO
import com.example.conjuntoresidencial.util.Resource
import kotlinx.coroutines.launch

class MisVehiculosViewModel : ViewModel() {

    private val _vehiculos = MutableLiveData<Resource<List<VehiculoDTO>>>()
    val vehiculos: LiveData<Resource<List<VehiculoDTO>>> get() = _vehiculos

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
                _vehiculos.value = Resource.Error("Error de red: ${e.localizedMessage}")
            }
        }
    }

    fun agregarVehiculo(vehiculo: VehiculoDTO) {
        viewModelScope.launch {
            try {
                RetrofitClient.instance.createVehiculo(vehiculo)
                fetchVehiculos(vehiculo.apartamentoId)
            } catch (_: Exception) {}
        }
    }

    fun eliminarVehiculo(vehiculo: VehiculoDTO, aptoId: Long) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.deleteVehiculo(vehiculo.id!!)
                if (response.isSuccessful) fetchVehiculos(aptoId)
            } catch (_: Exception) {}
        }
    }
}