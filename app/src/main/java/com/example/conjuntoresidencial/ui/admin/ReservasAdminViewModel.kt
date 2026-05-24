package com.example.conjuntoresidencial.ui.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.model.EstadoRequest
import com.example.conjuntoresidencial.model.ReservaDTO
import com.example.conjuntoresidencial.util.Resource
import kotlinx.coroutines.launch

class ReservasAdminViewModel : ViewModel() {

    private val _reservas = MutableLiveData<Resource<List<ReservaDTO>>>()
    val reservas: LiveData<Resource<List<ReservaDTO>>> get() = _reservas

    fun fetchReservas() {
        _reservas.value = Resource.Loading
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getReservas()
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
                if (response.isSuccessful) fetchReservas()
            } catch (_: Exception) {}
        }
    }
}