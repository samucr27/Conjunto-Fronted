package com.example.conjuntoresidencial.ui.propietario

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FragmentMisVehiculosBinding
import com.example.conjuntoresidencial.model.VehiculoDTO
import com.example.conjuntoresidencial.util.Resource
import com.example.conjuntoresidencial.util.Opciones
import com.example.conjuntoresidencial.util.SessionManager
import com.example.conjuntoresidencial.util.comoLista
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText

class MisVehiculosFragment : Fragment() {
    private var _binding: FragmentMisVehiculosBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MisVehiculosViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMisVehiculosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvVehiculos.layoutManager = LinearLayoutManager(requireContext())
        val session = SessionManager(requireContext())
        val aptoId = session.getApartamentoId()
        viewModel.fetchVehiculos(aptoId)

        binding.swipeRefreshVehiculos.setOnRefreshListener {
            viewModel.fetchVehiculos(aptoId)
        }

        viewModel.vehiculos.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressVehiculos.visibility = View.VISIBLE
                    binding.tvNoVehiculos.visibility = View.GONE
                    binding.rvVehiculos.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.progressVehiculos.visibility = View.GONE
                    binding.swipeRefreshVehiculos.isRefreshing = false
                    val lista = resource.data ?: emptyList()
                    if (lista.isEmpty()) {
                        binding.tvNoVehiculos.visibility = View.VISIBLE
                        binding.rvVehiculos.visibility = View.GONE
                    } else {
                        binding.tvNoVehiculos.visibility = View.GONE
                        binding.rvVehiculos.visibility = View.VISIBLE
                        binding.rvVehiculos.adapter = VehiculoAdapter(lista) { vehiculo ->
                            confirmarEliminar(vehiculo, aptoId)
                        }
                    }
                }
                is Resource.Error -> {
                    binding.progressVehiculos.visibility = View.GONE
                    binding.swipeRefreshVehiculos.isRefreshing = false
                    binding.tvNoVehiculos.visibility = View.VISIBLE
                    binding.tvNoVehiculos.text = resource.message
                }
            }
        }

        viewModel.mensaje.observe(viewLifecycleOwner) { evento ->
            evento.obtenerSiNoManejado()?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }

        binding.btnAgregarVehiculo.setOnClickListener {
            val dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_agregar_vehiculo, null)
            val tipoView = dialogView.findViewById<AutoCompleteTextView>(R.id.etTipo)
            tipoView.comoLista(Opciones.TIPOS_VEHICULO)
            AlertDialog.Builder(requireContext())
                .setTitle("Agregar Vehículo")
                .setView(dialogView)
                .setPositiveButton("Guardar") { _, _ ->
                    val placa = dialogView.findViewById<TextInputEditText>(R.id.etPlaca).text.toString().trim()
                    val marca = dialogView.findViewById<TextInputEditText>(R.id.etMarca).text.toString().trim()
                    val color = dialogView.findViewById<TextInputEditText>(R.id.etColor).text.toString().trim()
                    val tipo = tipoView.text.toString()
                    if (placa.isNotBlank()) {
                        viewModel.agregarVehiculo(VehiculoDTO(null, placa, marca, color, tipo, aptoId))
                    } else {
                        Snackbar.make(binding.root, "La placa es obligatoria", Snackbar.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun confirmarEliminar(vehiculo: VehiculoDTO, aptoId: Long) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar vehículo")
            .setMessage("¿Eliminar ${vehiculo.placa}?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.eliminarVehiculo(vehiculo, aptoId)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}