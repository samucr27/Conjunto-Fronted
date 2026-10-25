package com.example.conjuntoresidencial.ui.cartera

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.ItemCuotaBinding
import com.example.conjuntoresidencial.model.CuotaDTO
import com.example.conjuntoresidencial.ui.comun.DiffPorId
import com.example.conjuntoresidencial.ui.comun.EstadoVisual
import com.example.conjuntoresidencial.ui.comun.TonoEstado
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.mostrarEstado
import com.example.conjuntoresidencial.util.Formatos
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Lo común de las cuotas mensuales (P4 bloque 7): nombres de mes, estado y comprobante. */
object Cuotas {
    const val PENDIENTE = "PENDIENTE"
    const val EN_REVISION = "EN_REVISION"
    const val PAGADA = "PAGADA"
    const val RECHAZADA = "RECHAZADA"

    private val colombia = Locale.forLanguageTag("es-CO")

    /** "2026-11" → "Noviembre 2026". */
    fun nombreMes(periodo: String): String = try {
        val fecha = SimpleDateFormat("yyyy-MM", Locale.US).apply { isLenient = false }.parse(periodo)
        SimpleDateFormat("MMMM yyyy", colombia).format(fecha!!).replaceFirstChar { it.titlecase(colombia) }
    } catch (_: Exception) {
        periodo
    }

    /** El periodo ("2026-11") del mes actual movido [meses]. */
    fun periodo(meses: Int): String {
        val c = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); add(Calendar.MONTH, meses) }
        return String.format(Locale.US, "%04d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1)
    }

    /** Último día del mes de [periodo]. */
    fun diasDelMes(periodo: String): Int {
        val (anio, mes) = periodo.split("-").map { it.toInt() }
        return Calendar.getInstance().apply { clear(); set(anio, mes - 1, 1) }.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun estado(c: CuotaDTO): EstadoVisual = when (c.estado) {
        PAGADA -> EstadoVisual(R.string.cuota_pagada, TonoEstado.EXITO)
        EN_REVISION -> EstadoVisual(R.string.cuota_en_revision, TonoEstado.ADVERTENCIA)
        RECHAZADA -> EstadoVisual(R.string.cuota_rechazada, TonoEstado.ERROR)
        else -> if (c.vencida == true) EstadoVisual(R.string.cuota_vencida, TonoEstado.ERROR)
        else EstadoVisual(R.string.cuota_pendiente, TonoEstado.ADVERTENCIA)
    }

    /** "$175.000 · vence el 30/11/2026" y, si lo rechazaron, el motivo. */
    fun detalle(context: Context, c: CuotaDTO): String = listOfNotNull(
        context.getString(R.string.cuota_detalle, Formatos.moneda(c.valor), Formatos.fecha(c.fechaLimite)),
        c.motivoRechazo?.takeIf { c.estado == RECHAZADA }?.let { context.getString(R.string.cuota_motivo, it) }
    ).joinToString("\n")
}

/** Pide el enlace firmado (10 minutos) y abre el comprobante en el navegador o el visor de PDF. */
fun Fragment.abrirComprobante(cuota: CuotaDTO) {
    viewLifecycleOwner.lifecycleScope.launch {
        try {
            val r = RetrofitClient.instance.getComprobante(cuota.id)
            val enlace = r.body()
            if (!r.isSuccessful || enlace == null) {
                aviso(RetrofitClient.mensajeDeError(r, getString(R.string.cuota_error_comprobante)), Snackbar.LENGTH_LONG).show()
                return@launch
            }
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(enlace.url)))
        } catch (_: ActivityNotFoundException) {
            aviso(getString(R.string.cuota_sin_visor), Snackbar.LENGTH_LONG).show()
        } catch (e: Exception) {
            aviso(RetrofitClient.mensajeDeExcepcion(e), Snackbar.LENGTH_LONG).show()
        }
    }
}

/** Cuotas: mes (con torre y apto si [conApartamento]), valor, vencimiento y estado. */
class CuotaAdapter(
    private val conApartamento: Boolean,
    private val onTocar: (CuotaDTO) -> Unit
) : ListAdapter<CuotaDTO, CuotaAdapter.VH>(DiffPorId { it.id }) {

    class VH(val binding: ItemCuotaBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemCuotaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: VH, position: Int) {
        val c = getItem(position)
        val ctx = h.binding.root.context
        val mes = Cuotas.nombreMes(c.periodo)
        h.binding.tvTituloCuota.text =
            if (conApartamento) ctx.getString(R.string.cuota_titulo_apto, c.torre, c.apto, mes) else mes
        h.binding.tvDetalleCuota.text = Cuotas.detalle(ctx, c)
        h.binding.tvEstadoCuota.mostrarEstado(Cuotas.estado(c))
        h.binding.root.setOnClickListener { onTocar(c) }
    }
}
