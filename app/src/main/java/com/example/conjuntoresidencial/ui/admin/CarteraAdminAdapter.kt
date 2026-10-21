package com.example.conjuntoresidencial.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.ItemCarteraAdminBinding
import com.example.conjuntoresidencial.model.Apartamento
import com.example.conjuntoresidencial.model.PagoDTO
import com.example.conjuntoresidencial.ui.comun.DiffPorId
import com.example.conjuntoresidencial.ui.comun.Estados
import com.example.conjuntoresidencial.ui.comun.mostrarEstado

/** Cada elemento es un apartamento con su registro de pago (null si no tiene). */
class CarteraAdminAdapter :
    ListAdapter<Pair<Apartamento, PagoDTO?>, CarteraAdminAdapter.ViewHolder>(DiffPorId { it.first.id }) {

    class ViewHolder(val binding: ItemCarteraAdminBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemCarteraAdminBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (apto, pago) = getItem(position)
        val b = holder.binding
        b.tvNumeroApto.text = "Torre ${apto.torre} - Apto ${apto.apto}"

        // Observaciones del pago; si no hay registro de pago, se indica
        val detalle = when {
            pago == null -> b.root.context.getString(R.string.cartera_sin_registro)
            pago.observaciones.isNullOrBlank() -> null
            else -> pago.observaciones
        }
        b.tvPropietarioApto.text = detalle
        b.tvPropietarioApto.visibility = if (detalle == null) View.GONE else View.VISIBLE

        b.tvEstadoPago.mostrarEstado(Estados.cuenta(pago?.saldoPendiente, pago?.estadoCuenta))
    }
}
