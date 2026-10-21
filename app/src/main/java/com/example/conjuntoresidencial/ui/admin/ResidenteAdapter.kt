package com.example.conjuntoresidencial.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.model.ResidenteDTO
import com.example.conjuntoresidencial.ui.comun.DiffPorId

class ResidenteAdapter : ListAdapter<ResidenteDTO, ResidenteAdapter.VH>(DiffPorId { it.id }) {

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
        h.detalle.text = h.itemView.context.getString(R.string.residente_detalle, tipo, r.identificacion)
    }
}
