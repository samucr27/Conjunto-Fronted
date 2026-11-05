package com.example.conjuntoresidencial.ui.recepcion

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.ItemVisitaBinding
import com.example.conjuntoresidencial.model.VisitaDTO
import com.example.conjuntoresidencial.ui.comun.aviso

/** P8 bloque 5: textos compartidos por el inicio, el historial y la minuta de la recepción. */

/** "2026-11-05T14:30:00" → "05/11 14:30". */
internal fun fechaCorta(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    val fecha = iso.substringBefore('T')
    val hora = iso.substringAfter('T', "").take(5)
    val partes = fecha.split('-')
    val dia = if (partes.size == 3) "${partes[2]}/${partes[1]}" else fecha
    return if (hora.isEmpty()) dia else "$dia $hora"
}

internal fun horaCorta(iso: String?): String = iso?.substringAfter('T', "")?.take(5).orEmpty()

/** A pie, o la placa con su parqueadero. */
internal fun Fragment.ingreso(v: VisitaDTO): String = when {
    v.tipoIngreso == "VEHICULO" && v.parqueadero != null -> getString(R.string.ingreso_en_vehiculo, v.placa.orEmpty(), v.parqueadero)
    v.tipoIngreso == "VEHICULO" -> v.placa.orEmpty()
    else -> getString(R.string.ingreso_a_pie)
}

internal fun Fragment.estado(v: VisitaDTO): String {
    val canal = getString(if (v.canal == "LLAMADA") R.string.porteria_por_llamada else R.string.porteria_por_app)
    return when (v.estado) {
        "AUTORIZADA" -> getString(R.string.porteria_estado_autorizada, canal)
        "RECHAZADA" -> getString(R.string.porteria_estado_rechazada, canal)
        "EXPIRADA" -> getString(R.string.porteria_estado_expirada)
        else -> getString(R.string.porteria_estado_pendiente)
    }
}

/** Una visita en una lista de solo lectura (historial): sin los botones de llamar. */
internal fun Fragment.pintarVisita(fila: ItemVisitaBinding, v: VisitaDTO) {
    fila.tvVisitaTitulo.text = getString(R.string.porteria_visita_titulo, v.visitante, v.torre, v.apto)
    fila.tvVisitaDetalle.text = listOfNotNull(
        fechaCorta(v.creadaEn), ingreso(v), v.motivo, v.documento,
        v.salidaEn?.let { getString(R.string.ingreso_salio, horaCorta(it)) },
        v.guardia?.let { getString(R.string.porteria_registro_de, it) }
    ).filter { it.isNotBlank() }.joinToString(" · ")
    fila.tvVisitaEstado.text = estado(v)
    fila.accionesVisita.visibility = android.view.View.GONE
}

internal fun Fragment.llamar(telefono: String) {
    try {
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$telefono")))
    } catch (_: ActivityNotFoundException) {
        aviso(R.string.porteria_sin_marcador).show()
    }
}

/** Las categorías de la minuta, en el orden de los chips. */
internal val CATEGORIAS_MINUTA: List<Pair<String, Int>> = listOf(
    "NOVEDAD" to R.string.minuta_novedad,
    "RONDA" to R.string.minuta_ronda,
    "PAQUETE" to R.string.minuta_paquete,
    "INCIDENTE" to R.string.minuta_incidente,
    "TURNO" to R.string.minuta_turno
)

@StringRes
internal fun nombreCategoria(categoria: String): Int =
    CATEGORIAS_MINUTA.firstOrNull { it.first == categoria }?.second ?: R.string.minuta_novedad
