package com.example.conjuntoresidencial.ui.comun

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.ItemZonaBinding
import com.example.conjuntoresidencial.model.ZonaComunDTO
import com.example.conjuntoresidencial.ui.comun.DiffPorId
import com.example.conjuntoresidencial.ui.comun.EstadoVisual
import com.example.conjuntoresidencial.ui.comun.TonoEstado
import com.example.conjuntoresidencial.ui.comun.mostrarEstado
import com.example.conjuntoresidencial.util.Formatos

/**
 * Zonas comunes. Con [onReservar] (residente) las reservables muestran "Reservar"; con
 * [onTocar] (administrador) tocar la tarjeta la abre para editarla.
 */
class ZonaAdapter(
    private val onReservar: ((ZonaComunDTO) -> Unit)? = null,
    private val onTocar: ((ZonaComunDTO) -> Unit)? = null
) : ListAdapter<ZonaComunDTO, ZonaAdapter.ViewHolder>(DiffPorId { it.id }) {

    class ViewHolder(val binding: ItemZonaBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemZonaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val zona = getItem(position)
        val ctx = holder.binding.root.context
        val reservable = zona.esReservable == true
        holder.binding.tvNombreZona.text = zona.nombre
        // "Bueno · Aforo 20 · 9:00 a. m. a 10:00 p. m."
        holder.binding.tvEstadoZona.text = listOfNotNull(
            zona.estadoRecibido.takeIf { it.isNotBlank() },
            zona.aforo?.takeIf { reservable }?.let { ctx.resources.getQuantityString(R.plurals.zona_aforo_corto, it, it) },
            if (reservable && (zona.horaApertura != null || zona.horaCierre != null)) ctx.getString(R.string.reservar_horario,
                Formatos.hora(zona.horaApertura ?: "00:00"), Formatos.hora(zona.horaCierre ?: "23:59")) else null
        ).joinToString(" · ")
        holder.binding.tvReservable.mostrarEstado(
            if (reservable) EstadoVisual(R.string.zona_reservable, TonoEstado.EXITO)
            else EstadoVisual(R.string.zona_no_reservable, TonoEstado.NEUTRO)
        )
        if (onReservar != null && reservable) {
            holder.binding.btnReservar.visibility = View.VISIBLE
            holder.binding.btnReservar.contentDescription = ctx.getString(R.string.desc_reservar_zona, zona.nombre)
            holder.binding.btnReservar.setOnClickListener { onReservar.invoke(zona) }
        } else {
            holder.binding.btnReservar.visibility = View.GONE
        }
        if (onTocar != null) {
            holder.binding.root.isClickable = true
            holder.binding.root.isFocusable = true
            holder.binding.root.setOnClickListener { onTocar.invoke(zona) }
            // TalkBack lee la tarjeta y dice "Toca dos veces para editar"
            ViewCompat.replaceAccessibilityAction(holder.binding.root, AccessibilityActionCompat.ACTION_CLICK,
                ctx.getString(R.string.editar), null)
        }
    }
}
