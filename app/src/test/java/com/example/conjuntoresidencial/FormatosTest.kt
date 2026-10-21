package com.example.conjuntoresidencial

import com.example.conjuntoresidencial.util.Formatos
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatosTest {

    @Test
    fun fechaIsoAFormatoColombiano() {
        assertEquals("23/10/2026", Formatos.fecha("2026-10-23"))
        assertEquals("", Formatos.fecha(null))
        // Si no es una fecha válida se muestra tal cual, sin romper la pantalla
        assertEquals("mañana", Formatos.fecha("mañana"))
    }

    @Test
    fun horaDe24hA12hConAmPm() {
        assertEquals("2:00 p. m.", Formatos.hora("14:00:00"))
        assertEquals("10:30 a. m.", Formatos.hora("10:30"))
        assertEquals("12:00 p. m.", Formatos.hora("12:00"))
    }

    @Test
    fun fechaHoraUnidas() {
        assertEquals("23/10/2026 · 2:00 p. m.", Formatos.fechaHora("2026-10-23", "14:00:00"))
        assertEquals("23/10/2026", Formatos.fechaHora("2026-10-23", null))
    }

    @Test
    fun monedaEnPesosSinCentavos() {
        assertEquals("$350.000", Formatos.moneda(350000.0))
        assertEquals("$1.250.500", Formatos.moneda(1250500.4))
        assertEquals("$0", Formatos.moneda(0.0))
    }

    @Test
    fun rangoDeFechas() {
        assertEquals("16/10/2026 → 31/10/2026", Formatos.rango("2026-10-16", "2026-10-31"))
    }
}
