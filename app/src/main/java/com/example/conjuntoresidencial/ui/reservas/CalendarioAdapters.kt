package com.example.conjuntoresidencial.ui.reservas

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.ItemDiaCalendarioBinding
import com.example.conjuntoresidencial.databinding.ItemFranjaBinding
import com.example.conjuntoresidencial.ui.comun.EstadoVisual
import com.example.conjuntoresidencial.ui.comun.TonoEstado
import com.example.conjuntoresidencial.ui.comun.mostrarEstado
import com.example.conjuntoresidencial.util.Formatos
import com.google.android.material.color.MaterialColors

/** Una celda del calendario; null en los huecos antes del primer día del mes. */
data class DiaCalendario(
    val numero: Int,
    val fecha: String,
    val estado: Agenda.EstadoDia?,   // null mientras carga la disponibilidad
    val hoy: Boolean,
    val elegido: Boolean,
    val descripcion: String
) {
    val sePuedeElegir get() = estado == Agenda.EstadoDia.LIBRE || estado == Agenda.EstadoDia.PARCIAL
}

/** El color de cada estado del día (el mismo de las etiquetas de estado). */
fun Agenda.EstadoDia.tono(): TonoEstado = when (this) {
    Agenda.EstadoDia.LIBRE -> TonoEstado.EXITO
    Agenda.EstadoDia.PARCIAL -> TonoEstado.ADVERTENCIA
    Agenda.EstadoDia.LLENO -> TonoEstado.ERROR
    Agenda.EstadoDia.PASADO -> TonoEstado.NEUTRO
}

class DiaAdapter(private val onElegir: (DiaCalendario) -> Unit) :
    ListAdapter<DiaCalendario?, DiaAdapter.VH>(object : DiffUtil.ItemCallback<DiaCalendario?>() {
        override fun areItemsTheSame(a: DiaCalendario, b: DiaCalendario) = a.fecha == b.fecha
        override fun areContentsTheSame(a: DiaCalendario, b: DiaCalendario) = a == b
    }) {

    class VH(val binding: ItemDiaCalendarioBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemDiaCalendarioBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: VH, pos: Int) {
        val dia = getItem(pos)
        val celda = h.binding.celdaDia
        val numero = h.binding.tvNumeroDia
        if (dia == null) {
            numero.text = null
            numero.background = null
            celda.isClickable = false
            celda.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            return
        }
        val ctx = celda.context
        numero.text = dia.numero.toString()
        numero.setTypeface(null, if (dia.hoy) Typeface.BOLD else Typeface.NORMAL)
        val tono = dia.estado?.tono()
        val conColor = tono != null && dia.estado != Agenda.EstadoDia.PASADO
        numero.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(if (conColor) ContextCompat.getColor(ctx, tono!!.fondo) else 0)
            if (dia.elegido) {
                setStroke((3 * ctx.resources.displayMetrics.density).toInt(),
                    MaterialColors.getColor(numero, androidx.appcompat.R.attr.colorPrimary))
            }
        }
        numero.setTextColor(
            if (conColor) ContextCompat.getColor(ctx, tono!!.texto)
            else MaterialColors.getColor(numero, com.google.android.material.R.attr.colorOnSurfaceVariant)
        )
        numero.alpha = if (dia.estado == Agenda.EstadoDia.PASADO) 0.45f else 1f
        celda.isClickable = dia.sePuedeElegir
        celda.isEnabled = dia.sePuedeElegir
        celda.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        celda.contentDescription = dia.descripcion
        celda.isSelected = dia.elegido
        celda.setOnClickListener { if (dia.sePuedeElegir) onElegir(dia) }
    }
}

class FranjaAdapter(private val onTocar: (Int) -> Unit) : RecyclerView.Adapter<FranjaAdapter.VH>() {

    class VH(val binding: ItemFranjaBinding) : RecyclerView.ViewHolder(binding.root)

    private var franjas: List<Agenda.Franja> = emptyList()
    private var seleccion: IntRange? = null

    fun mostrar(nuevas: List<Agenda.Franja>, elegidas: IntRange?) {
        franjas = nuevas
        seleccion = elegidas
        @Suppress("NotifyDataSetChanged") // pocas franjas y cambia la selección de varias a la vez
        notifyDataSetChanged()
    }

    override fun getItemCount() = franjas.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemFranjaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: VH, pos: Int) {
        val franja = franjas[pos]
        val ctx = h.binding.root.context
        val elegida = seleccion?.contains(pos) == true
        val horario = ctx.getString(R.string.reservar_franja,
            Formatos.hora(Agenda.hora(franja.inicio)), Formatos.hora(Agenda.hora(franja.fin % Agenda.DIA_COMPLETO)))
        h.binding.tvHoraFranja.text = horario
        val estado = when {
            elegida -> EstadoVisual(R.string.franja_elegida, TonoEstado.EXITO)
            else -> when (franja.estado) {
                Agenda.EstadoFranja.LIBRE -> EstadoVisual(R.string.franja_libre, TonoEstado.EXITO)
                Agenda.EstadoFranja.OCUPADA -> EstadoVisual(R.string.franja_ocupada, TonoEstado.ERROR)
                Agenda.EstadoFranja.PROPIA -> EstadoVisual(R.string.franja_propia, TonoEstado.ADVERTENCIA)
                Agenda.EstadoFranja.PASADA -> EstadoVisual(R.string.franja_pasada, TonoEstado.NEUTRO)
            }
        }
        h.binding.tvEstadoFranja.mostrarEstado(estado)
        val tarjeta = h.binding.root
        tarjeta.setCardBackgroundColor(MaterialColors.getColor(tarjeta,
            if (elegida) com.google.android.material.R.attr.colorPrimaryContainer
            else com.google.android.material.R.attr.colorSurface))
        tarjeta.strokeColor = MaterialColors.getColor(tarjeta,
            if (elegida) androidx.appcompat.R.attr.colorPrimary else com.google.android.material.R.attr.colorOutlineVariant)
        tarjeta.alpha = if (franja.libre) 1f else 0.6f
        tarjeta.isEnabled = franja.libre
        // TalkBack: "9:00 a. m. a 10:00 a. m., Libre" (y "Elegida" si lo está)
        tarjeta.contentDescription = listOf(horario, ctx.getString(estado.etiqueta)).joinToString(", ")
        tarjeta.setOnClickListener { onTocar(pos) }
    }
}
