package com.example.conjuntoresidencial.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.databinding.ItemReservaBinding
import com.example.conjuntoresidencial.model.ReservaDTO

/**
 * [nombresZonas] lo carga el ViewModel una sola vez (zonaId -> nombre);
 * el adaptador no hace peticiones por fila.
 */
class ReservaAdapter(
    private val lista: List<ReservaDTO>,
    private val nombresZonas: Map<Long, String> = emptyMap(),
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
        holder.binding.tvZonaReserva.text = nombresZonas[reserva.zonaComunId] ?: "Zona ${reserva.zonaComunId}"

        // Mostrar botones solo si está PENDIENTE y hay callbacks
        if (reserva.estado == "PENDIENTE" && onAprobar != null && onRechazar != null) {
            holder.binding.layoutBotonesReserva.visibility = View.VISIBLE
            holder.binding.btnAprobarReserva.setOnClickListener { onAprobar.invoke(reserva) }
            holder.binding.btnRechazarReserva.setOnClickListener { onRechazar.invoke(reserva) }
        } else {
            holder.binding.layoutBotonesReserva.visibility = View.GONE
        }
    }

    override fun getItemCount() = lista.size
}
