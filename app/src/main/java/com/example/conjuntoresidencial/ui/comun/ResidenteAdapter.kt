package com.example.conjuntoresidencial.ui.comun

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.model.ResidenteDTO
import com.example.conjuntoresidencial.ui.comun.DiffPorId

/**
 * Residentes: "Arrendatario · CC 123 · Con acceso". Con [conApartamento] (pendientes del
 * administrador) empieza por la torre y el apto; con [onTocar] tocar la fila abre sus opciones.
 */
class ResidenteAdapter(
    private val conApartamento: Boolean = false,
    private val onTocar: ((ResidenteDTO) -> Unit)? = null
) : ListAdapter<ResidenteDTO, ResidenteAdapter.VH>(DiffPorId { it.id }) {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val nombre: TextView = v.findViewById(android.R.id.text1)
        val detalle: TextView = v.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
    )

    override fun onBindViewHolder(h: VH, pos: Int) {
        val r = getItem(pos)
        h.nombre.text = r.nombreCompleto
        // "PROPIETARIO" -> "Propietario"
        val tipo = r.tipoHabitante.lowercase().replaceFirstChar { it.uppercase() }
        val ctx = h.itemView.context
        h.detalle.text = listOfNotNull(
            if (conApartamento && r.torre != null) ctx.getString(R.string.apto_item_titulo, r.torre, r.apto) else null,
            ctx.getString(R.string.residente_detalle, tipo, r.identificacion),
            when {
                r.estado == "PENDIENTE" -> ctx.getString(R.string.residente_estado_pendiente)
                r.estado == "RECHAZADO" -> ctx.getString(R.string.residente_estado_rechazado)
                r.tieneAcceso == true -> ctx.getString(R.string.residente_con_acceso)
                else -> null
            }
        ).joinToString(" · ")
        if (onTocar != null) {
            h.itemView.isClickable = true
            h.itemView.isFocusable = true
            h.itemView.setOnClickListener { onTocar.invoke(r) }
        } else {
            h.itemView.setOnClickListener(null)
            h.itemView.isClickable = false
        }
    }
}
