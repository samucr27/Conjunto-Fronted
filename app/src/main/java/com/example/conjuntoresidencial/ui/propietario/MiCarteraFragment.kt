package com.example.conjuntoresidencial.ui.propietario

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FragmentMiCarteraBinding
import com.example.conjuntoresidencial.model.PagoDTO
import com.example.conjuntoresidencial.ui.comun.Estados
import com.example.conjuntoresidencial.ui.comun.mostrarEstado
import com.example.conjuntoresidencial.util.Formatos
import com.example.conjuntoresidencial.util.Resource
import com.example.conjuntoresidencial.util.SessionManager
import com.example.conjuntoresidencial.ui.comun.accionesDeLista
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.ui.comun.aviso
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class MiCarteraFragment : Fragment() {

    private var _binding: FragmentMiCarteraBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MiCarteraViewModel by viewModels()
    private var aptoId: Long = -1L

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMiCarteraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Pestaña de la barra inferior: sin botón para volver
        configurarEncabezado(
            binding.encabezado, R.string.mi_cartera_titulo, R.string.mi_cartera_subtitulo,
            atras = false, menu = R.menu.menu_lista, alElegir = accionesDeLista(binding.swipe) { cargar() }
        )
        aptoId = SessionManager(requireContext()).getApartamentoId()
        binding.estado.vincular(binding.contenido)
        binding.swipe.setOnRefreshListener { cargar() }

        viewModel.pagoState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> if (!binding.swipe.isRefreshing) binding.estado.mostrarCargando()
                is Resource.Success -> {
                    binding.swipe.isRefreshing = false
                    val pago = resource.data
                    if (pago == null) {
                        binding.estado.mostrarVacio(
                            R.drawable.ic_wallet, getString(R.string.mi_cartera_sin_registro),
                            getString(R.string.mi_cartera_sin_registro_mensaje)
                        )
                    } else {
                        mostrarPago(pago)
                        binding.estado.mostrarContenido()
                    }
                }
                is Resource.Error -> {
                    binding.swipe.isRefreshing = false
                    binding.estado.mostrarError(resource.message) { cargar() }
                }
            }
        }

        viewModel.simulacion.observe(viewLifecycleOwner) { evento ->
            evento.obtenerSiNoManejado()?.let { error ->
                aviso(error ?: getString(R.string.simulacion_exitosa), Snackbar.LENGTH_LONG).show()
            }
        }

        if (viewModel.pagoState.value == null) cargar()
    }

    private fun cargar() = viewModel.fetchPagos(aptoId, getString(R.string.mi_cartera_error))

    private fun mostrarPago(pago: PagoDTO) {
        val saldo = Formatos.moneda(pago.saldoPendiente)
        binding.tvSaldoPendiente.text = saldo
        val estado = Estados.cuenta(pago.saldoPendiente, pago.estadoCuenta)
        binding.tvEstadoCuenta.mostrarEstado(estado)
        // Resumen del encabezado: "Al día" o "Debe $350.000"
        binding.encabezado.ponerInfo(
            if (estado.argumento != null) getString(estado.etiqueta, estado.argumento) else getString(estado.etiqueta)
        )
        // Puede venir null del backend aunque el modelo diga String
        val observaciones: String? = pago.observaciones
        binding.tvObservacionesCartera.text = observaciones
        binding.cardObservaciones.visibility = if (observaciones.isNullOrBlank()) View.GONE else View.VISIBLE

        // Sin deuda el botón queda deshabilitado con su propio texto (colores del tema, no fijos)
        val debe = pago.saldoPendiente > 0
        binding.btnSimularPago.isEnabled = debe
        binding.btnSimularPago.setText(if (debe) R.string.btn_simular_pago else R.string.btn_sin_deuda)
        binding.btnSimularPago.setOnClickListener { confirmarPago(saldo) }
    }

    private fun confirmarPago(saldo: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.mi_cartera_pagar_titulo)
            .setMessage(getString(R.string.mi_cartera_pagar_pregunta, saldo))
            .setPositiveButton(R.string.mi_cartera_pagar_si) { _, _ ->
                viewModel.simularPago(aptoId, getString(R.string.mi_cartera_error_pago))
            }
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
