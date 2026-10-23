package com.example.conjuntoresidencial.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.FragmentDetalleApartamentoBinding
import com.example.conjuntoresidencial.model.CambioContrasenaRequest
import com.example.conjuntoresidencial.model.ResidenteDTO
import com.example.conjuntoresidencial.model.VehiculoDTO
import com.example.conjuntoresidencial.util.Opciones
import com.example.conjuntoresidencial.util.comoLista
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.ui.comun.ponerTitulo
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class DetalleApartamentoFragment : Fragment() {

    private var _binding: FragmentDetalleApartamentoBinding? = null
    private val binding get() = _binding!!
    private var aptoId: Long = -1L
    private val residentesAdapter = ResidenteAdapter()
    private val vehiculosAdapter = VehiculoAdminAdapter()

    private var totalResidentes = 0
    private var totalVehiculos = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDetalleApartamentoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        aptoId = arguments?.getLong("aptoId") ?: -1L
        configurarEncabezado(binding.encabezado, R.string.detalle_titulo, R.string.detalle_subtitulo)

        binding.rvResidentesApto.layoutManager = LinearLayoutManager(requireContext())
        binding.rvVehiculosApto.layoutManager = LinearLayoutManager(requireContext())
        binding.rvResidentesApto.adapter = residentesAdapter
        binding.rvVehiculosApto.adapter = vehiculosAdapter
        binding.estado.vincular(binding.contenido)

        binding.btnAgregarResidente.setOnClickListener { mostrarDialogResidente() }
        binding.btnAgregarVehiculoAdmin.setOnClickListener { mostrarDialogVehiculo() }
        binding.btnRestablecerContrasena.setOnClickListener { mostrarDialogRestablecerContrasena() }

        cargarTodo()
    }

    // Todas las peticiones usan viewLifecycleOwner.lifecycleScope: se cancelan si el usuario
    // sale de la pantalla, en vez de tocar una vista que ya no existe.

    /** Apartamento, residentes y vehículos en paralelo; si falla el apartamento, pantalla de error. */
    private fun cargarTodo() {
        binding.estado.mostrarCargando()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // coroutineScope: si una petición falla, el error llega al catch en vez de cerrar la app
                val (apto, residentes, vehiculos) = coroutineScope {
                    val a = async { RetrofitClient.instance.getApartamentoById(aptoId) }
                    val r = async { RetrofitClient.instance.getResidentesPorApartamento(aptoId) }
                    val v = async { RetrofitClient.instance.getVehiculosPorApartamento(aptoId) }
                    Triple(a.await(), r.await(), v.await())
                }
                val datos = apto.body()
                if (!apto.isSuccessful || datos == null) {
                    binding.estado.mostrarError(
                        RetrofitClient.mensajeDeError(apto, getString(R.string.detalle_error_cargar))
                    ) { cargarTodo() }
                    return@launch
                }
                binding.encabezado.ponerTitulo(getString(R.string.apto_item_titulo, datos.torre, datos.apto))
                mostrarResidentes(if (residentes.isSuccessful) residentes.body().orEmpty() else emptyList())
                mostrarVehiculos(if (vehiculos.isSuccessful) vehiculos.body().orEmpty() else emptyList())
                binding.estado.mostrarContenido()
            } catch (e: Exception) {
                binding.estado.mostrarError(RetrofitClient.mensajeDeExcepcion(e)) { cargarTodo() }
            }
        }
    }

    private fun mostrarResidentes(lista: List<ResidenteDTO>) {
        residentesAdapter.submitList(lista)
        binding.tvSinResidentes.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
        totalResidentes = lista.size
        ponerResumen()
    }

    private fun mostrarVehiculos(lista: List<VehiculoDTO>) {
        vehiculosAdapter.submitList(lista)
        binding.tvSinVehiculos.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
        totalVehiculos = lista.size
        ponerResumen()
    }

    /** Resumen del encabezado: "2 residentes · 2 vehículos". */
    private fun ponerResumen() {
        binding.encabezado.ponerInfo(listOf(
            resources.getQuantityString(R.plurals.resumen_residentes, totalResidentes, totalResidentes),
            resources.getQuantityString(R.plurals.resumen_vehiculos, totalVehiculos, totalVehiculos)
        ).joinToString(" · "))
    }

    private fun recargarResidentes() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getResidentesPorApartamento(aptoId)
                if (response.isSuccessful) mostrarResidentes(response.body().orEmpty())
                else avisar(RetrofitClient.mensajeDeError(response, getString(R.string.estado_error_titulo)))
            } catch (e: Exception) {
                avisar(RetrofitClient.mensajeDeExcepcion(e))
            }
        }
    }

    private fun recargarVehiculos() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getVehiculosPorApartamento(aptoId)
                if (response.isSuccessful) mostrarVehiculos(response.body().orEmpty())
                else avisar(RetrofitClient.mensajeDeError(response, getString(R.string.estado_error_titulo)))
            } catch (e: Exception) {
                avisar(RetrofitClient.mensajeDeExcepcion(e))
            }
        }
    }

    private fun mostrarDialogRestablecerContrasena() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_restablecer_contrasena, null)
        val til = dialogView.findViewById<TextInputLayout>(R.id.tilNuevaContrasena)
        val et = dialogView.findViewById<TextInputEditText>(R.id.etNuevaContrasena)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.titulo_restablecer_contrasena)
            .setView(dialogView)
            .setPositiveButton(R.string.btn_guardar, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()

        // Se asigna después de show() para que el diálogo no se cierre si la validación falla
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { boton ->
            val nueva = et.text?.toString().orEmpty()
            if (nueva.length < 6) {
                til.error = getString(R.string.error_contrasena_corta)
                return@setOnClickListener
            }
            til.error = null
            boton.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                val mensaje = try {
                    val response = RetrofitClient.instance.restablecerContrasena(aptoId, CambioContrasenaRequest(nueva))
                    if (response.isSuccessful) getString(R.string.contrasena_restablecida)
                    else RetrofitClient.mensajeDeError(response, getString(R.string.error_restablecer_contrasena))
                } catch (_: Exception) {
                    getString(R.string.error_red)
                }
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show()
                dialog.dismiss()
            }
        }
    }

    private fun mostrarDialogResidente() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_agregar_residente, null)
        val tipoView = dialogView.findViewById<AutoCompleteTextView>(R.id.etTipoResidente)
        tipoView.comoLista(Opciones.TIPOS_HABITANTE)
        val tilNombre = dialogView.findViewById<TextInputLayout>(R.id.tilNombreResidente)
        val dialogo = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.residente_nuevo)
            .setView(dialogView)
            .setPositiveButton(R.string.btn_guardar, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()

        // Se asigna después de show() para que el diálogo no se cierre si falta el nombre
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val nombre = dialogView.findViewById<TextInputEditText>(R.id.etNombreResidente).text.toString().trim()
            if (nombre.isBlank()) {
                tilNombre.error = getString(R.string.residente_nombre_obligatorio)
                return@setOnClickListener
            }
            val telefono = dialogView.findViewById<TextInputEditText>(R.id.etTelefonoResidente).text.toString().trim()
            val identificacion = dialogView.findViewById<TextInputEditText>(R.id.etIdentificacionResidente).text.toString().trim()
            val tipo = tipoView.text.toString()
            dialogo.dismiss()
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val response = RetrofitClient.instance.createResidente(
                        ResidenteDTO(null, nombre, telefono, identificacion, tipo, aptoId)
                    )
                    if (response.isSuccessful) recargarResidentes()
                    else avisar(RetrofitClient.mensajeDeError(response, getString(R.string.residente_error_guardar)))
                } catch (_: Exception) {
                    avisar(getString(R.string.error_red))
                }
            }
        }
    }

    private fun mostrarDialogVehiculo() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_agregar_vehiculo, null)
        val tipoView = dialogView.findViewById<AutoCompleteTextView>(R.id.etTipo)
        tipoView.comoLista(Opciones.TIPOS_VEHICULO)
        val tilPlaca = dialogView.findViewById<TextInputLayout>(R.id.tilPlaca)
        val dialogo = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.vehiculo_nuevo)
            .setView(dialogView)
            .setPositiveButton(R.string.btn_guardar, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()

        // Se asigna después de show() para que el diálogo no se cierre si falta la placa
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val placa = dialogView.findViewById<TextInputEditText>(R.id.etPlaca).text.toString().trim()
            if (placa.isBlank()) {
                tilPlaca.error = getString(R.string.vehiculo_placa_obligatoria)
                return@setOnClickListener
            }
            val marca = dialogView.findViewById<TextInputEditText>(R.id.etMarca).text.toString().trim()
            val color = dialogView.findViewById<TextInputEditText>(R.id.etColor).text.toString().trim()
            val tipo = tipoView.text.toString()
            dialogo.dismiss()
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val response = RetrofitClient.instance.createVehiculo(
                        VehiculoDTO(null, placa, marca, color, tipo, aptoId)
                    )
                    if (response.isSuccessful) recargarVehiculos()
                    else avisar(RetrofitClient.mensajeDeError(response, getString(R.string.vehiculo_error_guardar)))
                } catch (_: Exception) {
                    avisar(getString(R.string.error_red))
                }
            }
        }
    }

    private fun avisar(mensaje: String) {
        _binding?.let { Snackbar.make(it.root, mensaje, Snackbar.LENGTH_LONG).show() }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
