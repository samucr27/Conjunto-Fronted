package com.example.conjuntoresidencial.ui.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.model.Apartamento
import com.example.conjuntoresidencial.model.PagoDTO
import com.example.conjuntoresidencial.util.Resource
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class CarteraAdminViewModel : ViewModel() {

    private val _cartera = MutableLiveData<Resource<List<Pair<Apartamento, PagoDTO?>>>>()
    val cartera: LiveData<Resource<List<Pair<Apartamento, PagoDTO?>>>> get() = _cartera

    /**
     * Dos peticiones en paralelo (apartamentos y todos los pagos) que se unen por
     * apartamentoId. Antes era una petición por apartamento, una detrás de otra.
     */
    fun fetchCartera() {
        _cartera.value = Resource.Loading
        viewModelScope.launch {
            try {
                // coroutineScope: si una petición falla (sin red), el error llega al catch
                // en vez de cerrar la app (un async suelto cancelaría toda la corrutina)
                val (aptos, pagos) = coroutineScope {
                    val aptosAsync = async { RetrofitClient.instance.getApartamentos() }
                    val pagosAsync = async { RetrofitClient.instance.getPagos() }
                    aptosAsync.await() to pagosAsync.await()
                }

                if (aptos.isSuccessful && pagos.isSuccessful) {
                    val pagoPorApto = pagos.body().orEmpty().associateBy { it.apartamentoId }
                    _cartera.value = Resource.Success(
                        aptos.body().orEmpty().map { apto -> apto to pagoPorApto[apto.id] }
                    )
                } else {
                    val fallida = if (!aptos.isSuccessful) aptos else pagos
                    _cartera.value = Resource.Error(
                        RetrofitClient.mensajeDeError(fallida, "Error del servidor al obtener la cartera")
                    )
                }
            } catch (e: Exception) {
                _cartera.value = Resource.Error(RetrofitClient.mensajeDeExcepcion(e))
            }
        }
    }
}
