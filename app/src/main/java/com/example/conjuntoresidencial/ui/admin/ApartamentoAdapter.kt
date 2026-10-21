package com.example.conjuntoresidencial.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.databinding.ItemApartamentoBinding
import com.example.conjuntoresidencial.model.Apartamento
import com.example.conjuntoresidencial.ui.comun.DiffPorId

class ApartamentoAdapter(
    private val onClick: (Apartamento) -> Unit
) : ListAdapter<Apartamento, ApartamentoAdapter.ViewHolder>(DiffPorId { it.id }) {

    class ViewHolder(val binding: ItemApartamentoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemApartamentoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val apto = getItem(position)
        holder.binding.tvTorreApto.text = "Torre ${apto.torre} - Apto ${apto.apto}"
        holder.binding.root.setOnClickListener { onClick(apto) }
    }
}
