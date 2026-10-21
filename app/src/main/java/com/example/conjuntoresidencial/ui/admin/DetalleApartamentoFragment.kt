package com.example.conjuntoresidencial.ui.admin

import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.FragmentDetalleApartamentoBinding
import com.example.conjuntoresidencial.model.CambioContrasenaRequest
import com.example.conjuntoresidencial.model.ResidenteDTO
import com.example.conjuntoresidencial.model.VehiculoDTO
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.util.Opciones
import com.example.conjuntoresidencial.util.comoLista
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class DetalleApartamentoFragment : Fragment() {

    private var _binding: FragmentDetalleApartamentoBinding? = null
    private val binding get() = _binding!!
    private var aptoId: Long = -1L
    private val residentesAdapter = ResidenteAdapter()
    private val vehiculosAdapter = VehiculoAdminAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDetalleApartamentoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        aptoId = arguments?.getLong("aptoId") ?: -1L

        binding.rvResidentesApto.layoutManager = LinearLayoutManager(requireContext())
        binding.rvVehiculosApto.layoutManager = LinearLayoutManager(requireContext())
        binding.rvResidentesApto.adapter = residentesAdapter
        binding.rvVehiculosApto.adapter = vehiculosAdapter

        cargarInfoApartamento()
        cargarResidentes()
        cargarVehiculos()

        binding.btnAgregarResidente.setOnClickListener { mostrarDialogResidente() }
        binding.btnAgregarVehiculoAdmin.setOnClickListener { mostrarDialogVehiculo() }
        binding.btnRestablecerContrasena.setOnClickListener { mostrarDialogRestablecerContrasena() }
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

    // Todas las peticiones usan viewLifecycleOwner.lifecycleScope: se cancelan si el usuario
    // sale de la pantalla, en vez de tocar una vista que ya no existe.

    private fun cargarInfoApartamento() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getApartamentoById(aptoId)
                if (response.isSuccessful && response.body() != null) {
                    val apto = response.body()!!
                    binding.tvInfoTorre.text = "Torre ${apto.torre} — Apto ${apto.apto}"
                    binding.tvInfoTorreNum.text = apto.torre
                    binding.tvInfoAptoNum.text = apto.apto
                } else {
                    avisar(RetrofitClient.mensajeDeError(response, "No se pudo cargar el apartamento"))
                }
            } catch (_: Exception) {
                avisar(getString(R.string.error_red))
            }
        }
    }

    private fun cargarResidentes() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getResidentesPorApartamento(aptoId)
                if (response.isSuccessful)
                    residentesAdapter.submitList(response.body() ?: emptyList())
                else avisar(RetrofitClient.mensajeDeError(response, "No se pudieron cargar los residentes"))
            } catch (_: Exception) {
                avisar(getString(R.string.error_red))
            }
        }
    }

    private fun cargarVehiculos() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getVehiculosPorApartamento(aptoId)
                if (response.isSuccessful)
                    vehiculosAdapter.submitList(response.body() ?: emptyList())
                else avisar(RetrofitClient.mensajeDeError(response, "No se pudieron cargar los vehículos"))
            } catch (_: Exception) {
                avisar(getString(R.string.error_red))
            }
        }
    }

    private fun mostrarDialogResidente() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_agregar_residente, null)
        val tipoView = dialogView.findViewById<AutoCompleteTextView>(R.id.etTipoResidente)
        tipoView.comoLista(Opciones.TIPOS_HABITANTE)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Agregar Residente")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = dialogView.findViewById<TextInputEditText>(R.id.etNombreResidente).text.toString().trim()
                val telefono = dialogView.findViewById<TextInputEditText>(R.id.etTelefonoResidente).text.toString().trim()
                val identificacion = dialogView.findViewById<TextInputEditText>(R.id.etIdentificacionResidente).text.toString().trim()
                val tipo = tipoView.text.toString()
                if (nombre.isBlank()) {
                    avisar("El nombre del residente es obligatorio")
                    return@setPositiveButton
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val response = RetrofitClient.instance.createResidente(
                            ResidenteDTO(null, nombre, telefono, identificacion, tipo, aptoId)
                        )
                        if (response.isSuccessful) cargarResidentes()
                        else avisar(RetrofitClient.mensajeDeError(response, "No se pudo guardar el residente"))
                    } catch (_: Exception) {
                        avisar(getString(R.string.error_red))
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogVehiculo() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_agregar_vehiculo, null)
        val tipoView = dialogView.findViewById<AutoCompleteTextView>(R.id.etTipo)
        tipoView.comoLista(Opciones.TIPOS_VEHICULO)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Agregar Vehículo")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val placa = dialogView.findViewById<TextInputEditText>(R.id.etPlaca).text.toString().trim()
                val marca = dialogView.findViewById<TextInputEditText>(R.id.etMarca).text.toString().trim()
                val color = dialogView.findViewById<TextInputEditText>(R.id.etColor).text.toString().trim()
                val tipo = tipoView.text.toString()
                if (placa.isBlank()) {
                    avisar("La placa es obligatoria")
                    return@setPositiveButton
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val response = RetrofitClient.instance.createVehiculo(
                            VehiculoDTO(null, placa, marca, color, tipo, aptoId)
                        )
                        if (response.isSuccessful) cargarVehiculos()
                        else avisar(RetrofitClient.mensajeDeError(response, "No se pudo guardar el vehículo"))
                    } catch (_: Exception) {
                        avisar(getString(R.string.error_red))
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun avisar(mensaje: String) {
        _binding?.let { Snackbar.make(it.root, mensaje, Snackbar.LENGTH_LONG).show() }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}