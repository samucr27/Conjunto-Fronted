package com.example.conjuntoresidencial.ui.admin

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.databinding.ItemCarteraAdminBinding
import com.example.conjuntoresidencial.model.Apartamento
import com.example.conjuntoresidencial.model.PagoDTO

class CarteraAdminAdapter(private val listaCartera: List<Pair<Apartamento, PagoDTO?>>) :
    RecyclerView.Adapter<CarteraAdminAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemCarteraAdminBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCarteraAdminBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (apto, pago) = listaCartera[position]

        // Mostramos la torre y el apartamento de forma clara
        holder.binding.tvNumeroApto.text = "Torre ${apto.torre} - Apto ${apto.apto}"

        // Usamos las observaciones del pago si existen, de lo contrario un texto por defecto
        holder.binding.tvPropietarioApto.text = if (!pago?.observaciones.isNullOrBlank()) {
            pago?.observaciones
        } else {
            "ID de Cuenta: ${apto.id}"
        }

        // Si no se encuentra registro de pago, asumimos por seguridad que está AL DÍA (saldo 0)
        val saldo = pago?.saldoPendiente ?: 0.0
        val estado = pago?.estadoCuenta ?: "AL DIA"
        val estaAlDia = saldo <= 0.0 || estado.equals("AL DIA", ignoreCase = true)

        if (estaAlDia) {
            holder.binding.tvEstadoPago.text = "AL DÍA"
            holder.binding.tvEstadoPago.setTextColor(Color.parseColor("#2E7D32"))
            holder.binding.cardEstadoPago.setCardBackgroundColor(Color.parseColor("#E8F5E9"))
        } else {
            holder.binding.tvEstadoPago.text = "DEBE $${saldo}"
            holder.binding.tvEstadoPago.setTextColor(Color.parseColor("#C62828"))
            holder.binding.cardEstadoPago.setCardBackgroundColor(Color.parseColor("#FFEBEE"))
        }
    }

    override fun getItemCount(): Int = listaCartera.size
}