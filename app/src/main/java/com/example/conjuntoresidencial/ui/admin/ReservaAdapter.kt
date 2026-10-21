package com.example.conjuntoresidencial.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.ItemReservaBinding
import com.example.conjuntoresidencial.model.ReservaDTO
import com.example.conjuntoresidencial.ui.comun.DiffPorId
import com.example.conjuntoresidencial.ui.comun.Estados
import com.example.conjuntoresidencial.ui.comun.mostrarEstado
import com.example.conjuntoresidencial.util.Formatos

/** Una reserva con el nombre de su zona (el ViewModel carga las zonas una sola vez). */
data class ReservaConZona(val reserva: ReservaDTO, val nombreZona: String) {
    companion object {
        fun de(reservas: List<ReservaDTO>, nombres: Map<Long, String>) =
            reservas.map { ReservaConZona(it, nombres[it.zonaComunId] ?: "Zona ${it.zonaComunId}") }
    }
}

/** Sin callbacks (vista del propietario) no se muestran los botones de aprobar/rechazar. */
class ReservaAdapter(
    private val onAprobar: ((ReservaDTO) -> Unit)? = null,
    private val onRechazar: ((ReservaDTO) -> Unit)? = null
) : ListAdapter<ReservaConZona, ReservaAdapter.ViewHolder>(DiffPorId { it.reserva.id }) {

    class ViewHolder(val binding: ItemReservaBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemReservaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (reserva, nombreZona) = getItem(position)
        holder.binding.tvZonaReserva.text = nombreZona
        holder.binding.tvFechaReserva.text = Formatos.fechaHora(reserva.fecha, reserva.hora)
        holder.binding.tvEstadoReserva.mostrarEstado(Estados.reserva(reserva.estado))

        if (reserva.estado == "PENDIENTE" && onAprobar != null && onRechazar != null) {
            holder.binding.layoutBotonesReserva.visibility = View.VISIBLE
            val ctx = holder.binding.root.context
            val cuando = Formatos.fechaHora(reserva.fecha, reserva.hora)
            holder.binding.btnAprobarReserva.contentDescription = ctx.getString(R.string.desc_aprobar_reserva, nombreZona, cuando)
            holder.binding.btnRechazarReserva.contentDescription = ctx.getString(R.string.desc_rechazar_reserva, nombreZona, cuando)
            holder.binding.btnAprobarReserva.setOnClickListener { onAprobar.invoke(reserva) }
            holder.binding.btnRechazarReserva.setOnClickListener { onRechazar.invoke(reserva) }
        } else {
            holder.binding.layoutBotonesReserva.visibility = View.GONE
        }
    }
}
