package com.example.conjuntoresidencial.ui.propietario

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.api.instance
import com.example.conjuntoresidencial.model.CuotaDTO
import com.example.conjuntoresidencial.model.PagoDTO
import okhttp3.MultipartBody
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

    // Cuotas del apartamento (P4); vacía si no se pudieron cargar: la cartera se muestra igual
    private val _cuotas = MutableLiveData<List<CuotaDTO>>()
    val cuotas: LiveData<List<CuotaDTO>> get() = _cuotas

    private val _comprobante = MutableLiveData<Evento<String>>()
    val comprobante: LiveData<Evento<String>> get() = _comprobante

    fun fetchPagos(aptoId: Long, errorPorDefecto: String) {
        _pagoState.value = Resource.Loading
        viewModelScope.launch {
            _cuotas.value = try {
                RetrofitClient.instance.getCuotasPorApartamento(aptoId).body().orEmpty()
            } catch (_: Exception) {
                emptyList()
            }
        }
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

    /** Sube la foto o el PDF del pago; queda en revisión hasta que el contador lo confirme. */
    fun subirComprobante(aptoId: Long, cuotaId: Long, archivo: MultipartBody.Part, exito: String, errorPorDefecto: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.subirComprobante(cuotaId, archivo)
                _comprobante.value = Evento(
                    if (response.isSuccessful) exito else RetrofitClient.mensajeDeError(response, errorPorDefecto)
                )
                if (response.isSuccessful) fetchPagos(aptoId, errorPorDefecto)
            } catch (e: Exception) {
                _comprobante.value = Evento(RetrofitClient.mensajeDeExcepcion(e))
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
