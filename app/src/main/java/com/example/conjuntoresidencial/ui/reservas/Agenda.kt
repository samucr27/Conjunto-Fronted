package com.example.conjuntoresidencial.ui.reservas

/**
 * Cálculos del calendario de reservas, sin Android (se prueban en AgendaTest): las franjas de
 * una hora de un día, si cada una está libre u ocupada, el estado del día y el rango elegido.
 * Las horas van en minutos desde la medianoche; sin java.time (la app soporta Android 7).
 */
object Agenda {

    enum class EstadoFranja { LIBRE, OCUPADA, PROPIA, PASADA }

    /** PARCIAL: hay franjas libres y ocupadas; LLENO: no queda ninguna libre. */
    enum class EstadoDia { PASADO, LIBRE, PARCIAL, LLENO }

    /** Una reserva existente de ese día, [inicio, fin) en minutos. */
    data class Ocupada(val inicio: Int, val fin: Int, val propia: Boolean)

    /** Una franja que se puede elegir, [inicio, fin) en minutos. */
    data class Franja(val inicio: Int, val fin: Int, val estado: EstadoFranja) {
        val libre get() = estado == EstadoFranja.LIBRE
    }

    const val DIA_COMPLETO = 24 * 60
    private const val DURACION_FRANJA = 60

    /** "14:00" o "14:00:00" → 840; "23:59:59" (fin de un día que sigue al siguiente) → 1440. */
    fun minutos(hora: String?): Int? {
        if (hora.isNullOrBlank()) return null
        val partes = hora.split(":")
        val h = partes.getOrNull(0)?.toIntOrNull() ?: return null
        val m = partes.getOrNull(1)?.toIntOrNull() ?: return null
        return if (h == 23 && m == 59) DIA_COMPLETO else h * 60 + m
    }

    /** 840 → "14:00" (lo que espera el backend). */
    fun hora(minutos: Int): String = String.format(java.util.Locale.US, "%02d:%02d", minutos / 60, minutos % 60)

    /**
     * Franjas de una hora entre la apertura y el cierre (la última puede ser más corta si cierra
     * a la media hora). [ahora] en minutos solo para el día de hoy: lo que ya empezó no se puede.
     */
    fun franjas(apertura: Int?, cierre: Int?, ocupadas: List<Ocupada>, ahora: Int?): List<Franja> {
        val desde = apertura ?: 0
        val hasta = cierre ?: DIA_COMPLETO
        val lista = mutableListOf<Franja>()
        var inicio = desde
        while (inicio < hasta) {
            val fin = minOf(inicio + DURACION_FRANJA, hasta)
            val cruce = ocupadas.filter { it.inicio < fin && inicio < it.fin }
            val estado = when {
                cruce.any { it.propia } -> EstadoFranja.PROPIA
                cruce.isNotEmpty() -> EstadoFranja.OCUPADA
                ahora != null && inicio <= ahora -> EstadoFranja.PASADA
                else -> EstadoFranja.LIBRE
            }
            lista += Franja(inicio, fin, estado)
            inicio = fin
        }
        return lista
    }

    /** El color del día en el calendario. [pasado]: el día ya terminó. */
    fun estadoDia(franjas: List<Franja>, pasado: Boolean): EstadoDia {
        if (pasado) return EstadoDia.PASADO
        val futuras = franjas.filter { it.estado != EstadoFranja.PASADA }
        return when {
            futuras.isEmpty() -> EstadoDia.PASADO          // hoy, y ya no queda horario
            futuras.all { it.libre } -> EstadoDia.LIBRE
            futuras.none { it.libre } -> EstadoDia.LLENO
            else -> EstadoDia.PARCIAL
        }
    }

    /**
     * Al tocar la franja [tocada]: si no hay nada elegido, o se toca antes del inicio, o el rango
     * pasaría por una franja ocupada, empieza una selección nueva; si no, la extiende hasta ahí.
     * Devuelve el rango de índices elegido, o null si la franja tocada no está libre.
     */
    fun elegir(franjas: List<Franja>, actual: IntRange?, tocada: Int, maximoFranjas: Int): IntRange? {
        if (franjas.getOrNull(tocada)?.libre != true) return actual
        if (actual == null || tocada < actual.first) return tocada..tocada
        // Tocar la única elegida la quita
        if (actual.first == actual.last && tocada == actual.first) return null
        val rango = actual.first..tocada
        val todasLibres = rango.all { franjas[it].libre }
        return if (todasLibres && rango.count() <= maximoFranjas) rango else tocada..tocada
    }
}
