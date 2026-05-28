package com.example.conjuntoresidencial.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.ItemObraConPersonalBinding
import com.example.conjuntoresidencial.model.ObraDTO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ObraConPersonalAdapter(
    private val lista: List<ObraDTO>,
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
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getPersonalPorObra(obra.id!!)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        h.binding.rvPersonalObra.adapter =
                            PersonalAdapter(response.body() ?: emptyList())
                    }
                }
            } catch (_: Exception) {}
        }
    }

    override fun getItemCount() = lista.size
}