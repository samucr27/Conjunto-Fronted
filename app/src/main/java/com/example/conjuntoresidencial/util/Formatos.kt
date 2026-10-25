package com.example.conjuntoresidencial.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Formatos colombianos para mostrar datos del backend.
 * Sin java.time a propósito: la app soporta Android 7 (minSdk 24).
 * Si el valor no tiene el formato esperado se devuelve tal cual.
 */
object Formatos {

    private val colombia = Locale("es", "CO")

    /** "2026-10-23" -> "23/10/2026" */
    fun fecha(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        return try {
            val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(iso.take(10))
            SimpleDateFormat("dd/MM/yyyy", colombia).format(d!!)
        } catch (_: Exception) {
            iso
        }
    }

    /** "14:00" o "14:00:00" -> "2:00 p. m." */
    fun hora(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        return try {
            val d = SimpleDateFormat("HH:mm", Locale.US).apply { isLenient = false }.parse(iso.take(5))
            SimpleDateFormat("h:mm a", colombia).format(d!!)
                .replace("AM", "a. m.").replace("PM", "p. m.") // algunos dispositivos usan AM/PM
                .replace(' ', ' ').replace(' ', ' ')
        } catch (_: Exception) {
            iso
        }
    }

    /** "2026-10-23" + "14:00:00" -> "23/10/2026 · 2:00 p. m." */
    fun fechaHora(fecha: String?, hora: String?): String =
        listOf(fecha(fecha), hora(hora)).filter { it.isNotBlank() }.joinToString(" · ")

    /** "2026-10-25" + "14:00" + "17:00" -> "25/10/2026 · 2:00 p. m. a 5:00 p. m." (sin fin, solo el inicio) */
    fun horario(fecha: String?, inicio: String?, fin: String?): String {
        val horas = if (fin.isNullOrBlank()) hora(inicio) else "${hora(inicio)} a ${hora(fin)}"
        return listOf(fecha(fecha), horas).filter { it.isNotBlank() }.joinToString(" · ")
    }

    /** Rango de fechas de una obra: "16/10/2026 → 31/10/2026" */
    fun rango(desde: String?, hasta: String?): String = "${fecha(desde)} → ${fecha(hasta)}"

    /** 350000.0 -> "$350.000" (pesos colombianos, sin centavos) */
    fun moneda(valor: Double): String {
        val f = NumberFormat.getNumberInstance(colombia).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }
        return "$" + f.format(valor)
    }
}
