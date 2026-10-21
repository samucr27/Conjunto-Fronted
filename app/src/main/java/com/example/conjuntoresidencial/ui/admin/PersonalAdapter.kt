package com.example.conjuntoresidencial.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.ItemPersonalBinding
import com.example.conjuntoresidencial.model.PersonalObraDTO
import com.example.conjuntoresidencial.ui.comun.DiffPorId
import com.example.conjuntoresidencial.ui.comun.Estados
import com.example.conjuntoresidencial.ui.comun.mostrarEstado

class PersonalAdapter : ListAdapter<PersonalObraDTO, PersonalAdapter.VH>(DiffPorId { it.id }) {

    class VH(val binding: ItemPersonalBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemPersonalBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: VH, pos: Int) {
        val p = getItem(pos)
        h.binding.tvNombrePersonal.text = p.nombreTrabajador
        val ctx = h.binding.root.context
        h.binding.tvCedulaPersonal.text = ctx.getString(R.string.personal_cedula, p.cedula)
        h.binding.tvArlPersonal.mostrarEstado(Estados.arl(p.arlEstado))
        if (!p.torrePiso.isNullOrBlank() && !p.aptoNum.isNullOrBlank()) {
            h.binding.tvUbicacionPersonal.text = ctx.getString(R.string.personal_ubicacion, p.torrePiso, p.aptoNum)
            h.binding.tvUbicacionPersonal.visibility = View.VISIBLE
        } else {
            h.binding.tvUbicacionPersonal.visibility = View.GONE
        }
    }
}
