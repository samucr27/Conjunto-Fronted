package com.example.conjuntoresidencial.ui.propietario

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.model.ReservaDTO
import com.example.conjuntoresidencial.util.Resource
import com.example.conjuntoresidencial.util.nombresDeZonas
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class MisReservasViewModel : ViewModel() {
    private val _reservas = MutableLiveData<Resource<List<ReservaDTO>>>()
    val reservas: LiveData<Resource<List<ReservaDTO>>> get() = _reservas

    // zonaId -> nombre; se carga junto con las reservas (una sola petición para todas las zonas)
    var nombresZonas: Map<Long, String> = emptyMap()
        private set

    fun fetchReservas(aptoId: Long) {
        _reservas.value = Resource.Loading
        viewModelScope.launch {
            try {
                // coroutineScope: si una petición falla, el error llega al catch en vez de cerrar la app
                val response = coroutineScope {
                    val zonasAsync = async { RetrofitClient.instance.getZonasComunes() }
                    val reservas = RetrofitClient.instance.getReservasPorApartamento(aptoId)
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
}
