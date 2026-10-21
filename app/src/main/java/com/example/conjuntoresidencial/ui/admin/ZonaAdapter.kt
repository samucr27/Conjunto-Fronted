package com.example.conjuntoresidencial.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.ItemZonaBinding
import com.example.conjuntoresidencial.model.ZonaComunDTO
import com.example.conjuntoresidencial.ui.comun.DiffPorId
import com.example.conjuntoresidencial.ui.comun.EstadoVisual
import com.example.conjuntoresidencial.ui.comun.TonoEstado
import com.example.conjuntoresidencial.ui.comun.mostrarEstado

/** Sin [onReservar] (vista del admin) no se muestra el botón de reservar. */
class ZonaAdapter(
    private val onReservar: ((ZonaComunDTO) -> Unit)? = null
) : ListAdapter<ZonaComunDTO, ZonaAdapter.ViewHolder>(DiffPorId { it.id }) {

    class ViewHolder(val binding: ItemZonaBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemZonaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val zona = getItem(position)
        val reservable = zona.esReservable == true
        holder.binding.tvNombreZona.text = zona.nombre
        holder.binding.tvEstadoZona.text = zona.estadoRecibido
        holder.binding.tvReservable.mostrarEstado(
            if (reservable) EstadoVisual(R.string.zona_reservable, TonoEstado.EXITO)
            else EstadoVisual(R.string.zona_no_reservable, TonoEstado.NEUTRO)
        )
        if (onReservar != null && reservable) {
            holder.binding.btnReservar.visibility = View.VISIBLE
            holder.binding.btnReservar.setOnClickListener { onReservar.invoke(zona) }
        } else {
            holder.binding.btnReservar.visibility = View.GONE
        }
    }
}
