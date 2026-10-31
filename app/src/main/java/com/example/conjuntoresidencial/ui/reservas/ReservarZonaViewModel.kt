package com.example.conjuntoresidencial.ui.reservas

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.model.DisponibilidadDTO
import com.example.conjuntoresidencial.model.ReservaDTO
import com.example.conjuntoresidencial.util.Evento
import com.example.conjuntoresidencial.util.Resource
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

/**
 * Reservar una zona: la disponibilidad del mes que se ve en el calendario, el día y las
 * franjas elegidas (sobreviven al girar la pantalla) y el envío de la reserva.
 */
class ReservarZonaViewModel : ViewModel() {

    /** Mes que muestra el calendario (mes de Calendar: 0 = enero). */
    var anio: Int = Calendar.getInstance().get(Calendar.YEAR)
        private set
    var mes: Int = Calendar.getInstance().get(Calendar.MONTH)
        private set

    /** La reserva que se edita (P5); null al crear una nueva. */
    var reservaId: Long? = null

    /** Muestra un mes sin cargarlo (al abrir para editar). */
    fun irA(anio: Int, mes: Int) {
        this.anio = anio
        this.mes = mes
    }

    /** Día elegido ("2026-10-25") y franjas elegidas de ese día (índices). */
    var diaElegido: String? = null
    var seleccion: IntRange? = null

    private val _disponibilidad = MutableLiveData<Resource<DisponibilidadDTO>>()
    val disponibilidad: LiveData<Resource<DisponibilidadDTO>> get() = _disponibilidad

    // Resultado de reservar: null si salió bien, o el mensaje de error del backend
    private val _resultado = MutableLiveData<Evento<String?>>()
    val resultado: LiveData<Evento<String?>> get() = _resultado

    private val _enviando = MutableLiveData(false)
    val enviando: LiveData<Boolean> get() = _enviando

    /** Meses que se pueden ver desde el actual: este y los [MESES_ADELANTE] siguientes. */
    val puedeRetroceder: Boolean get() = (anio * 12 + mes) > mesActual()
    val puedeAvanzar: Boolean get() = (anio * 12 + mes) < mesActual() + MESES_ADELANTE

    private fun mesActual() = Calendar.getInstance().let { it.get(Calendar.YEAR) * 12 + it.get(Calendar.MONTH) }

    fun cargar(zonaId: Long, errorPorDefecto: String) {
        _disponibilidad.value = Resource.Loading
        val ultimoDia = Calendar.getInstance().apply { set(anio, mes, 1) }.getActualMaximum(Calendar.DAY_OF_MONTH)
        val desde = String.format(Locale.US, "%04d-%02d-01", anio, mes + 1)
        val hasta = String.format(Locale.US, "%04d-%02d-%02d", anio, mes + 1, ultimoDia)
        viewModelScope.launch {
            try {
                val response = RetrofitClient.comun.getDisponibilidad(zonaId, desde, hasta)
                val datos = response.body()
                _disponibilidad.value = if (response.isSuccessful && datos != null) Resource.Success(datos)
                    else Resource.Error(RetrofitClient.mensajeDeError(response, errorPorDefecto))
            } catch (e: Exception) {
                _disponibilidad.value = Resource.Error(RetrofitClient.mensajeDeExcepcion(e))
            }
        }
    }

    /** Cambia de mes: se olvida lo elegido (el día ya no se ve). */
    fun moverMes(delta: Int, zonaId: Long, errorPorDefecto: String) {
        val total = anio * 12 + mes + delta
        anio = total / 12
        mes = total % 12
        diaElegido = null
        seleccion = null
        cargar(zonaId, errorPorDefecto)
    }

    fun reservar(reserva: ReservaDTO, errorPorDefecto: String) {
        if (_enviando.value == true) return
        _enviando.value = true
        viewModelScope.launch {
            try {
                val id = reservaId
                val response = if (id == null) RetrofitClient.comun.createReserva(reserva)
                    else RetrofitClient.comun.updateReserva(id, reserva)
                // 409 si mientras tanto otro reservó esa franja; 400 si no cabe en el horario o el aforo
                _resultado.value = Evento(if (response.isSuccessful) null else RetrofitClient.mensajeDeError(response, errorPorDefecto))
            } catch (e: Exception) {
                _resultado.value = Evento(RetrofitClient.mensajeDeExcepcion(e))
            }
            _enviando.value = false
        }
    }

    private companion object {
        const val MESES_ADELANTE = 5
    }
}
