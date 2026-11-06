package com.example.conjuntoresidencial.ui.plataforma

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.ItemCifraPlataformaBinding
import com.example.conjuntoresidencial.databinding.ItemTextoConAccionBinding
import com.example.conjuntoresidencial.model.EventoPlataforma
import com.example.conjuntoresidencial.util.Formatos

/** P8 bloque 6: piezas que comparten el panel, el detalle del conjunto y la auditoría. */

internal fun ItemCifraPlataformaBinding.poner(@DrawableRes icono: Int, @StringRes nombre: Int) {
    imgIcono.setImageResource(icono)
    tvNombre.setText(nombre)
}

internal fun ItemCifraPlataformaBinding.valor(valor: Any, detalle: String? = null) {
    tvValor.text = if (valor is Number) java.text.NumberFormat.getIntegerInstance(java.util.Locale.forLanguageTag("es-CO")).format(valor) else valor.toString()
    tvDetalle.isVisible = !detalle.isNullOrBlank()
    tvDetalle.text = detalle
}

/** P10: la cifra abre su pantalla al tocarla. */
internal fun ItemCifraPlataformaBinding.alTocar(accion: () -> Unit) {
    imgIr.isVisible = true
    root.setOnClickListener { accion() }
}


/** "2026-11-03T08:39:12" (hora de Colombia, sin zona) → la fecha y la hora como en el resto de la app. */
internal fun fechaEvento(iso: String?): String =
    if (iso.isNullOrBlank()) "" else Formatos.fechaHora(iso.take(10), iso.substringAfter('T', ""))

/**
 * Una fila de la auditoría; [conConjunto] muestra a qué conjunto se refiere (en las listas de todos).
 * P10: tocarla abre la operación completa (EventoPlataformaFragment).
 */
internal fun Fragment.filaEvento(padre: ViewGroup, e: EventoPlataforma, conConjunto: Boolean): View {
    val fila = ItemTextoConAccionBinding.inflate(LayoutInflater.from(padre.context), padre, false)
    fila.tvEtiqueta.isVisible = true
    fila.tvEtiqueta.setText(nombreAccion(e.accion))
    fila.tvTitulo.text = listOfNotNull(e.conjunto?.takeIf { conConjunto }, e.detalle?.takeIf { it.isNotBlank() })
        .joinToString(" · ").ifBlank { getString(nombreAccion(e.accion)) }
    fila.tvDetalle.text = listOf(fechaEvento(e.fecha), e.operador).filter { it.isNotBlank() }.joinToString(" · ")
    fila.imgFlecha.isVisible = true
    fila.root.setOnClickListener {
        findNavController().navigate(R.id.eventoPlataformaFragment, EventoPlataformaFragment.argumentos(e))
    }
    return fila.root
}

/** P10: fila en cápsula con ícono redondo a la izquierda y flecha si se puede tocar. */
internal fun ItemTextoConAccionBinding.conIcono(@DrawableRes icono: Int, tocable: Boolean = false): ItemTextoConAccionBinding {
    marcoIcono.isVisible = true
    imgIcono.setImageResource(icono)
    imgFlecha.isVisible = tocable
    return this
}
