package com.example.conjuntoresidencial

import com.example.conjuntoresidencial.ui.reservas.Agenda
import com.example.conjuntoresidencial.ui.reservas.Agenda.EstadoDia
import com.example.conjuntoresidencial.ui.reservas.Agenda.EstadoFranja
import com.example.conjuntoresidencial.ui.reservas.Agenda.Ocupada
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** P4 bloque 5: franjas del calendario de reservas, estado del día y selección del horario. */
class AgendaTest {

    private val nueve = 9 * 60
    private val doce = 12 * 60
    private val dieciocho = 18 * 60

    @Test
    fun horasEnMinutos() {
        assertEquals(840, Agenda.minutos("14:00"))
        assertEquals(870, Agenda.minutos("14:30:00"))
        // El fin de una reserva que sigue al día siguiente llega como 23:59:59
        assertEquals(Agenda.DIA_COMPLETO, Agenda.minutos("23:59:59"))
        assertNull(Agenda.minutos(null))
        assertEquals("09:05", Agenda.hora(545))
    }

    @Test
    fun franjasDeUnaHoraEntreApertureYCierre() {
        val franjas = Agenda.franjas(nueve, doce + 30, emptyList(), null)
        // 9-10, 10-11, 11-12 y la última, más corta: 12:00-12:30
        assertEquals(4, franjas.size)
        assertEquals(doce + 30, franjas.last().fin)
        assertEquals(true, franjas.all { it.libre })
        // Sin horario: todo el día
        assertEquals(24, Agenda.franjas(null, null, emptyList(), null).size)
    }

    @Test
    fun marcaLoOcupadoLoPropioYLoQueYaPaso() {
        val ocupadas = listOf(Ocupada(10 * 60, 11 * 60 + 30, propia = false), Ocupada(15 * 60, 16 * 60, propia = true))
        val franjas = Agenda.franjas(nueve, dieciocho, ocupadas, ahora = nueve + 10)
        assertEquals(EstadoFranja.PASADA, franjas[0].estado)   // 9-10 ya empezó
        assertEquals(EstadoFranja.OCUPADA, franjas[1].estado)  // 10-11
        assertEquals(EstadoFranja.OCUPADA, franjas[2].estado)  // 11-12 (la reserva sigue hasta 11:30)
        assertEquals(EstadoFranja.LIBRE, franjas[3].estado)    // 12-13
        assertEquals(EstadoFranja.PROPIA, franjas[6].estado)   // 15-16
    }

    @Test
    fun estadoDelDiaParaElCalendario() {
        val libre = Agenda.franjas(nueve, doce, emptyList(), null)
        assertEquals(EstadoDia.LIBRE, Agenda.estadoDia(libre, pasado = false))
        val parcial = Agenda.franjas(nueve, doce, listOf(Ocupada(nueve, 10 * 60, false)), null)
        assertEquals(EstadoDia.PARCIAL, Agenda.estadoDia(parcial, pasado = false))
        val lleno = Agenda.franjas(nueve, doce, listOf(Ocupada(nueve, doce, false)), null)
        assertEquals(EstadoDia.LLENO, Agenda.estadoDia(lleno, pasado = false))
        assertEquals(EstadoDia.PASADO, Agenda.estadoDia(libre, pasado = true))
        // Hoy, cuando ya pasó todo el horario
        val tarde = Agenda.franjas(nueve, doce, emptyList(), ahora = doce)
        assertEquals(EstadoDia.PASADO, Agenda.estadoDia(tarde, pasado = false))
    }

    @Test
    fun elegirUnRangoDeFranjasLibres() {
        // 9-10 libre, 10-11 libre, 11-12 ocupada, 12-13 libre, 13-14 libre
        val franjas = Agenda.franjas(nueve, 14 * 60, listOf(Ocupada(11 * 60, doce, false)), null)
        var rango = Agenda.elegir(franjas, null, 0, maximoFranjas = 8)
        assertEquals(0..0, rango)
        rango = Agenda.elegir(franjas, rango, 1, 8)
        assertEquals(0..1, rango)
        // No se puede extender pasando por la ocupada: empieza de nuevo en la tocada
        assertEquals(4..4, Agenda.elegir(franjas, rango, 4, 8))
        // Tocar una ocupada no cambia nada
        assertEquals(0..1, Agenda.elegir(franjas, rango, 2, 8))
        // Tocar antes del inicio empieza de nuevo
        assertEquals(0..0, Agenda.elegir(franjas, 1..1, 0, 8))
        // Tocar la única elegida la quita
        assertNull(Agenda.elegir(franjas, 3..3, 3, 8))
        // Más largo que el máximo: empieza de nuevo
        assertEquals(4..4, Agenda.elegir(franjas, 3..3, 4, maximoFranjas = 1))
    }
}
