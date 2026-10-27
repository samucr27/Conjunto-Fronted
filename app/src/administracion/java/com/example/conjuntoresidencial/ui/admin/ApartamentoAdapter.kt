package com.example.conjuntoresidencial.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.ItemApartamentoBinding
import com.example.conjuntoresidencial.model.Apartamento
import com.example.conjuntoresidencial.ui.comun.DiffPorId

/**
 * "Vive el propietario", "Arrendado" o "Desocupado" según lo que fijó la administración; si el
 * servidor aún no lo manda, se deduce de si hay arrendatario activo (null = sin dato).
 */
fun textoOcupacion(contexto: android.content.Context, tipo: String?, arrendado: Boolean): String? = when {
    tipo == "ARRENDATARIO" || (tipo == null && arrendado) -> contexto.getString(R.string.ocupacion_arrendado)
    tipo == "DESOCUPADO" -> contexto.getString(R.string.ocupacion_desocupado)
    tipo == "PROPIETARIO" -> contexto.getString(R.string.ocupacion_propietario)
    else -> null
}

/** @param soloNumero dentro de una torre: "Apartamento 101" en vez de "Torre 1 · Apto 101" */
class ApartamentoAdapter(
    private val soloNumero: Boolean = false,
    private val onClick: (Apartamento) -> Unit
) : ListAdapter<Apartamento, ApartamentoAdapter.ViewHolder>(DiffPorId { it.id }) {

    class ViewHolder(val binding: ItemApartamentoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemApartamentoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val apto = getItem(position)
        val contexto = holder.binding.root.context
        holder.binding.tvTorreApto.text =
            if (soloNumero) contexto.getString(R.string.apto_item_solo, apto.apto)
            else contexto.getString(R.string.apto_item_titulo, apto.torre, apto.apto)
        val ocupacion = textoOcupacion(contexto, apto.tipoOcupacion, apto.arrendado == true)
        if (ocupacion == null) {
            holder.binding.tvDetalleApto.setText(R.string.apto_item_detalle)
        } else {
            holder.binding.tvDetalleApto.text = contexto.getString(R.string.apto_item_detalle_ocupacion, ocupacion)
        }
        holder.binding.root.setOnClickListener { onClick(apto) }
    }
}
