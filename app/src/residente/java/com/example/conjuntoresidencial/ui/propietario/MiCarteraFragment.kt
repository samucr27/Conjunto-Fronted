package com.example.conjuntoresidencial.ui.propietario

import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.lifecycleScope
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.model.CheckoutRequest
import kotlinx.coroutines.launch
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FragmentMiCarteraBinding
import com.example.conjuntoresidencial.model.CuotaDTO
import com.example.conjuntoresidencial.model.PagoDTO
import com.example.conjuntoresidencial.ui.cartera.CuotaAdapter
import com.example.conjuntoresidencial.ui.cartera.Cuotas
import com.example.conjuntoresidencial.ui.cartera.abrirComprobante
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
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
        configurarEncabezado(
            binding.encabezado, R.string.mi_cartera_titulo, R.string.mi_cartera_subtitulo,
            menu = R.menu.menu_lista, alElegir = accionesDeLista(binding.swipe) { cargar() }
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
            // null = salió bien (antes ese caso nunca se mostraba)
            evento.siNoManejado { error ->
                aviso(error ?: getString(R.string.simulacion_exitosa), Snackbar.LENGTH_LONG).show()
            }
        }

        val cuotas = CuotaAdapter(conApartamento = false) { opcionesCuota(it) }
        binding.rvCuotas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCuotas.adapter = cuotas
        viewModel.cuotas.observe(viewLifecycleOwner) { lista ->
            cuotas.submitList(lista)
            binding.tvTituloCuotas.visibility = if (lista.isEmpty()) View.GONE else View.VISIBLE
        }
        viewModel.comprobante.observe(viewLifecycleOwner) { evento ->
            evento.obtenerSiNoManejado()?.let { aviso(it, Snackbar.LENGTH_LONG).show() }
        }

        if (viewModel.pagoState.value == null) cargar()
    }

    private fun opcionesCuota(c: CuotaDTO) {
        val dialogo = MaterialAlertDialogBuilder(requireContext())
            .setTitle(Cuotas.nombreMes(c.periodo))
            .setMessage(Cuotas.detalle(requireContext(), c) + "\n" + getString(Cuotas.estado(c).etiqueta))
        val subir = {
            cuotaPorSubir = c.id
            elegirArchivo.launch(arrayOf("image/*", "application/pdf"))
        }
        if (c.estado != Cuotas.PAGADA) {
            // P6: el pago en línea es lo principal; el comprobante queda como segunda opción
            dialogo.setPositiveButton(R.string.cuota_pagar_en_linea) { _, _ -> pagarEnLinea(c) }
            dialogo.setNeutralButton(R.string.cuota_otras_opciones) { _, _ ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(Cuotas.nombreMes(c.periodo))
                    .setItems(listOfNotNull(
                        getString(if (c.tieneComprobante == true) R.string.cuota_cambiar_comprobante else R.string.cuota_subir_comprobante),
                        getString(R.string.cuota_ver_comprobante).takeIf { c.tieneComprobante == true }
                    ).toTypedArray()) { _, i -> if (i == 0) subir() else abrirComprobante(c) }
                    .setNegativeButton(R.string.btn_cerrar, null).show()
            }
        } else if (c.tieneComprobante == true) {
            dialogo.setNeutralButton(R.string.cuota_ver_comprobante) { _, _ -> abrirComprobante(c) }
        }
        dialogo.setNegativeButton(R.string.btn_cerrar, null).show()
    }

    /** Pide al servidor la dirección firmada y la abre en una pestaña del navegador; al volver se recarga la cartera. */
    private fun pagarEnLinea(c: CuotaDTO) {
        if (!SessionManager(requireContext()).arrendatarioPuedePagar()) {
            aviso(R.string.cuota_sin_permiso_pagar).show()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val r = RetrofitClient.comun.crearCheckout(CheckoutRequest(c.id))
                val checkout = r.body()
                if (_binding == null) return@launch
                if (r.isSuccessful && checkout != null) {
                    esperandoPago = true
                    CustomTabsIntent.Builder().build().launchUrl(requireContext(), Uri.parse(checkout.url))
                } else {
                    aviso(RetrofitClient.mensajeDeError(r, getString(R.string.cuota_error_pago)), Snackbar.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                if (_binding != null) aviso(RetrofitClient.mensajeDeExcepcion(e)).show()
            }
        }
    }

    // El pago se confirma en el servidor (webhook): al volver del navegador solo se recarga para verlo
    private var esperandoPago = false

    override fun onResume() {
        super.onResume()
        if (esperandoPago) {
            esperandoPago = false
            cargar()
        }
    }

    // La cuota elegida sobrevive a que el sistema cierre la app mientras se escoge el archivo
    private var cuotaPorSubir: Long?
        get() = arguments?.getLong(ARG_CUOTA)?.takeIf { it > 0 }
        set(valor) { arguments = (arguments ?: Bundle()).apply { putLong(ARG_CUOTA, valor ?: 0) } }

    private val elegirArchivo = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val cuotaId = cuotaPorSubir ?: return@registerForActivityResult
        cuotaPorSubir = null
        if (uri == null) return@registerForActivityResult
        val resolver = requireContext().contentResolver
        val datos = try {
            resolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (_: Exception) {
            null
        }
        when {
            datos == null -> aviso(getString(R.string.cuota_error_leer), Snackbar.LENGTH_LONG).show()
            datos.size > TAMANO_MAXIMO -> aviso(getString(R.string.cuota_archivo_grande), Snackbar.LENGTH_LONG).show()
            else -> {
                val tipo = resolver.getType(uri) ?: "application/octet-stream"
                val nombre = if (tipo == "application/pdf") "comprobante.pdf" else "comprobante.jpg"
                val parte = MultipartBody.Part.createFormData("archivo", nombre, datos.toRequestBody(tipo.toMediaTypeOrNull()))
                viewModel.subirComprobante(aptoId, cuotaId, parte, getString(R.string.cuota_comprobante_enviado),
                    getString(R.string.cuota_error_subir))
            }
        }
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

    companion object {
        private const val ARG_CUOTA = "cuota_por_subir"
        private const val TAMANO_MAXIMO = 5 * 1024 * 1024
    }
}
