package com.example.conjuntoresidencial.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.databinding.ItemObraBinding
import com.example.conjuntoresidencial.model.ObraDTO

class ObraAdapter(private val lista: List<ObraDTO>) :
    RecyclerView.Adapter<ObraAdapter.ViewHolder>() {
    inner class ViewHolder(val binding: ItemObraBinding) : RecyclerView.ViewHolder(binding.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemObraBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val obra = lista[position]
        holder.binding.tvDescripcionObra.text = obra.descripcionAdecuacion
        holder.binding.tvEstadoObra.text = obra.estadoObra
    }
    override fun getItemCount() = lista.size
}