package com.example.conjuntoresidencial.ui.comun

import android.graphics.drawable.GradientDrawable
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.util.Formatos
import java.text.Normalizer

/** Tono semántico de un estado: define los colores de la etiqueta. */
enum class TonoEstado(@ColorRes val fondo: Int, @ColorRes val texto: Int) {
    EXITO(R.color.estado_exito_container, R.color.estado_on_exito_container),
    ADVERTENCIA(R.color.estado_advertencia_container, R.color.estado_on_advertencia_container),
    ERROR(R.color.md_error_container, R.color.md_on_error_container),
    NEUTRO(R.color.md_surface_container_highest, R.color.md_on_surface_variant)
}

/** Texto (recurso, con un argumento opcional) y tono de una etiqueta de estado. */
data class EstadoVisual(@StringRes val etiqueta: Int, val tono: TonoEstado, val argumento: String? = null)

/** Traduce los valores del backend (ver Catalogo.java) a etiquetas legibles. */
object Estados {

    fun reserva(estado: String?): EstadoVisual = when (estado?.uppercase()) {
        "PENDIENTE" -> EstadoVisual(R.string.estado_pendiente, TonoEstado.ADVERTENCIA)
        "APROBADA" -> EstadoVisual(R.string.estado_aprobada, TonoEstado.EXITO)
        "RECHAZADA" -> EstadoVisual(R.string.estado_rechazada, TonoEstado.ERROR)
        else -> desconocido(estado)
    }

    /** Los vehículos de antes de P4 no traen estado: estaban aprobados. */
    fun vehiculo(estado: String?): EstadoVisual = when (estado?.uppercase()) {
        "PENDIENTE" -> EstadoVisual(R.string.estado_pendiente_aprobacion, TonoEstado.ADVERTENCIA)
        "APROBADO", null -> EstadoVisual(R.string.estado_aprobado, TonoEstado.EXITO)
        "RECHAZADO" -> EstadoVisual(R.string.estado_rechazado, TonoEstado.ERROR)
        else -> desconocido(estado)
    }

    fun obra(estado: String?): EstadoVisual = when (estado?.uppercase()) {
        "EN_PROCESO" -> EstadoVisual(R.string.estado_en_proceso, TonoEstado.ADVERTENCIA)
        "FINALIZADA" -> EstadoVisual(R.string.estado_finalizada, TonoEstado.EXITO)
        else -> desconocido(estado)
    }

    /** "Al día" si no hay saldo pendiente o el backend lo marca así; si no, "Debe $X". */
    fun cuenta(saldo: Double?, estadoCuenta: String?): EstadoVisual {
        val alDia = (saldo ?: 0.0) <= 0.0 || sinTildes(estadoCuenta).equals("al dia", ignoreCase = true)
        return if (alDia) EstadoVisual(R.string.estado_al_dia, TonoEstado.EXITO)
        else EstadoVisual(R.string.estado_debe, TonoEstado.ERROR, Formatos.moneda(saldo ?: 0.0))
    }

    fun arl(vigente: Boolean): EstadoVisual =
        if (vigente) EstadoVisual(R.string.estado_arl_vigente, TonoEstado.EXITO)
        else EstadoVisual(R.string.estado_arl_no_vigente, TonoEstado.ERROR)

    private fun desconocido(valor: String?) =
        EstadoVisual(R.string.estado_desconocido, TonoEstado.NEUTRO, valor ?: "—")

    private fun sinTildes(texto: String?): String =
        Normalizer.normalize(texto.orEmpty().trim(), Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")
}

/**
 * Muestra un estado como etiqueta con forma de píldora (texto y fondo del tono).
 * TalkBack lo lee como "Estado: aprobada".
 */
fun TextView.mostrarEstado(estado: EstadoVisual) {
    val texto = if (estado.argumento != null) context.getString(estado.etiqueta, estado.argumento)
    else context.getString(estado.etiqueta)
    this.text = texto
    TextViewCompat.setTextAppearance(this, R.style.TextAppearance_App_LabelMedium)
    setTextColor(ContextCompat.getColor(context, estado.tono.texto))
    background = GradientDrawable().apply {
        cornerRadius = resources.displayMetrics.density * 50
        setColor(ContextCompat.getColor(context, estado.tono.fondo))
    }
    val h = (resources.displayMetrics.density * 10).toInt()
    val v = (resources.displayMetrics.density * 4).toInt()
    setPadding(h, v, h, v)
    contentDescription = context.getString(R.string.estado_descripcion, texto)
}
