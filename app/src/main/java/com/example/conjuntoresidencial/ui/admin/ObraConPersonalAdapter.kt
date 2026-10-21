package com.example.conjuntoresidencial.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.databinding.ItemObraConPersonalBinding
import com.example.conjuntoresidencial.model.ObraDTO
import com.example.conjuntoresidencial.model.PersonalObraDTO
import com.example.conjuntoresidencial.ui.comun.DiffPorId
import com.example.conjuntoresidencial.ui.comun.Estados
import com.example.conjuntoresidencial.ui.comun.mostrarEstado
import com.example.conjuntoresidencial.util.Formatos

/** Una obra con su personal (el ViewModel carga todo el personal una sola vez). */
data class ObraConPersonal(val obra: ObraDTO, val personal: List<PersonalObraDTO>)

class ObraConPersonalAdapter(
    private val onCambiarEstado: (obraId: Long, nuevoEstado: String) -> Unit
) : ListAdapter<ObraConPersonal, ObraConPersonalAdapter.VH>(DiffPorId { it.obra.id }) {

    class VH(val binding: ItemObraConPersonalBinding) : RecyclerView.ViewHolder(binding.root) {
        val personalAdapter = PersonalAdapter().also {
            binding.rvPersonalObra.layoutManager = LinearLayoutManager(binding.root.context)
            binding.rvPersonalObra.adapter = it
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemObraConPersonalBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: VH, pos: Int) {
        val (obra, personal) = getItem(pos)
        h.binding.tvDescripcionObraAdmin.text = obra.descripcionAdecuacion
        h.binding.tvFechasObra.text = Formatos.rango(obra.fechaInicio, obra.fechaFinEstimada)
        h.binding.tvEstadoObraAdmin.mostrarEstado(Estados.obra(obra.estadoObra))

        // Botón cambiar estado — solo visible si está EN_PROCESO
        if (obra.estadoObra == "EN_PROCESO") {
            h.binding.btnCambiarEstadoObra.visibility = View.VISIBLE
            h.binding.btnCambiarEstadoObra.text = "Finalizar"
            h.binding.btnCambiarEstadoObra.setOnClickListener { onCambiarEstado(obra.id!!, "FINALIZADA") }
        } else {
            h.binding.btnCambiarEstadoObra.visibility = View.GONE
        }

        h.personalAdapter.submitList(personal)
    }
}
