package com.example.conjuntoresidencial.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.databinding.ItemZonaBinding
import com.example.conjuntoresidencial.model.ZonaComunDTO

class ZonaAdapter(
    private val lista: List<ZonaComunDTO>,
    private val onReservar: ((ZonaComunDTO) -> Unit)? = null
) : RecyclerView.Adapter<ZonaAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemZonaBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemZonaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val zona = lista[position]
        holder.binding.tvNombreZona.text = zona.nombre
        holder.binding.tvEstadoZona.text = zona.estadoRecibido
        holder.binding.tvReservable.text = if (zona.esReservable == true) "✓ Reservable" else "✗ No reservable"

        if (onReservar != null && zona.esReservable == true) {
            holder.binding.btnReservar.visibility = android.view.View.VISIBLE
            holder.binding.btnReservar.setOnClickListener { onReservar.invoke(zona) }
        } else {
            holder.binding.btnReservar.visibility = android.view.View.GONE
        }
    }

    override fun getItemCount() = lista.size
}