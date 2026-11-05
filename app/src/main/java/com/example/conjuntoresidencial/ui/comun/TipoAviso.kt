package com.example.conjuntoresidencial.ui.comun

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.conjuntoresidencial.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * P9 bloque 1: de qué es cada aviso, para que se lea igual en el celular y en la app: su
 * categoría (Reservas, Pagos, Portería…) con su ícono y su color, un título corto según el tipo
 * exacto y, si la respuesta fue un sí o un no, la etiqueta Aprobado o Rechazado.
 */
enum class CategoriaAviso(@StringRes val nombre: Int, @DrawableRes val icono: Int, @ColorRes val color: Int) {
    RESERVAS(R.string.aviso_cat_reservas, R.drawable.ic_event, R.color.aviso_reserva),
    VEHICULOS(R.string.aviso_cat_vehiculos, R.drawable.ic_car, R.color.aviso_vehiculo),
    RESIDENTES(R.string.aviso_cat_residentes, R.drawable.ic_people, R.color.aviso_residente),
    PAGOS(R.string.aviso_cat_pagos, R.drawable.ic_wallet, R.color.aviso_pago),
    PUBLICACIONES(R.string.aviso_cat_publicaciones, R.drawable.ic_campaign, R.color.aviso_publicacion),
    PORTERIA(R.string.aviso_cat_porteria, R.drawable.ic_security, R.color.aviso_porteria),
    CHAT(R.string.aviso_cat_chat, R.drawable.ic_chat, R.color.aviso_chat),
    OBRAS(R.string.aviso_cat_obras, R.drawable.ic_build, R.color.aviso_obra),
    CORRESPONDENCIA(R.string.aviso_cat_correspondencia, R.drawable.ic_package, R.color.aviso_correspondencia),
    GENERAL(R.string.aviso_cat_general, R.drawable.ic_notifications, R.color.aviso_general)
}

enum class EstadoAviso(@StringRes val nombre: Int, @ColorRes val color: Int) {
    APROBADO(R.string.aviso_estado_aprobado, R.color.aviso_aprobado),
    RECHAZADO(R.string.aviso_estado_rechazado, R.color.aviso_rechazado)
}

data class TipoAviso(val categoria: CategoriaAviso, @StringRes val titulo: Int, val estado: EstadoAviso?) {

    companion object {
        private val TITULOS = mapOf(
            "RESERVA" to R.string.aviso_reserva,
            "RESERVA_APROBADA" to R.string.aviso_reserva_aprobada,
            "RESERVA_RECHAZADA" to R.string.aviso_reserva_rechazada,
            "VEHICULO" to R.string.aviso_vehiculo,
            "VEHICULO_APROBADO" to R.string.aviso_vehiculo_aprobado,
            "VEHICULO_RECHAZADO" to R.string.aviso_vehiculo_rechazado,
            "RESIDENTE" to R.string.aviso_residente,
            "RESIDENTE_APROBADO" to R.string.aviso_residente_aprobado,
            "RESIDENTE_RECHAZADO" to R.string.aviso_residente_rechazado,
            "REGISTRO" to R.string.aviso_registro,
            "SOLICITUD_ARRENDATARIO" to R.string.aviso_solicitud_arrendatario,
            "CUOTA" to R.string.aviso_cuota,
            "CUOTA_PAGADA" to R.string.aviso_cuota_pagada,
            "CUOTA_RECHAZADA" to R.string.aviso_cuota_rechazada,
            "COMPROBANTE" to R.string.aviso_comprobante,
            "SOLICITUD_CONTABLE" to R.string.aviso_solicitud_contable,
            "PUBLICACION" to R.string.aviso_publicacion,
            "VISITA_PORTERIA" to R.string.aviso_visita,
            "CHAT" to R.string.aviso_chat,
            "OBRA" to R.string.aviso_obra,
            "OBRA_APROBADA" to R.string.aviso_obra_aprobada,
            "OBRA_RECHAZADA" to R.string.aviso_obra_rechazada,
            "PERSONAL_OBRA" to R.string.aviso_personal,
            "PERSONAL_OBRA_APROBADO" to R.string.aviso_personal_aprobado,
            "PERSONAL_OBRA_RECHAZADO" to R.string.aviso_personal_rechazado,
            "CORRESPONDENCIA" to R.string.aviso_correspondencia,
            "RECIBO" to R.string.aviso_recibo
        )

        fun de(tipo: String?): TipoAviso {
            val t = tipo.orEmpty()
            val categoria = when {
                t.startsWith("RESERVA") -> CategoriaAviso.RESERVAS
                t.startsWith("VEHICULO") -> CategoriaAviso.VEHICULOS
                t.startsWith("RESIDENTE") || t == "REGISTRO" || t == "SOLICITUD_ARRENDATARIO" -> CategoriaAviso.RESIDENTES
                t.startsWith("CUOTA") || t == "COMPROBANTE" || t == "SOLICITUD_CONTABLE" -> CategoriaAviso.PAGOS
                t == "PUBLICACION" -> CategoriaAviso.PUBLICACIONES
                t.startsWith("VISITA") -> CategoriaAviso.PORTERIA
                t == "CHAT" -> CategoriaAviso.CHAT
                t.startsWith("OBRA") || t.startsWith("PERSONAL_OBRA") -> CategoriaAviso.OBRAS
                t == "CORRESPONDENCIA" || t == "RECIBO" -> CategoriaAviso.CORRESPONDENCIA
                else -> CategoriaAviso.GENERAL
            }
            val estado = when {
                t.endsWith("_APROBADA") || t.endsWith("_APROBADO") || t == "CUOTA_PAGADA" -> EstadoAviso.APROBADO
                t.endsWith("_RECHAZADA") || t.endsWith("_RECHAZADO") -> EstadoAviso.RECHAZADO
                else -> null
            }
            return TipoAviso(categoria, TITULOS[t] ?: R.string.aviso_general, estado)
        }

        // Encabezados con que el backend empieza algunos mensajes y que ya dice el título
        private val ENCABEZADOS = setOf(
            "nueva reserva", "nuevo vehiculo registrado", "nuevo arrendatario por aprobar", "comprobante por revisar",
            "nuevo registro por aprobar", "solicitud de arrendatario", "visita en porteria", "por aprobar",
            "obra por aprobar", "trabajador por aprobar"
        )

        /**
         * El mensaje del backend suele empezar por lo mismo que el título ("Nueva reserva — 25/10…",
         * "Visita en portería: Juan…"): se quita para no leerlo dos veces y queda solo el detalle.
         * Si lo de antes dice algo más ("Cuota de administración de noviembre — …"), se deja todo.
         */
        fun detalle(context: Context, mensaje: String, tipo: TipoAviso): String {
            val texto = mensaje.trim()
            val titulo = normal(context.getString(tipo.titulo))
            for (separador in listOf(" — ", ": ")) {
                val i = texto.indexOf(separador)
                if (i <= 0) continue
                val antes = normal(texto.substring(0, i))
                if (antes in ENCABEZADOS || antes == titulo) {
                    return texto.substring(i + separador.length).trim().replaceFirstChar { it.uppercase() }
                }
            }
            return texto
        }

        private fun normal(s: String) = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "").lowercase(Locale.ROOT).trim()
    }
}

/** Fechas de los avisos: el backend manda la hora de Colombia sin zona ("2026-11-05T10:40:16"). */
object FechaAviso {
    private val zona = TimeZone.getTimeZone("America/Bogota")
    private val colombia = Locale("es", "CO")

    fun milis(iso: String?): Long? = try {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = zona; isLenient = false }
            .parse(iso.orEmpty().take(19))?.time
    } catch (_: Exception) {
        null
    }

    /** "Ahora", "hace 12 min" (la última hora) o la hora ("2:30 p. m."). */
    fun cuando(context: Context, iso: String?, ahora: Long = System.currentTimeMillis()): String {
        val t = milis(iso) ?: return ""
        val minutos = (ahora - t) / 60_000
        return when {
            minutos < 1 -> context.getString(R.string.aviso_ahora)
            minutos < 60 -> context.getString(R.string.aviso_hace_min, minutos.toInt())
            else -> SimpleDateFormat("h:mm a", colombia).apply { timeZone = zona }.format(t)
        }
    }

    /** El encabezado del día: "Hoy", "Ayer" o "lunes 3 de noviembre". */
    fun dia(context: Context, iso: String?, ahora: Long = System.currentTimeMillis()): String {
        val t = milis(iso) ?: return iso.orEmpty().take(10)
        fun claveDia(ms: Long) = Calendar.getInstance(zona).apply { timeInMillis = ms }
            .let { it.get(Calendar.YEAR) * 1000 + it.get(Calendar.DAY_OF_YEAR) }
        val hoy = claveDia(ahora)
        val ayer = claveDia(ahora - 86_400_000L)
        return when (claveDia(t)) {
            hoy -> context.getString(R.string.aviso_hoy)
            ayer -> context.getString(R.string.aviso_ayer)
            else -> SimpleDateFormat("EEEE d 'de' MMMM", colombia).apply { timeZone = zona }.format(t)
                .replaceFirstChar { it.uppercase() }
        }
    }
}
