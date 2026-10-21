package com.example.conjuntoresidencial.ui.propietario

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.model.ReservaDTO
import com.example.conjuntoresidencial.model.ZonaComunDTO
import com.example.conjuntoresidencial.util.Evento
import com.example.conjuntoresidencial.util.Resource
import kotlinx.coroutines.launch

class ZonasPropietarioViewModel : ViewModel() {

    private val _zonas = MutableLiveData<Resource<List<ZonaComunDTO>>>()
    val zonas: LiveData<Resource<List<ZonaComunDTO>>> get() = _zonas

    // Resultado de crear una reserva: null si salió bien, o el mensaje de error
    private val _reserva = MutableLiveData<Evento<String?>>()
    val reserva: LiveData<Evento<String?>> get() = _reserva

    fun fetchZonas() {
        _zonas.value = Resource.Loading
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getZonasComunes()
                if (response.isSuccessful && response.body() != null) {
                    _zonas.value = Resource.Success(response.body()!!)
                } else _zonas.value = Resource.Error("Error al cargar zonas")
            } catch (e: Exception) {
                _zonas.value = Resource.Error(RetrofitClient.mensajeDeExcepcion(e))
            }
        }
    }

    fun crearReserva(fecha: String, hora: String, zonaComunId: Long, apartamentoId: Long, errorPorDefecto: String) {
        viewModelScope.launch {
            try {
                val reserva = ReservaDTO(null, fecha, hora, "PENDIENTE", zonaComunId, apartamentoId)
                val response = RetrofitClient.instance.createReserva(reserva)
                // El backend crea la notificación para el admin.
                // Si falla: 409 choque de horario, 400 fecha pasada o zona no reservable; el backend explica cuál
                _reserva.value = Evento(
                    if (response.isSuccessful) null else RetrofitClient.mensajeDeError(response, errorPorDefecto)
                )
            } catch (e: Exception) {
                _reserva.value = Evento(RetrofitClient.mensajeDeExcepcion(e))
            }
        }
    }
}
