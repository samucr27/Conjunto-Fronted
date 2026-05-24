package com.example.conjuntoresidencial.ui.propietario

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.model.PagoDTO
import com.example.conjuntoresidencial.util.Resource
import kotlinx.coroutines.launch

class MiCarteraViewModel : ViewModel() {

    private val _pagoState = MutableLiveData<Resource<PagoDTO>>()
    val pagoState: LiveData<Resource<PagoDTO>> get() = _pagoState

    private val _simulacionState = MutableLiveData<Resource<Unit>>()
    val simulacionState: LiveData<Resource<Unit>> get() = _simulacionState

    fun fetchPagos(aptoId: Long) {
        _pagoState.value = Resource.Loading
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getPagosPorApartamento(aptoId)
                if (response.isSuccessful && response.body() != null) {
                    _pagoState.value = Resource.Success(response.body()!!)
                } else {
                    _pagoState.value = Resource.Error("Error al consultar cuenta de cobro")
                }
            } catch (e: Exception) {
                _pagoState.value = Resource.Error("Error de Red: ${e.localizedMessage}")
            }
        }
    }

    fun simularPago(aptoId: Long) {
        _simulacionState.value = Resource.Loading
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.simularPagoApartamento(aptoId)
                if (response.isSuccessful) {
                    _simulacionState.value = Resource.Success(Unit)
                } else {
                    _simulacionState.value = Resource.Error("Fallo al procesar simulación")
                }
            } catch (_: Exception) {
                _simulacionState.value = Resource.Error("Fallo de red del servidor")
            }
        }
    }
}