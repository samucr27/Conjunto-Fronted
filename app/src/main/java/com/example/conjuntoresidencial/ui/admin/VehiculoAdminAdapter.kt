package com.example.conjuntoresidencial.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.model.VehiculoDTO

class VehiculoAdminAdapter(private val lista: List<VehiculoDTO>) :
    RecyclerView.Adapter<VehiculoAdminAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val placa: TextView = v.findViewById(android.R.id.text1)
        val detalle: TextView = v.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
    )

    override fun onBindViewHolder(h: VH, pos: Int) {
        val v = lista[pos]
        h.placa.text = v.placa
        h.detalle.text = "${v.marca} · ${v.color} · ${v.tipo}"
    }

    override fun getItemCount() = lista.size
}