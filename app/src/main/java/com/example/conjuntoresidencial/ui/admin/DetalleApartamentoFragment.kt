package com.example.conjuntoresidencial.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.FragmentDetalleApartamentoBinding
import com.example.conjuntoresidencial.model.ResidenteDTO
import com.example.conjuntoresidencial.model.VehiculoDTO
import com.example.conjuntoresidencial.R
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetalleApartamentoFragment : Fragment() {

    private var _binding: FragmentDetalleApartamentoBinding? = null
    private val binding get() = _binding!!
    private var aptoId: Long = -1L

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDetalleApartamentoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        aptoId = arguments?.getLong("aptoId") ?: -1L

        binding.rvResidentesApto.layoutManager = LinearLayoutManager(requireContext())
        binding.rvVehiculosApto.layoutManager = LinearLayoutManager(requireContext())

        cargarInfoApartamento()
        cargarResidentes()
        cargarVehiculos()

        binding.btnAgregarResidente.setOnClickListener { mostrarDialogResidente() }
        binding.btnAgregarVehiculoAdmin.setOnClickListener { mostrarDialogVehiculo() }
    }

    private fun cargarInfoApartamento() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getApartamentoById(aptoId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val apto = response.body()!!
                        binding.tvInfoTorre.text = "Torre ${apto.torre} — Apto ${apto.apto}"
                        binding.tvInfoTorreNum.text = apto.torre
                        binding.tvInfoAptoNum.text = apto.apto
                        binding.tvInfoClave.text = apto.contrasena
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun cargarResidentes() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getResidentesPorApartamento(aptoId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful)
                        binding.rvResidentesApto.adapter = ResidenteAdapter(response.body() ?: emptyList())
                }
            } catch (_: Exception) {}
        }
    }

    private fun cargarVehiculos() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getVehiculosPorApartamento(aptoId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful)
                        binding.rvVehiculosApto.adapter = VehiculoAdminAdapter(response.body() ?: emptyList())
                }
            } catch (_: Exception) {}
        }
    }

    private fun mostrarDialogResidente() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_agregar_residente, null)
        AlertDialog.Builder(requireContext())
            .setTitle("Agregar Residente")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = dialogView.findViewById<TextInputEditText>(R.id.etNombreResidente).text.toString().trim()
                val telefono = dialogView.findViewById<TextInputEditText>(R.id.etTelefonoResidente).text.toString().trim()
                val identificacion = dialogView.findViewById<TextInputEditText>(R.id.etIdentificacionResidente).text.toString().trim()
                val tipo = dialogView.findViewById<TextInputEditText>(R.id.etTipoResidente).text.toString().trim()
                if (nombre.isNotBlank()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            RetrofitClient.instance.createResidente(
                                ResidenteDTO(null, nombre, telefono, identificacion, tipo, aptoId)
                            )
                            withContext(Dispatchers.Main) { cargarResidentes() }
                        } catch (_: Exception) {}
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogVehiculo() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_agregar_vehiculo, null)
        AlertDialog.Builder(requireContext())
            .setTitle("Agregar Vehículo")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val placa = dialogView.findViewById<TextInputEditText>(R.id.etPlaca).text.toString().trim()
                val marca = dialogView.findViewById<TextInputEditText>(R.id.etMarca).text.toString().trim()
                val color = dialogView.findViewById<TextInputEditText>(R.id.etColor).text.toString().trim()
                val tipo = dialogView.findViewById<TextInputEditText>(R.id.etTipo).text.toString().trim()
                if (placa.isNotBlank()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            RetrofitClient.instance.createVehiculo(
                                VehiculoDTO(null, placa, marca, color, tipo, aptoId)
                            )
                            withContext(Dispatchers.Main) { cargarVehiculos() }
                        } catch (_: Exception) {}
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}