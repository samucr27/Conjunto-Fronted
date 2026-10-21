package com.example.conjuntoresidencial.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.databinding.ItemObraConPersonalBinding
import com.example.conjuntoresidencial.model.ObraDTO
import com.example.conjuntoresidencial.model.PersonalObraDTO

/**
 * [personalPorObra] lo carga el ViewModel una sola vez (obraId -> personal);
 * el adaptador no hace peticiones por fila.
 */
class ObraConPersonalAdapter(
    private val lista: List<ObraDTO>,
    private val personalPorObra: Map<Long, List<PersonalObraDTO>>,
    private val onCambiarEstado: (obraId: Long, nuevoEstado: String) -> Unit
) : RecyclerView.Adapter<ObraConPersonalAdapter.VH>() {

    class VH(val binding: ItemObraConPersonalBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemObraConPersonalBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: VH, pos: Int) {
        val obra = lista[pos]
        h.binding.tvDescripcionObraAdmin.text = obra.descripcionAdecuacion
        h.binding.tvFechasObra.text = "${obra.fechaInicio} → ${obra.fechaFinEstimada}"

        // Estado con color según valor
        val esFinalizada = obra.estadoObra == "FINALIZADA"
        h.binding.tvEstadoObraAdmin.text = if (esFinalizada) "✓ Finalizada" else "⏳ En proceso"
        h.binding.tvEstadoObraAdmin.setTextColor(
            if (esFinalizada)
                h.binding.root.context.getColor(android.R.color.holo_green_dark)
            else
                h.binding.root.context.getColor(android.R.color.holo_orange_dark)
        )

        // Botón cambiar estado — solo visible si está EN_PROCESO
        if (!esFinalizada) {
            h.binding.btnCambiarEstadoObra.visibility = android.view.View.VISIBLE
            h.binding.btnCambiarEstadoObra.text = "Finalizar"
            h.binding.btnCambiarEstadoObra.setOnClickListener {
                onCambiarEstado(obra.id!!, "FINALIZADA")
            }
        } else {
            h.binding.btnCambiarEstadoObra.visibility = android.view.View.GONE
        }

        // Personal de la obra
        h.binding.rvPersonalObra.layoutManager = LinearLayoutManager(h.binding.root.context)
        h.binding.rvPersonalObra.adapter = PersonalAdapter(obra.id?.let { personalPorObra[it] }.orEmpty())
    }

    override fun getItemCount() = lista.size
}
