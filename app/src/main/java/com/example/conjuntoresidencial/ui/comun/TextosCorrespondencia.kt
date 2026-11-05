package com.example.conjuntoresidencial.ui.comun

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.ItemCorrespondenciaBinding
import com.example.conjuntoresidencial.model.CorrespondenciaDTO

/** P9 bloque 3: cómo se ve un paquete, carta, documento o recibo en la portería y en Mi correspondencia. */

/** Los tipos, en el orden de los chips al registrar. */
internal val TIPOS_CORRESPONDENCIA: List<Pair<String, Int>> = listOf(
    "PAQUETE" to R.string.corr_tipo_paquete,
    "CARTA" to R.string.corr_tipo_carta,
    "DOCUMENTO" to R.string.corr_tipo_documento,
    "RECIBO" to R.string.corr_tipo_recibo
)

@StringRes
internal fun nombreTipoCorrespondencia(tipo: String): Int =
    TIPOS_CORRESPONDENCIA.firstOrNull { it.first == tipo }?.second ?: R.string.corr_tipo_paquete

@DrawableRes
private fun iconoTipo(tipo: String): Int = when (tipo) {
    "CARTA" -> R.drawable.ic_mail
    "DOCUMENTO" -> R.drawable.ic_description
    "RECIBO" -> R.drawable.ic_wallet
    else -> R.drawable.ic_package
}

/** "2026-11-05T14:30:00" → "hoy 14:30", "ayer 09:10", "03/11 18:00". */
internal fun Fragment.fechaCorrespondencia(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    val hora = iso.substringAfter('T', "").take(5)
    val hoy = getString(R.string.aviso_hoy)
    val ayer = getString(R.string.aviso_ayer)
    val dia = when (val d = FechaAviso.dia(requireContext(), iso)) {
        hoy, ayer -> d.lowercase()
        else -> iso.take(10).split('-').let { if (it.size == 3) "${it[2]}/${it[1]}" else d }
    }
    return if (hora.isEmpty()) dia else "$dia $hora"
}

/**
 * Llena una fila. [conApartamento]: en la portería se ve en grande a qué apartamento va; el
 * residente no lo necesita. [alEntregar] null: sin botón (residente o ya entregada).
 */
internal fun Fragment.pintarCorrespondencia(
    fila: ItemCorrespondenciaBinding,
    c: CorrespondenciaDTO,
    conApartamento: Boolean,
    alEntregar: ((CorrespondenciaDTO) -> Unit)?
) {
    val ctx = requireContext()
    val entregada = c.estado == "ENTREGADA"
    val color = ContextCompat.getColor(ctx, if (entregada) R.color.aviso_aprobado else R.color.aviso_correspondencia)
    fila.imgTipo.setImageResource(iconoTipo(c.tipo))
    fila.imgTipo.setColorFilter(color)
    fila.fondoIcono.background.mutate().setTint(ColorUtils.setAlphaComponent(color, 0x2E))
    fila.tvTipo.setText(nombreTipoCorrespondencia(c.tipo))
    fila.tvTipo.setTextColor(color)
    fila.tvEstado.setText(if (entregada) R.string.corr_entregada else if (conApartamento) R.string.corr_por_entregar else R.string.corr_por_recoger)
    fila.tvEstado.setTextColor(color)
    fila.tvEstado.background.mutate().setTint(ColorUtils.setAlphaComponent(color, 0x24))

    fila.tvTitulo.text = listOfNotNull(c.remitente, c.nota).joinToString(" · ").ifBlank { getString(nombreTipoCorrespondencia(c.tipo)) }
    fila.tvApartamento.isVisible = conApartamento
    fila.tvApartamento.text = getString(R.string.corr_apartamento, c.torre, c.apto)
    fila.tvDetalle.text = if (entregada) {
        listOfNotNull(
            getString(R.string.corr_entregada_a, c.entregadaA.orEmpty(), fechaCorrespondencia(c.entregadaEn)),
            c.entregadaPor?.let { getString(R.string.corr_entregada_por, it) }
        ).joinToString(" ")
    } else {
        listOfNotNull(
            getString(R.string.corr_llego, fechaCorrespondencia(c.recibidaEn)),
            c.guardia?.takeIf { conApartamento }?.let { getString(R.string.corr_recibio, it) }
        ).joinToString(" · ")
    }
    fila.btnEntregar.isVisible = !entregada && alEntregar != null
    fila.btnEntregar.setOnClickListener { alEntregar?.invoke(c) }
}
