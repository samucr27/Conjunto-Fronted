package com.example.conjuntoresidencial.ui.propietario

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.ItemObraBinding
import com.example.conjuntoresidencial.model.ObraDTO
import com.example.conjuntoresidencial.ui.comun.DiffPorId
import com.example.conjuntoresidencial.ui.comun.Estados
import com.example.conjuntoresidencial.ui.comun.mostrarEstado
import com.example.conjuntoresidencial.ui.obras.ObraDetalleFragment

class MisObrasAdapter(
    private val onDetalle: (ObraDTO) -> Unit,
    // null: sin botón de finalizar (lista de la administración)
    private val onFinalizar: ((ObraDTO) -> Unit)?,
    // P5: torre y apto (o "Obra del conjunto") encima de la descripción
    private val conLugar: Boolean = false
) : ListAdapter<ObraDTO, MisObrasAdapter.VH>(DiffPorId { it.id }) {

    class VH(val binding: ItemObraBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemObraBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: VH, pos: Int) {
        val obra = getItem(pos)
        h.binding.tvDescripcionObra.text = obra.descripcionAdecuacion
        h.binding.tvEstadoObra.mostrarEstado(Estados.obra(obra.estadoObra))
        h.binding.root.setOnClickListener { onDetalle(obra) }
        val contexto = h.binding.root.context
        h.binding.tvLugarObra.visibility = if (conLugar) View.VISIBLE else View.GONE
        if (conLugar) h.binding.tvLugarObra.text = if (obra.torre == null) contexto.getString(R.string.obra_del_conjunto)
            else contexto.getString(R.string.apto_item_titulo, obra.torre, obra.apto)
        // La aprobación solo se muestra mientras falta: casi todas están aprobadas
        val aprobada = ObraDetalleFragment.aprobada(obra)
        h.binding.tvAprobacionObra.visibility = if (aprobada) View.GONE else View.VISIBLE
        if (!aprobada) h.binding.tvAprobacionObra.mostrarEstado(Estados.aprobacion(obra.aprobacion))
        val pendientes = obra.personalPendiente ?: 0
        h.binding.tvPendientesObra.visibility = if (pendientes > 0) View.VISIBLE else View.GONE
        h.binding.tvPendientesObra.text =
            contexto.resources.getQuantityString(R.plurals.obras_personal_pendiente, pendientes, pendientes)
        val finalizar = onFinalizar
        // Finalizar desde la lista: la obra aprobada y en proceso (antes de aprobarla se cambia en el editor)
        if (finalizar != null && aprobada && obra.estadoObra == "EN_PROCESO") {
            h.binding.btnCambiarEstadoObra.visibility = View.VISIBLE
            h.binding.btnCambiarEstadoObra.contentDescription =
                h.binding.root.context.getString(R.string.desc_finalizar_obra, obra.descripcionAdecuacion)
            h.binding.btnCambiarEstadoObra.setOnClickListener { finalizar(obra) }
        } else {
            h.binding.btnCambiarEstadoObra.visibility = View.GONE
        }
    }
}
