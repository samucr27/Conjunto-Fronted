package com.example.conjuntoresidencial.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.ItemReservaBinding
import com.example.conjuntoresidencial.model.ReservaDTO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReservaAdapter(
    private val lista: List<ReservaDTO>,
    private val onAprobar: ((ReservaDTO) -> Unit)? = null,
    private val onRechazar: ((ReservaDTO) -> Unit)? = null
) : RecyclerView.Adapter<ReservaAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemReservaBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemReservaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val reserva = lista[position]
        holder.binding.tvFechaReserva.text = "${reserva.fecha} ${reserva.hora}"
        holder.binding.tvEstadoReserva.text = reserva.estado
        holder.binding.tvZonaReserva.text = "Cargando..."

        // Mostrar botones solo si está PENDIENTE y hay callbacks
        if (reserva.estado == "PENDIENTE" && onAprobar != null && onRechazar != null) {
            holder.binding.layoutBotonesReserva.visibility = View.VISIBLE
            holder.binding.btnAprobarReserva.setOnClickListener { onAprobar.invoke(reserva) }
            holder.binding.btnRechazarReserva.setOnClickListener { onRechazar.invoke(reserva) }
        } else {
            holder.binding.layoutBotonesReserva.visibility = View.GONE
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getZonaComunById(reserva.zonaComunId)
                val nombre = if (response.isSuccessful) response.body()?.nombre ?: "Zona ${reserva.zonaComunId}"
                else "Zona ${reserva.zonaComunId}"
                withContext(Dispatchers.Main) {
                    holder.binding.tvZonaReserva.text = nombre
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    holder.binding.tvZonaReserva.text = "Zona ${reserva.zonaComunId}"
                }
            }
        }
    }

    override fun getItemCount() = lista.size
}