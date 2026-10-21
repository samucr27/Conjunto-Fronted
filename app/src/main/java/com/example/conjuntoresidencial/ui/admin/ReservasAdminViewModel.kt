package com.example.conjuntoresidencial.ui.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.model.EstadoRequest
import com.example.conjuntoresidencial.model.ReservaDTO
import com.example.conjuntoresidencial.util.Evento
import com.example.conjuntoresidencial.util.Resource
import com.example.conjuntoresidencial.util.nombresDeZonas
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class ReservasAdminViewModel : ViewModel() {

    private val _reservas = MutableLiveData<Resource<List<ReservaDTO>>>()
    val reservas: LiveData<Resource<List<ReservaDTO>>> get() = _reservas

    // Mensajes de una sola vez (por ejemplo, "no se pudo aprobar: choca con otra reserva")
    private val _mensaje = MutableLiveData<Evento<String>>()
    val mensaje: LiveData<Evento<String>> get() = _mensaje

    // zonaId -> nombre; se carga junto con las reservas (una sola petición para todas las zonas)
    var nombresZonas: Map<Long, String> = emptyMap()
        private set

    fun fetchReservas() {
        _reservas.value = Resource.Loading
        viewModelScope.launch {
            try {
                // coroutineScope: si una petición falla, el error llega al catch en vez de cerrar la app
                val response = coroutineScope {
                    val zonasAsync = async { RetrofitClient.instance.getZonasComunes() }
                    val reservas = RetrofitClient.instance.getReservas()
                    nombresZonas = nombresDeZonas(zonasAsync.await())
                    reservas
                }
                if (response.isSuccessful && response.body() != null)
                    _reservas.value = Resource.Success(response.body()!!)
                else _reservas.value = Resource.Error("Error al cargar reservas")
            } catch (e: Exception) {
                _reservas.value = Resource.Error("Error de red: ${e.localizedMessage}")
            }
        }
    }

    fun actualizarEstado(reservaId: Long, estado: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.updateEstadoReserva(reservaId, EstadoRequest(estado))
                if (response.isSuccessful) {
                    fetchReservas()
                } else {
                    // 409 si al aprobar choca con otra reserva ya aprobada
                    _mensaje.value = Evento(
                        RetrofitClient.mensajeDeError(response, "No se pudo actualizar la reserva")
                    )
                }
            } catch (e: Exception) {
                _mensaje.value = Evento("Error de red: ${e.localizedMessage}")
            }
        }
    }
}
