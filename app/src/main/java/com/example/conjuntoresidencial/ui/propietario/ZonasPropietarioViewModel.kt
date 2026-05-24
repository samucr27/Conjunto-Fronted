package com.example.conjuntoresidencial.ui.propietario

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.model.ReservaDTO
import com.example.conjuntoresidencial.model.ZonaComunDTO
import com.example.conjuntoresidencial.util.Resource
import kotlinx.coroutines.launch

class ZonasPropietarioViewModel : ViewModel() {

    private val _zonas = MutableLiveData<Resource<List<ZonaComunDTO>>>()
    val zonas: LiveData<Resource<List<ZonaComunDTO>>> get() = _zonas

    private val _reservaState = MutableLiveData<Resource<ReservaDTO>>()
    val reservaState: LiveData<Resource<ReservaDTO>> get() = _reservaState

    fun fetchZonas() {
        _zonas.value = Resource.Loading
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getZonasComunes()
                if (response.isSuccessful && response.body() != null) {
                    _zonas.value = Resource.Success(response.body()!!)
                } else _zonas.value = Resource.Error("Error al cargar zonas")
            } catch (e: Exception) {
                _zonas.value = Resource.Error("Error de red: ${e.localizedMessage}")
            }
        }
    }

    fun crearReserva(fecha: String, hora: String, zonaComunId: Long, apartamentoId: Long) {
        _reservaState.value = Resource.Loading
        viewModelScope.launch {
            try {
                val reserva = ReservaDTO(null, fecha, hora, "PENDIENTE", zonaComunId, apartamentoId)
                val response = RetrofitClient.instance.createReserva(reserva)
                if (response.isSuccessful && response.body() != null) {
                    _reservaState.value = Resource.Success(response.body())
                } else {
                    _reservaState.value = Resource.Error("Error al crear reserva")
                }
            } catch (e: Exception) {
                _reservaState.value = Resource.Error("Error de red: ${e.localizedMessage}")
            }
        }
    }
}