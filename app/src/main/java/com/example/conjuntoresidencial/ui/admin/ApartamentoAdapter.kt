package com.example.conjuntoresidencial.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.databinding.ItemApartamentoBinding
import com.example.conjuntoresidencial.model.Apartamento

class ApartamentoAdapter(
    private val lista: List<Apartamento>,
    private val onClick: (Apartamento) -> Unit
) : RecyclerView.Adapter<ApartamentoAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemApartamentoBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemApartamentoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val apto = lista[position]
        holder.binding.tvTorreApto.text = "Torre ${apto.torre} - Apto ${apto.apto}"
        holder.binding.root.setOnClickListener { onClick(apto) }
    }

    override fun getItemCount() = lista.size
}