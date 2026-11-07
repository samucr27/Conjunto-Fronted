package com.example.conjuntoresidencial.ui.contabilidad

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.api.instance
import com.example.conjuntoresidencial.databinding.FragmentPantallaContenidoBinding
import com.example.conjuntoresidencial.model.CarteraApartamentoDTO
import com.example.conjuntoresidencial.model.CoeficienteDTO
import com.example.conjuntoresidencial.model.CuotaDTO
import com.example.conjuntoresidencial.ui.comun.Bloques
import com.example.conjuntoresidencial.ui.comun.Bloques.textoLimpio
import com.example.conjuntoresidencial.ui.comun.TonoEstado
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.ejecutarAccion
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.ui.comun.ponerTitulo
import com.example.conjuntoresidencial.util.Formatos
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * P11 bloque 6: la cartera de un apartamento para el contador: saldo, coeficiente, sus cuotas (con
 * concepto y referencia de pago) y lo que tiene esperando al administrador. Desde aquí se pide una
 * cuota nueva, un cambio, anular una cuota o registrar un pago hecho en la oficina.
 */
class CarteraApartamentoFragment : Fragment() {

    private var _binding: FragmentPantallaContenidoBinding? = null
    private val binding get() = _binding!!
    private val aptoId: Long get() = requireArguments().getLong(FormulariosCartera.ARG_APARTAMENTO)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPantallaContenidoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(binding.encabezado, R.string.cont_apto_titulo, R.string.cont_apto_sub)
        binding.swipe.setOnRefreshListener { cargar() }
        binding.estado.vincular(binding.desplazar)
        cargar()
    }

    private fun cargar() {
        if (!binding.swipe.isRefreshing) binding.estado.mostrarCargando()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.carteraDeApartamento(aptoId)
                if (_binding == null) return@launch
                binding.swipe.isRefreshing = false
                val d = r.body()
                if (!r.isSuccessful || d == null) {
                    binding.estado.mostrarError(RetrofitClient.mensajeDeError(r, getString(R.string.cont_error_cargar))) { cargar() }
                    return@launch
                }
                pintar(d)
                binding.estado.mostrarContenido()
            } catch (e: Exception) {
                if (_binding == null) return@launch
                binding.swipe.isRefreshing = false
                binding.estado.mostrarError(RetrofitClient.mensajeDeExcepcion(e)) { cargar() }
            }
        }
    }

    private fun pintar(d: CarteraApartamentoDTO) {
        val a = d.resumen
        val c = binding.contenido
        c.removeAllViews()
        binding.encabezado.ponerTitulo(getString(R.string.cont_torre_apto, a.torre, a.apto))
        binding.encabezado.ponerInfo(if (a.vencidas > 0) getString(R.string.cont_en_mora) else getString(R.string.cont_al_dia))

        Bloques.cifras(c, Formatos.moneda(a.saldo) to getString(R.string.cont_saldo),
            a.pendientes.toString() to getString(R.string.cont_pendientes), a.vencidas.toString() to getString(R.string.cont_vencidas))
        val coef = Bloques.tarjeta(c, getString(R.string.cont_coeficiente),
            detalle = a.coeficiente?.toString() ?: getString(R.string.cont_sin_coeficiente))
        Bloques.boton(coef, getString(R.string.cont_cambiar_coeficiente), relleno = false) { cambiarCoeficiente(a.coeficiente) }

        Bloques.boton(c, getString(R.string.cont_nueva_cuota), icono = R.drawable.ic_add) {
            FormulariosCartera.cuotaNueva(this, aptoId) { cargar() }
        }

        Bloques.seccion(c, getString(R.string.cont_cuotas))
        if (d.cuotas.isEmpty()) Bloques.parrafo(c, getString(R.string.cont_sin_cuotas))
        d.cuotas.forEach { q ->
            val etiqueta = when {
                q.estado == "PAGADA" -> getString(R.string.cont_pagada) to TonoEstado.EXITO
                q.pagoPorAprobar == true -> getString(R.string.cont_pago_por_aprobar) to TonoEstado.ADVERTENCIA
                q.estado == "EN_REVISION" -> getString(R.string.cont_en_revision) to TonoEstado.ADVERTENCIA
                q.vencida == true -> getString(R.string.cont_vencida) to TonoEstado.ERROR
                else -> getString(R.string.cont_por_pagar) to TonoEstado.NEUTRO
            }
            val pagada = q.estado == "PAGADA"
            val extra = listOfNotNull(
                q.referenciaPago?.let { getString(R.string.cont_ref_linea, it) },
                if (pagada && q.medioPago != null) getString(R.string.cont_pagada_con, q.medioPago.lowercase(), q.pagoReferencia.orEmpty()).trim() else null,
                q.descripcion
            ).joinToString("\n").ifEmpty { null }
            Bloques.tarjeta(c, getString(R.string.cont_cuota_titulo, q.concepto ?: "Administración", com.example.conjuntoresidencial.ui.cartera.Cuotas.nombreMes(q.periodo)),
                detalle = getString(R.string.cont_cuota_detalle, Formatos.moneda(q.valor), Formatos.fecha(q.fechaLimite)),
                extra = extra, etiqueta = etiqueta, alTocar = if (pagada) null else ({ opciones(q) }))
        }

        if (d.solicitudes.isNotEmpty()) {
            Bloques.seccion(c, getString(R.string.cont_solicitudes_apto))
            d.solicitudes.forEach { s ->
                Bloques.tarjeta(c, s.resumen ?: TextosContables.tipo(this, s.tipo), detalle = TextosContables.quien(this, s),
                    etiqueta = TextosContables.estado(this, s)) {
                    findNavController().navigate(R.id.solicitudDetalleFragment, bundleOf(FormulariosCartera.ARG_SOLICITUD to s.id))
                }
            }
        }
    }

    private fun opciones(q: CuotaDTO) {
        val opciones = mutableListOf<Pair<Int, () -> Unit>>()
        if (q.pagoPorAprobar != true) {
            opciones += R.string.cont_registrar_pago to { FormulariosCartera.pago(this, q, q.id) { cargar() } }
        }
        opciones += R.string.cont_modificar_cuota to { FormulariosCartera.cambio(this, q, q.id) { cargar() } }
        opciones += R.string.cont_anular_cuota to { FormulariosCartera.anular(this, q, q.id) { cargar() } }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.cont_cuota_titulo, q.concepto ?: "Administración", com.example.conjuntoresidencial.ui.cartera.Cuotas.nombreMes(q.periodo)))
            .setItems(opciones.map { getString(it.first) }.toTypedArray()) { _, i -> opciones[i].second() }
            .show()
    }

    /** El coeficiente se cambia directo (no mueve dinero); vacío lo quita. */
    private fun cambiarCoeficiente(actual: Double?) {
        val caja = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            val dp = resources.displayMetrics.density
            setPadding((24 * dp).toInt(), (4 * dp).toInt(), (24 * dp).toInt(), 0)
        }
        val campo = Bloques.campo(caja, getString(R.string.cont_coeficiente),
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL, getString(R.string.cont_coeficiente_ayuda), actual?.toString())
        val dialogo = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.cont_cambiar_coeficiente)
            .setView(caja)
            .setPositiveButton(R.string.btn_guardar, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
        dialogo.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val texto = campo.textoLimpio.replace(",", ".")
            val valor = if (texto.isEmpty()) null else texto.toDoubleOrNull()
            if (texto.isNotEmpty() && (valor == null || valor < 0 || valor > 100)) {
                campo.error = getString(R.string.cont_coeficiente_invalido); return@setOnClickListener
            }
            dialogo.dismiss()
            ejecutarAccion(getString(R.string.cont_coeficiente_guardado), { RetrofitClient.instance.ponerCoeficiente(aptoId, CoeficienteDTO(valor)) }) { cargar() }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
