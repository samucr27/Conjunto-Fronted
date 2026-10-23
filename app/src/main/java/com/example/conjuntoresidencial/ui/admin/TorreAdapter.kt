package com.example.conjuntoresidencial.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.ItemApartamentoBinding
import com.example.conjuntoresidencial.ui.comun.DiffPorId

/** Una torre del directorio con cuántos apartamentos tiene. */
data class Torre(val nombre: String, val apartamentos: Int)

/** Lista de torres del directorio; usa la misma tarjeta que los apartamentos. */
class TorreAdapter(
    private val onClick: (Torre) -> Unit
) : ListAdapter<Torre, TorreAdapter.ViewHolder>(DiffPorId { it.nombre }) {

    class ViewHolder(val binding: ItemApartamentoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemApartamentoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val torre = getItem(position)
        val contexto = holder.binding.root.context
        holder.binding.tvTorreApto.text = contexto.getString(R.string.directorio_torre_titulo, torre.nombre)
        holder.binding.tvDetalleApto.text = contexto.resources.getQuantityString(
            R.plurals.resumen_apartamentos, torre.apartamentos, torre.apartamentos
        )
        holder.binding.root.setOnClickListener { onClick(torre) }
    }
}
