package com.example.conjuntoresidencial.ui.propietario

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.databinding.ItemObraBinding
import com.example.conjuntoresidencial.model.ObraDTO
import com.example.conjuntoresidencial.ui.comun.DiffPorId
import com.example.conjuntoresidencial.ui.comun.Estados
import com.example.conjuntoresidencial.ui.comun.mostrarEstado

class MisObrasAdapter(
    private val onDetalle: (ObraDTO) -> Unit,
    private val onFinalizar: (ObraDTO) -> Unit
) : ListAdapter<ObraDTO, MisObrasAdapter.VH>(DiffPorId { it.id }) {

    class VH(val binding: ItemObraBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemObraBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: VH, pos: Int) {
        val obra = getItem(pos)
        h.binding.tvDescripcionObra.text = obra.descripcionAdecuacion
        h.binding.tvEstadoObra.mostrarEstado(Estados.obra(obra.estadoObra))
        h.binding.root.setOnClickListener { onDetalle(obra) }
        if (obra.estadoObra == "EN_PROCESO") {
            h.binding.btnCambiarEstadoObra.visibility = View.VISIBLE
            h.binding.btnCambiarEstadoObra.setOnClickListener { onFinalizar(obra) }
        } else {
            h.binding.btnCambiarEstadoObra.visibility = View.GONE
        }
    }
}
