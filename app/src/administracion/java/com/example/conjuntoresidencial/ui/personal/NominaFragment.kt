package com.example.conjuntoresidencial.ui.personal

import android.os.Bundle
import android.text.InputType
import android.widget.LinearLayout
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.api.instance
import com.example.conjuntoresidencial.model.CambioNomina
import com.example.conjuntoresidencial.model.NominaDTO
import com.example.conjuntoresidencial.ui.comun.Bloques
import com.example.conjuntoresidencial.ui.comun.Bloques.textoLimpio
import com.example.conjuntoresidencial.ui.comun.TonoEstado
import com.example.conjuntoresidencial.ui.comun.confirmar
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.util.Formatos
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.YearMonth

/**
 * P11 bloque 8: la nómina de un mes: generarla (una fila por trabajador activo con su salario),
 * ajustar bonificaciones y descuentos de cada uno, marcar pagada una o todas, y los totales.
 */
class NominaFragment : PantallaHoja() {
    override val titulo = R.string.pers_nomina
    override val descripcion = R.string.pers_nomina_sub

    private var periodo: String = YearMonth.now().toString()
    private var filas: List<NominaDTO> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.getString(PERIODO)?.let { periodo = it }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(PERIODO, periodo)
    }

    override suspend fun traer(): String? {
        val r = RetrofitClient.instance.getNomina(periodo)
        filas = r.body().orEmpty()
        return mensajeDe(r)
    }

    override fun pintar() {
        val porPagar = filas.count { it.estado != "PAGADA" }
        binding.encabezado.ponerInfo(if (filas.isEmpty()) getString(R.string.pers_sin_generar)
            else resources.getQuantityString(R.plurals.pers_por_pagar_n, porPagar, porPagar))
        Personal.selector(hoja, Personal.mes(periodo), getString(R.string.pers_mes_anterior), getString(R.string.pers_mes_siguiente)) { paso ->
            periodo = YearMonth.parse(periodo).plusMonths(paso.toLong()).toString(); recargar()
        }
        Bloques.cifras(hoja, Formatos.moneda(filas.sumOf { it.neto }) to getString(R.string.pers_total_neto),
            filas.size.toString() to getString(R.string.pers_trabajadores))
        Bloques.cifras(hoja, Formatos.moneda(filas.sumOf { it.bonificaciones }) to getString(R.string.pers_bonificaciones),
            Formatos.moneda(filas.sumOf { it.descuentos }) to getString(R.string.pers_descuentos))
        Bloques.boton(hoja, getString(if (filas.isEmpty()) R.string.pers_generar else R.string.pers_generar_faltantes), icono = R.drawable.ic_add) {
            Personal.accion(this, R.string.pers_generada, { RetrofitClient.instance.generarNomina(periodo) }) { recargar() }
        }
        if (porPagar > 0) Bloques.boton(hoja, getString(R.string.pers_pagar_todo), relleno = false, icono = R.drawable.ic_check_circle) {
            confirmar(getString(R.string.pers_pagar_todo), getString(R.string.pers_pagar_todo_mensaje, porPagar, Personal.mes(periodo)), R.string.pers_pagar) {
                Personal.accion(this, R.string.pers_pagada_ok, { RetrofitClient.instance.pagarNomina(periodo) }) { recargar() }
            }
        }
        if (filas.isEmpty()) { Bloques.parrafo(hoja, getString(R.string.pers_nomina_vacia)); return }
        Bloques.seccion(hoja, getString(R.string.pers_por_trabajador))
        filas.forEach { n ->
            Bloques.tarjeta(hoja, n.trabajador, detalle = Personal.cargo(this, n.cargo) + " · " + getString(R.string.pers_neto, Formatos.moneda(n.neto)),
                extra = getString(R.string.pers_desglose, Formatos.moneda(n.salarioBase), Formatos.moneda(n.bonificaciones), Formatos.moneda(n.descuentos)),
                etiqueta = if (n.estado == "PAGADA") getString(R.string.pers_pagada) to TonoEstado.EXITO else getString(R.string.pers_por_pagar) to TonoEstado.ADVERTENCIA,
                alTocar = if (n.estado == "PAGADA") null else ({ ajustar(n) }))
        }
    }

    private fun ajustar(n: NominaDTO) {
        val c = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            val d = resources.displayMetrics.density
            setPadding((24 * d).toInt(), (4 * d).toInt(), (24 * d).toInt(), 0)
        }
        Bloques.parrafo(c, getString(R.string.pers_salario_base, Formatos.moneda(n.salarioBase)))
        val bon = Bloques.campo(c, getString(R.string.pers_bonificaciones), InputType.TYPE_CLASS_NUMBER, valor = n.bonificaciones.toLong().toString())
        val des = Bloques.campo(c, getString(R.string.pers_descuentos), InputType.TYPE_CLASS_NUMBER, valor = n.descuentos.toLong().toString())
        val pagada = com.google.android.material.materialswitch.MaterialSwitch(requireContext()).apply { setText(R.string.pers_marcar_pagada) }
        c.addView(pagada)
        val d = MaterialAlertDialogBuilder(requireContext())
            .setTitle(n.trabajador)
            .setView(c)
            .setPositiveButton(R.string.btn_guardar, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
        d.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val b = bon.textoLimpio.ifEmpty { "0" }.toDoubleOrNull()
            val s = des.textoLimpio.ifEmpty { "0" }.toDoubleOrNull()
            bon.error = if (b == null || b < 0) getString(R.string.pers_valor_invalido) else null
            des.error = if (s == null || s < 0) getString(R.string.pers_valor_invalido) else if (s > n.salarioBase + (b ?: 0.0)) getString(R.string.pers_descuento_alto) else null
            if (bon.error != null || des.error != null) return@setOnClickListener
            d.dismiss()
            Personal.accion(this, R.string.pers_guardado, {
                RetrofitClient.instance.ajustarNomina(n.id, CambioNomina(b, s, if (pagada.isChecked) "PAGADA" else null))
            }) { recargar() }
        }
    }

    companion object { private const val PERIODO = "periodo" }
}
