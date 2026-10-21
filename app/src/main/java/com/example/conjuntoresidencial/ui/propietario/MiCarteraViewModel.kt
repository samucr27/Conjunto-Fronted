package com.example.conjuntoresidencial.ui.propietario

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.model.PagoDTO
import com.example.conjuntoresidencial.util.Evento
import com.example.conjuntoresidencial.util.Resource
import kotlinx.coroutines.launch

class MiCarteraViewModel : ViewModel() {

    // Success(null) = el apartamento todavía no tiene registro de cartera (el backend responde 404)
    private val _pagoState = MutableLiveData<Resource<PagoDTO?>>()
    val pagoState: LiveData<Resource<PagoDTO?>> get() = _pagoState

    // Resultado de simular el pago: null si salió bien, o el mensaje de error
    private val _simulacion = MutableLiveData<Evento<String?>>()
    val simulacion: LiveData<Evento<String?>> get() = _simulacion

    fun fetchPagos(aptoId: Long, errorPorDefecto: String) {
        _pagoState.value = Resource.Loading
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getPagosPorApartamento(aptoId)
                _pagoState.value = when {
                    response.isSuccessful -> Resource.Success(response.body())
                    response.code() == 404 -> Resource.Success(null)
                    else -> Resource.Error(RetrofitClient.mensajeDeError(response, errorPorDefecto))
                }
            } catch (e: Exception) {
                _pagoState.value = Resource.Error(RetrofitClient.mensajeDeExcepcion(e))
            }
        }
    }

    fun simularPago(aptoId: Long, errorPorDefecto: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.simularPagoApartamento(aptoId)
                if (response.isSuccessful) {
                    _simulacion.value = Evento(null)
                    fetchPagos(aptoId, errorPorDefecto)
                } else {
                    _simulacion.value = Evento(RetrofitClient.mensajeDeError(response, errorPorDefecto))
                }
            } catch (e: Exception) {
                _simulacion.value = Evento(RetrofitClient.mensajeDeExcepcion(e))
            }
        }
    }
}
