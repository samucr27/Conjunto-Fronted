package com.example.conjuntoresidencial.ui.propietario

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.R
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
        val ctx = h.binding.root.context
        // "Mazda · Carro" y "ABC123 · Rojo"; sin marca o color se omiten (son opcionales)
        h.binding.tvMarcaVehiculo.text = listOf(v.marca, v.tipo).filterNot { it.isNullOrBlank() }.joinToString(SEPARADOR)
        h.binding.tvPlacaVehiculo.text = listOf(v.placa, v.color).filterNot { it.isNullOrBlank() }.joinToString(SEPARADOR)
        h.binding.btnEliminarVehiculo.contentDescription = ctx.getString(R.string.desc_eliminar_vehiculo, v.placa)
        h.binding.btnEliminarVehiculo.setOnClickListener { onEliminar(v) }
    }
}

private const val SEPARADOR = " · "
