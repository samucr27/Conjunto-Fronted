package com.example.conjuntoresidencial.ui.comun

import android.content.Context
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.conjuntoresidencial.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * P12: cuánto tiempo lleva un visitante dentro del conjunto ("12 min", "1 h 05 min"). Las fechas
 * del servidor vienen sin zona y en la hora de Colombia. Sin java.time: la app soporta Android 7.
 */
object TiempoDentro {

    private val zona: TimeZone = TimeZone.getTimeZone("America/Bogota")

    private fun milis(iso: String?): Long? {
        if (iso.isNullOrBlank() || iso.length < 16) return null
        val formato = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).apply { timeZone = zona }
        return runCatching { formato.parse(iso.take(16))?.time }.getOrNull()
    }

    /** Minutos desde [iso] hasta ahora; null si no se entiende la fecha. */
    fun minutos(iso: String?): Long? {
        val desde = milis(iso) ?: return null
        return ((System.currentTimeMillis() - desde) / 60_000L).coerceAtLeast(0)
    }

    /** Si [iso] todavía no llega (por ejemplo, hasta cuándo sigue abierto un chat). */
    fun esFuturo(iso: String?): Boolean = (milis(iso) ?: 0L) > System.currentTimeMillis()

    fun texto(ctx: Context, iso: String?): String {
        val m = minutos(iso) ?: return ""
        return if (m < 60) ctx.getString(R.string.tiempo_dentro_min, m)
        else ctx.getString(R.string.tiempo_dentro_horas, m / 60, m % 60)
    }

    /**
     * Mantiene al día los textos con el tiempo: cada 30 segundos vuelve a escribir cada fila
     * registrada con [agregar] mientras la pantalla esté visible.
     */
    class Reloj(dueno: LifecycleOwner) {
        private val filas = mutableListOf<Pair<TextView, () -> String>>()

        init {
            dueno.lifecycleScope.launch {
                dueno.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    while (true) {
                        filas.forEach { (tv, texto) -> tv.text = texto() }
                        delay(30_000)
                    }
                }
            }
        }

        fun limpiar() = filas.clear()

        fun agregar(tv: TextView, texto: () -> String) {
            tv.text = texto()
            filas += tv to texto
        }
    }
}
