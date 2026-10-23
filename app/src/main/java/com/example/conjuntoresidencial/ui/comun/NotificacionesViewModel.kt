package com.example.conjuntoresidencial.ui.comun

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.model.NotificacionDTO
import com.example.conjuntoresidencial.util.Evento
import com.example.conjuntoresidencial.util.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Notificaciones del administrador, compartidas por toda la actividad (activityViewModels):
 * la barra inferior muestra la insignia, el panel el resumen y la pestaña la lista.
 *
 * Verlas en la lista no las marca como leídas: cada una se marca al abrirla (o todas desde
 * el menú), así se distinguen las nuevas de las ya vistas.
 */
class NotificacionesViewModel : ViewModel() {

    private val _noLeidas = MutableLiveData(0)
    val noLeidas: LiveData<Int> get() = _noLeidas

    private val _lista = MutableLiveData<Resource<List<NotificacionDTO>>>()
    val lista: LiveData<Resource<List<NotificacionDTO>>> get() = _lista

    private val _mensaje = MutableLiveData<Evento<String>>()
    val mensaje: LiveData<Evento<String>> get() = _mensaje

    // Borrados que aún se pueden deshacer: id → envío al servidor programado
    private val borradosPendientes = mutableMapOf<Long, Job>()

    private val actuales: List<NotificacionDTO>
        get() = (_lista.value as? Resource.Success)?.data.orEmpty()

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

    fun cargar(errorPorDefecto: String) {
        _lista.value = Resource.Loading
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getNotificaciones()
                if (!response.isSuccessful) {
                    _lista.value = Resource.Error(RetrofitClient.mensajeDeError(response, errorPorDefecto))
                    return@launch
                }
                // Las que se están borrando (con "Deshacer" en pantalla) no vuelven a aparecer
                val notificaciones = response.body().orEmpty().filter { it.id !in borradosPendientes }
                _lista.value = Resource.Success(notificaciones)
                _noLeidas.value = notificaciones.count { !it.leida }
            } catch (e: Exception) {
                _lista.value = Resource.Error(RetrofitClient.mensajeDeExcepcion(e))
            }
        }
    }

    /**
     * Al abrir una notificación (en la lista o desde el aviso del celular): se marca leída al
     * instante en pantalla y luego en el servidor. Si falla, la próxima carga la muestra como nueva.
     */
    fun marcarLeida(id: Long) {
        val lista = actuales
        if (lista.any { it.id == id && !it.leida }) {
            _lista.value = Resource.Success(lista.map { if (it.id == id) it.copy(leida = true) else it })
            _noLeidas.value = ((_noLeidas.value ?: 0) - 1).coerceAtLeast(0)
        }
        viewModelScope.launch {
            try { RetrofitClient.instance.marcarNotificacionLeida(id) } catch (_: Exception) {}
            actualizarConteo()
        }
    }

    fun marcarTodasLeidas(errorPorDefecto: String) {
        viewModelScope.launch {
            val ok = try { RetrofitClient.instance.marcarNotificacionesLeidas().isSuccessful } catch (_: Exception) { false }
            if (ok) {
                _lista.value = Resource.Success(actuales.map { it.copy(leida = true) })
                _noLeidas.value = 0
            } else {
                _mensaje.value = Evento(errorPorDefecto)
            }
        }
    }

    /**
     * Quita la notificación de la lista; se borra del servidor cuando se cierra el "Deshacer"
     * ([confirmarEliminar]) o, si eso no llega a pasar (se cerró la pantalla), tras
     * [ESPERA_MAXIMA_MS]. Corre en el ViewModel de la actividad: sigue aunque se cambie de pestaña.
     */
    fun eliminar(n: NotificacionDTO, errorPorDefecto: String) {
        val lista = actuales
        _lista.value = Resource.Success(lista.filter { it.id != n.id })
        if (!n.leida) _noLeidas.value = ((_noLeidas.value ?: 0) - 1).coerceAtLeast(0)
        borradosPendientes[n.id]?.cancel()
        borradosPendientes[n.id] = viewModelScope.launch {
            delay(ESPERA_MAXIMA_MS)
            borrarEnServidor(n, errorPorDefecto)
        }
    }

    /** Se cerró el "Deshacer" sin usarlo: se borra ya. */
    fun confirmarEliminar(n: NotificacionDTO, errorPorDefecto: String) {
        borradosPendientes[n.id]?.cancel() ?: return
        borradosPendientes[n.id] = viewModelScope.launch { borrarEnServidor(n, errorPorDefecto) }
    }

    fun deshacerEliminar(n: NotificacionDTO) {
        borradosPendientes.remove(n.id)?.cancel() ?: return
        restaurarEnLista(n)
    }

    private suspend fun borrarEnServidor(n: NotificacionDTO, errorPorDefecto: String) {
        val ok = try { RetrofitClient.instance.eliminarNotificacion(n.id).isSuccessful } catch (_: Exception) { false }
        borradosPendientes.remove(n.id)
        if (!ok) {
            _mensaje.value = Evento(errorPorDefecto)
            restaurarEnLista(n)
        }
    }

    // Vuelve a su lugar: la lista va de la más nueva a la más antigua
    private fun restaurarEnLista(n: NotificacionDTO) {
        val lista = actuales
        if (lista.any { it.id == n.id }) return
        _lista.value = Resource.Success((lista + n).sortedWith(compareByDescending<NotificacionDTO> { it.fecha }.thenByDescending { it.id }))
        if (!n.leida) _noLeidas.value = (_noLeidas.value ?: 0) + 1
    }

    fun limpiar(errorPorDefecto: String) {
        viewModelScope.launch {
            val ok = try { RetrofitClient.instance.eliminarNotificaciones().isSuccessful } catch (_: Exception) { false }
            if (ok) {
                borradosPendientes.values.forEach { it.cancel() }
                borradosPendientes.clear()
                _lista.value = Resource.Success(emptyList())
                _noLeidas.value = 0
            } else {
                _mensaje.value = Evento(errorPorDefecto)
            }
        }
    }

    companion object {
        /** Respaldo por si el "Deshacer" nunca se cierra (la pantalla se destruyó antes). */
        const val ESPERA_MAXIMA_MS = 15_000L
    }
}
