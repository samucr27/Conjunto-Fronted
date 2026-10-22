package com.example.conjuntoresidencial.ui.comun

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.model.NotificacionDTO
import com.example.conjuntoresidencial.util.Evento
import com.example.conjuntoresidencial.util.Resource
import kotlinx.coroutines.launch

/**
 * Notificaciones del administrador, compartidas por toda la actividad (activityViewModels):
 * la barra inferior muestra la insignia, el panel el resumen y la pestaña la lista.
 */
class NotificacionesViewModel : ViewModel() {

    private val _noLeidas = MutableLiveData(0)
    val noLeidas: LiveData<Int> get() = _noLeidas

    private val _lista = MutableLiveData<Resource<List<NotificacionDTO>>>()
    val lista: LiveData<Resource<List<NotificacionDTO>>> get() = _lista

    private val _mensaje = MutableLiveData<Evento<String>>()
    val mensaje: LiveData<Evento<String>> get() = _mensaje

    /** Solo el número: no hace falta descargar la lista cada vez que se cambia de pantalla. */
    fun actualizarConteo() {
        viewModelScope.launch {
            _noLeidas.value = try {
                val response = RetrofitClient.instance.getNotificacionesNoLeidas()
                if (response.isSuccessful) response.body()?.total?.toInt() ?: 0 else 0
            } catch (_: Exception) {
                0 // sin conexión: no se muestra el contador
            }
        }
    }

    /**
     * Carga la lista. Verla cuenta como leerla: se marcan como leídas en el servidor, pero
     * en pantalla siguen resaltadas las que eran nuevas hasta la próxima carga.
     */
    fun cargar(errorPorDefecto: String) {
        _lista.value = Resource.Loading
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getNotificaciones()
                if (!response.isSuccessful) {
                    _lista.value = Resource.Error(RetrofitClient.mensajeDeError(response, errorPorDefecto))
                    return@launch
                }
                val notificaciones = response.body().orEmpty()
                _lista.value = Resource.Success(notificaciones)
                if (notificaciones.any { !it.leida }) {
                    // Si falla no se avisa: el contador sigue mostrando las no leídas
                    try { RetrofitClient.instance.marcarNotificacionesLeidas() } catch (_: Exception) {}
                }
                actualizarConteo()
            } catch (e: Exception) {
                _lista.value = Resource.Error(RetrofitClient.mensajeDeExcepcion(e))
            }
        }
    }

    fun limpiar(errorPorDefecto: String) {
        viewModelScope.launch {
            val ok = try { RetrofitClient.instance.eliminarNotificaciones().isSuccessful } catch (_: Exception) { false }
            if (ok) {
                _lista.value = Resource.Success(emptyList())
                _noLeidas.value = 0
            } else {
                _mensaje.value = Evento(errorPorDefecto)
            }
        }
    }
}
