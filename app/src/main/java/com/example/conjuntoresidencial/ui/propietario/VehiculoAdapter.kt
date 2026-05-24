package com.example.conjuntoresidencial.ui.propietario

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.databinding.ItemVehiculoBinding
import com.example.conjuntoresidencial.model.VehiculoDTO

class VehiculoAdapter(
    private val lista: List<VehiculoDTO>,
    private val onEliminar: (VehiculoDTO) -> Unit
) : RecyclerView.Adapter<VehiculoAdapter.VH>() {

    class VH(val binding: ItemVehiculoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemVehiculoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: VH, pos: Int) {
        val v = lista[pos]
        h.binding.tvMarcaVehiculo.text = "${v.marca} · ${v.tipo}"
        h.binding.tvPlacaVehiculo.text = "${v.placa} · ${v.color}"
        h.binding.btnEliminarVehiculo.setOnClickListener { onEliminar(v) }
    }

    override fun getItemCount() = lista.size
}