package com.example.conjuntoresidencial.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.databinding.ItemPersonalBinding
import com.example.conjuntoresidencial.model.PersonalObraDTO

class PersonalAdapter(private val lista: List<PersonalObraDTO>) :
    RecyclerView.Adapter<PersonalAdapter.VH>() {

    class VH(val binding: ItemPersonalBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemPersonalBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: VH, pos: Int) {
        val p = lista[pos]
        h.binding.tvNombrePersonal.text = p.nombreTrabajador
        h.binding.tvCedulaPersonal.text = "CC: ${p.cedula}"
        h.binding.tvArlPersonal.text = if (p.arlEstado) "✓ ARL vigente" else "✗ ARL no vigente"
        if (!p.torrePiso.isNullOrBlank() && !p.aptoNum.isNullOrBlank()) {
            h.binding.tvUbicacionPersonal.text = "Personal externo — Torre ${p.torrePiso} Apto ${p.aptoNum}"
            h.binding.tvUbicacionPersonal.visibility = android.view.View.VISIBLE
        } else {
            h.binding.tvUbicacionPersonal.visibility = android.view.View.GONE
        }
    }

    override fun getItemCount() = lista.size
}