package com.example.conjuntoresidencial.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.model.ResidenteDTO

class ResidenteAdapter(private val lista: List<ResidenteDTO>) :
    RecyclerView.Adapter<ResidenteAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val nombre: TextView = v.findViewById(android.R.id.text1)
        val detalle: TextView = v.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
    )

    override fun onBindViewHolder(h: VH, pos: Int) {
        val r = lista[pos]
        h.nombre.text = r.nombreCompleto
        h.detalle.text = "${r.tipoHabitante} · ${r.identificacion}"
    }

    override fun getItemCount() = lista.size
}