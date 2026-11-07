package com.example.conjuntoresidencial.ui.personal

import android.os.Bundle
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.api.instance
import com.example.conjuntoresidencial.model.TrabajadorDTO
import com.example.conjuntoresidencial.model.TurnoDTO
import com.example.conjuntoresidencial.ui.comun.Bloques
import com.example.conjuntoresidencial.ui.comun.confirmar
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

/**
 * P11 bloque 8: los turnos de una semana, día por día, con las horas de cada trabajador. Se pasa de
 * semana con las flechas, se agrega un turno (y se repite los días siguientes), se edita o se
 * borra tocándolo, y se copia la semana anterior completa.
 */
class TurnosFragment : PantallaHoja() {
    override val titulo = R.string.pers_turnos
    override val descripcion = R.string.pers_turnos_sub

    private var lunes: LocalDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    private var turnos: List<TurnoDTO> = emptyList()
    private var trabajadores: List<TrabajadorDTO> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.getString(LUNES)?.let { lunes = LocalDate.parse(it) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(LUNES, lunes.toString())
    }

    override suspend fun traer(): String? {
        val r = RetrofitClient.instance.getTurnos(lunes.toString(), lunes.plusDays(6).toString())
        turnos = r.body().orEmpty()
        trabajadores = RetrofitClient.instance.getTrabajadores().body().orEmpty()
        return mensajeDe(r)
    }

    private fun horas(t: TurnoDTO): Double = runCatching {
        Duration.between(LocalTime.parse(t.horaInicio), LocalTime.parse(t.horaFin)).toMinutes() / 60.0
    }.getOrDefault(0.0)

    override fun pintar() {
        val total = turnos.sumOf { horas(it) }
        binding.encabezado.ponerInfo(resources.getQuantityString(R.plurals.pers_turnos_semana, turnos.size, turnos.size) +
            " · " + getString(R.string.pers_horas, formato(total)))
        Personal.selector(hoja, getString(R.string.pers_semana_de, Personal.dia(lunes)),
            getString(R.string.pers_semana_anterior), getString(R.string.pers_semana_siguiente)) { paso ->
            lunes = lunes.plusWeeks(paso.toLong()); recargar()
        }
        Bloques.boton(hoja, getString(R.string.pers_nuevo_turno), icono = R.drawable.ic_add) {
            Personal.formularioTurno(this, trabajadores, null, lunes, null) { recargar() }
        }
        Bloques.boton(hoja, getString(R.string.pers_copiar_semana), relleno = false) {
            confirmar(getString(R.string.pers_copiar_semana), getString(R.string.pers_copiar_semana_mensaje), R.string.pers_copiar) {
                Personal.accion(this, R.string.pers_copiada, { RetrofitClient.instance.copiarSemana(lunes.toString()) }) { recargar() }
            }
        }
        for (i in 0..6) {
            val dia = lunes.plusDays(i.toLong())
            val delDia = turnos.filter { it.fecha == dia.toString() }.sortedBy { it.horaInicio }
            Bloques.seccion(hoja, Personal.dia(dia) + if (dia == LocalDate.now()) " · " + getString(R.string.pers_hoy) else "")
            if (delDia.isEmpty()) Bloques.parrafo(hoja, getString(R.string.pers_sin_turnos_dia))
            delDia.forEach { t ->
                Bloques.tarjeta(hoja, getString(R.string.pers_horario, t.horaInicio?.take(5), t.horaFin?.take(5)) + " · " + t.trabajador.orEmpty(),
                    detalle = Personal.cargo(this, t.cargo) + " · " + getString(R.string.pers_horas, formato(horas(t)))) { opciones(t) }
            }
        }
        // Horas de cada uno en la semana
        if (turnos.isNotEmpty()) {
            Bloques.seccion(hoja, getString(R.string.pers_horas_por_trabajador))
            val c = Bloques.tarjeta(hoja, getString(R.string.pers_esta_semana))
            turnos.groupBy { it.trabajador.orEmpty() }.toSortedMap().forEach { (nombre, suyos) ->
                Bloques.dato(c, nombre, getString(R.string.pers_horas, formato(suyos.sumOf { horas(it) })))
            }
        }
    }

    private fun opciones(t: TurnoDTO) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(t.trabajador)
            .setItems(arrayOf(getString(R.string.pers_editar_turno), getString(R.string.pers_borrar_turno))) { _, i ->
                if (i == 0) Personal.formularioTurno(this, trabajadores, t, null, null) { recargar() }
                else Personal.accion(this, R.string.pers_borrado, { RetrofitClient.instance.eliminarTurno(t.id!!) }) { recargar() }
            }.show()
    }

    private fun formato(h: Double) = if (h % 1.0 == 0.0) h.toInt().toString() else String.format(java.util.Locale.US, "%.1f", h)

    companion object { private const val LUNES = "lunes" }
}
