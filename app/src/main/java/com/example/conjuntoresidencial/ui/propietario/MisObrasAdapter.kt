package com.example.conjuntoresidencial.ui.propietario

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.databinding.ItemObraBinding
import com.example.conjuntoresidencial.model.ObraDTO

class MisObrasAdapter(
    private val lista: List<ObraDTO>,
    private val onClick: (ObraDTO) -> Unit
) : RecyclerView.Adapter<MisObrasAdapter.VH>() {

    class VH(val binding: ItemObraBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemObraBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: VH, pos: Int) {
        val obra = lista[pos]
        h.binding.tvDescripcionObra.text = obra.descripcionAdecuacion
        h.binding.tvEstadoObra.text = obra.estadoObra
        h.binding.root.setOnClickListener { onClick(obra) }
    }

    override fun getItemCount() = lista.size
}