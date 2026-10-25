package com.example.conjuntoresidencial.ui.cartera

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.model.CuotaDTO
import com.example.conjuntoresidencial.util.Evento
import com.example.conjuntoresidencial.util.Resource
import kotlinx.coroutines.launch
import retrofit2.Response

/** Facturación del contador: cuotas por revisar o de un mes, y las acciones sobre ellas. */
class CuotasViewModel : ViewModel() {
    private val _cuotas = MutableLiveData<Resource<List<CuotaDTO>>>()
    val cuotas: LiveData<Resource<List<CuotaDTO>>> get() = _cuotas

    private val _mensaje = MutableLiveData<Evento<String>>()
    val mensaje: LiveData<Evento<String>> get() = _mensaje

    /** null = por revisar (todas las que tienen comprobante en revisión); si no, el mes. */
    var periodo: String? = null
        private set

    fun filtrar(periodo: String?) {
        this.periodo = periodo
        recargar()
    }

    fun recargar() {
        _cuotas.value = Resource.Loading
        viewModelScope.launch {
            try {
                val r = RetrofitClient.instance.getCuotas(periodo, if (periodo == null) Cuotas.EN_REVISION else null)
                _cuotas.value = if (r.isSuccessful) Resource.Success(r.body().orEmpty())
                else Resource.Error(RetrofitClient.mensajeDeError(r, "No se pudieron cargar las cuotas"))
            } catch (e: Exception) {
                _cuotas.value = Resource.Error(RetrofitClient.mensajeDeExcepcion(e))
            }
        }
    }

    /** Generar, confirmar o rechazar: [exito] recibe la respuesta para armar el aviso. */
    fun <T> ejecutar(error: String, peticion: suspend () -> Response<T>, exito: (T?) -> String) {
        viewModelScope.launch {
            try {
                val r = peticion()
                if (r.isSuccessful) {
                    _mensaje.value = Evento(exito(r.body()))
                    recargar()
                } else {
                    _mensaje.value = Evento(RetrofitClient.mensajeDeError(r, error))
                }
            } catch (e: Exception) {
                _mensaje.value = Evento(RetrofitClient.mensajeDeExcepcion(e))
            }
        }
    }
}
