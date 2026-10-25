package com.example.conjuntoresidencial.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.ItemApartamentoBinding
import com.example.conjuntoresidencial.model.Apartamento
import com.example.conjuntoresidencial.ui.comun.DiffPorId

/** @param soloNumero dentro de una torre: "Apartamento 101" en vez de "Torre 1 · Apto 101" */
class ApartamentoAdapter(
    private val soloNumero: Boolean = false,
    private val onClick: (Apartamento) -> Unit
) : ListAdapter<Apartamento, ApartamentoAdapter.ViewHolder>(DiffPorId { it.id }) {

    class ViewHolder(val binding: ItemApartamentoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemApartamentoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val apto = getItem(position)
        val contexto = holder.binding.root.context
        holder.binding.tvTorreApto.text =
            if (soloNumero) contexto.getString(R.string.apto_item_solo, apto.apto)
            else contexto.getString(R.string.apto_item_titulo, apto.torre, apto.apto)
        holder.binding.root.setOnClickListener { onClick(apto) }
    }
}
