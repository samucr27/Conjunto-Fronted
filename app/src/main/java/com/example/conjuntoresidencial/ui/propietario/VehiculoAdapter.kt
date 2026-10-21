package com.example.conjuntoresidencial.ui.propietario

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.databinding.ItemVehiculoBinding
import com.example.conjuntoresidencial.model.VehiculoDTO
import com.example.conjuntoresidencial.ui.comun.DiffPorId

class VehiculoAdapter(
    private val onEliminar: (VehiculoDTO) -> Unit
) : ListAdapter<VehiculoDTO, VehiculoAdapter.VH>(DiffPorId { it.id }) {

    class VH(val binding: ItemVehiculoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemVehiculoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: VH, pos: Int) {
        val v = getItem(pos)
        h.binding.tvMarcaVehiculo.text = "${v.marca} · ${v.tipo}"
        h.binding.tvPlacaVehiculo.text = "${v.placa} · ${v.color}"
        h.binding.btnEliminarVehiculo.setOnClickListener { onEliminar(v) }
    }
}
