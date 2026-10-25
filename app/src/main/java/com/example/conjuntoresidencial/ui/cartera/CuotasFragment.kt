package com.example.conjuntoresidencial.ui.cartera

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.FragmentCuotasBinding
import com.example.conjuntoresidencial.model.CuotaDTO
import com.example.conjuntoresidencial.model.GenerarCuotasRequest
import com.example.conjuntoresidencial.model.RevisionCuota
import com.example.conjuntoresidencial.ui.comun.accionesDeLista
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.controlesFlotantes
import com.example.conjuntoresidencial.ui.comun.mostrarRecurso
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.ui.comun.resumen
import com.example.conjuntoresidencial.util.Formatos
import com.example.conjuntoresidencial.util.comoLista
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * Facturación (contador y administrador, P4 bloque 7): los comprobantes por revisar o las
 * cuotas de un mes. El botón genera la cuota del mes para todos los apartamentos; tocar una
 * cuota deja ver el comprobante, confirmar el pago o rechazarlo con un motivo.
 */
class CuotasFragment : Fragment() {
    private var _binding: FragmentCuotasBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CuotasViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCuotasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(
            binding.encabezado, R.string.menu_facturacion, R.string.facturacion_subtitulo, menu = R.menu.menu_lista,
            alElegir = accionesDeLista(binding.swipe) { viewModel.recargar() }
        )

        val adaptador = CuotaAdapter(conApartamento = true) { revisar(it) }
        binding.rvCuotas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCuotas.adapter = adaptador
        binding.estado.vincular(binding.rvCuotas)
        binding.swipe.setOnRefreshListener { viewModel.recargar() }
        ponerFiltros()

        controlesFlotantes?.mostrarBotonFlotante(R.string.facturacion_generar, R.drawable.ic_add) { generar() }?.let { boton ->
            binding.rvCuotas.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                    if (dy > 0) boton.shrink() else if (dy < 0) boton.extend()
                }
            })
        }

        viewModel.cuotas.observe(viewLifecycleOwner) { resource ->
            val porRevisar = viewModel.periodo == null
            binding.estado.mostrarRecurso(
                resource, binding.swipe, R.drawable.ic_wallet,
                getString(if (porRevisar) R.string.facturacion_sin_revisar else R.string.facturacion_sin_cuotas),
                getString(if (porRevisar) R.string.facturacion_sin_revisar_mensaje else R.string.facturacion_sin_cuotas_mensaje),
                reintentar = { viewModel.recargar() }
            ) { lista ->
                adaptador.submitList(lista)
                binding.encabezado.ponerInfo(resumen(
                    R.plurals.resumen_cuotas, lista.size,
                    R.plurals.resumen_pagadas to lista.count { it.estado == Cuotas.PAGADA }
                ))
            }
        }
        viewModel.mensaje.observe(viewLifecycleOwner) { evento ->
            evento.obtenerSiNoManejado()?.let { aviso(it, Snackbar.LENGTH_LONG).show() }
        }

        if (viewModel.cuotas.value == null) viewModel.filtrar(null)
    }

    /** "Por revisar" y los meses cercanos (el siguiente, por si ya se facturó). */
    private fun ponerFiltros() {
        val opciones = listOf<Pair<String, String?>>(getString(R.string.facturacion_por_revisar) to null) +
            listOf(1, 0, -1, -2).map { Cuotas.periodo(it) }.map { Cuotas.nombreMes(it) to it }
        binding.chipsFiltro.removeAllViews()
        opciones.forEach { (texto, periodo) ->
            val chip = Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle).apply {
                text = texto
                isCheckable = true
                isChecked = periodo == viewModel.periodo
                setOnClickListener { if (periodo != viewModel.periodo) viewModel.filtrar(periodo) }
            }
            binding.chipsFiltro.addView(chip)
        }
    }

    private fun generar() {
        val vista = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_generar_cuota, null)
        val periodos = listOf(0, 1, -1).map { Cuotas.periodo(it) }
        val mes = vista.findViewById<AutoCompleteTextView>(R.id.etMesCuota).apply { comoLista(periodos.map(Cuotas::nombreMes)) }
        val tilValor = vista.findViewById<TextInputLayout>(R.id.tilValorCuota)
        val tilDia = vista.findViewById<TextInputLayout>(R.id.tilDiaLimite)
        val dialogo = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.facturacion_generar)
            .setView(vista)
            .setPositiveButton(R.string.facturacion_generar_si, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
        // Después de show() para que no se cierre si falta un dato
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val periodo = periodos[periodos.map(Cuotas::nombreMes).indexOf(mes.text.toString()).coerceAtLeast(0)]
            val valor = vista.findViewById<TextInputEditText>(R.id.etValorCuota).text?.toString()?.toDoubleOrNull()
            val dia = vista.findViewById<TextInputEditText>(R.id.etDiaLimite).text?.toString()?.toIntOrNull()
            val maximo = Cuotas.diasDelMes(periodo)
            tilValor.error = if (valor == null || valor <= 0) getString(R.string.cuota_valor_obligatorio) else null
            tilDia.error = if (dia == null || dia !in 1..maximo) getString(R.string.cuota_dia_invalido, maximo) else null
            if (tilValor.error != null || tilDia.error != null) return@setOnClickListener
            dialogo.dismiss()
            val limite = "$periodo-${dia.toString().padStart(2, '0')}"
            viewModel.ejecutar(getString(R.string.facturacion_error_generar),
                { RetrofitClient.instance.generarCuotas(GenerarCuotasRequest(periodo, valor!!, limite)) }) { g ->
                if (g == null) getString(R.string.facturacion_generada_sin_datos)
                else resources.getQuantityString(R.plurals.facturacion_generadas, g.creadas, g.creadas, Cuotas.nombreMes(g.periodo))
            }
            // Muestra el mes recién generado
            viewModel.filtrar(periodo)
            ponerFiltros()
        }
    }

    private fun revisar(c: CuotaDTO) {
        val mensaje = listOfNotNull(
            Cuotas.detalle(requireContext(), c),
            getString(Cuotas.estado(c).etiqueta),
            c.comprobanteSubidoEn?.let { getString(R.string.cuota_comprobante_subido, Formatos.fecha(it)) }
        ).joinToString("\n")
        val dialogo = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.cuota_titulo_apto, c.torre, c.apto, Cuotas.nombreMes(c.periodo)))
            .setMessage(mensaje)
        if (c.estado != Cuotas.PAGADA) {
            dialogo.setPositiveButton(R.string.cuota_confirmar_pago) { _, _ ->
                viewModel.ejecutar(getString(R.string.cuota_error_revisar),
                    { RetrofitClient.instance.revisarCuota(c.id, RevisionCuota(Cuotas.PAGADA)) }) {
                    getString(R.string.cuota_pago_confirmado)
                }
            }
        }
        if (c.estado == Cuotas.EN_REVISION) dialogo.setNegativeButton(R.string.cuota_rechazar) { _, _ -> pedirMotivo(c) }
        if (c.tieneComprobante == true) dialogo.setNeutralButton(R.string.cuota_ver_comprobante) { _, _ -> abrirComprobante(c) }
        dialogo.show()
    }

    private fun pedirMotivo(c: CuotaDTO) {
        val campo = TextInputEditText(requireContext())
        val til = TextInputLayout(requireContext()).apply {
            hint = getString(R.string.cuota_motivo_hint)
            addView(campo)
            val margen = (24 * resources.displayMetrics.density).toInt()
            setPadding(margen, margen / 2, margen, 0)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.cuota_rechazar_titulo)
            .setView(til)
            .setPositiveButton(R.string.cuota_rechazar) { _, _ ->
                val motivo = campo.text?.toString()?.trim()?.ifBlank { null }
                viewModel.ejecutar(getString(R.string.cuota_error_revisar),
                    { RetrofitClient.instance.revisarCuota(c.id, RevisionCuota(Cuotas.RECHAZADA, motivo)) }) {
                    getString(R.string.cuota_comprobante_rechazado)
                }
            }
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
