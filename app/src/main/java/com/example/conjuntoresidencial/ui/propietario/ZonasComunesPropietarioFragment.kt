package com.example.conjuntoresidencial.ui.propietario

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.conjuntoresidencial.databinding.FragmentZonasComunesPropietarioBinding
import com.example.conjuntoresidencial.model.ZonaComunDTO
import com.example.conjuntoresidencial.ui.admin.ZonaAdapter
import com.example.conjuntoresidencial.util.Resource
import com.example.conjuntoresidencial.util.SessionManager
import com.google.android.material.snackbar.Snackbar
import java.util.Calendar

class ZonasComunesPropietarioFragment : Fragment() {
    private var _binding: FragmentZonasComunesPropietarioBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ZonasPropietarioViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentZonasComunesPropietarioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvZonasPropietario.layoutManager = LinearLayoutManager(requireContext())
        val aptoId = SessionManager(requireContext()).getApartamentoId()
        viewModel.fetchZonas()

        binding.swipeRefreshZonas.setOnRefreshListener {
            viewModel.fetchZonas()
        }

        viewModel.zonas.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressZonasProp.visibility = View.VISIBLE
                    binding.tvNoZonasProp.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.progressZonasProp.visibility = View.GONE
                    binding.swipeRefreshZonas.isRefreshing = false
                    val lista = resource.data ?: emptyList()
                    if (lista.isEmpty()) {
                        binding.tvNoZonasProp.visibility = View.VISIBLE
                    } else {
                        binding.tvNoZonasProp.visibility = View.GONE
                        binding.rvZonasPropietario.adapter = ZonaAdapter(lista) { zona ->
                            mostrarDialogReserva(zona, aptoId)
                        }
                    }
                }
                is Resource.Error -> {
                    binding.progressZonasProp.visibility = View.GONE
                    binding.swipeRefreshZonas.isRefreshing = false
                    binding.tvNoZonasProp.visibility = View.VISIBLE
                    binding.tvNoZonasProp.text = resource.message
                }
            }
        }

        viewModel.reservaState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {}
                is Resource.Success -> Snackbar.make(binding.root, "Reserva creada exitosamente", Snackbar.LENGTH_SHORT).show()
                is Resource.Error -> Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun mostrarDialogReserva(zona: ZonaComunDTO, aptoId: Long) {
        var fechaSeleccionada = ""
        var horaSeleccionada = ""
        val cal = Calendar.getInstance()

        val fechaText = android.widget.TextView(requireContext()).apply {
            text = "Fecha: No seleccionada"
            setPadding(16, 8, 16, 8)
        }
        val horaText = android.widget.TextView(requireContext()).apply {
            text = "Hora: No seleccionada"
            setPadding(16, 8, 16, 8)
        }
        val btnFecha = com.google.android.material.button.MaterialButton(requireContext()).apply {
            text = "Seleccionar Fecha"
            setOnClickListener {
                DatePickerDialog(requireContext(), { _, y, m, d ->
                    fechaSeleccionada = String.format("%04d-%02d-%02d", y, m + 1, d)
                    fechaText.text = "Fecha: $fechaSeleccionada"
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
            }
        }
        val btnHora = com.google.android.material.button.MaterialButton(requireContext()).apply {
            text = "Seleccionar Hora"
            setOnClickListener {
                TimePickerDialog(requireContext(), { _, h, min ->
                    horaSeleccionada = String.format("%02d:%02d", h, min)
                    horaText.text = "Hora: $horaSeleccionada"
                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
            }
        }

        val layout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
            addView(btnFecha)
            addView(fechaText)
            addView(btnHora)
            addView(horaText)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Reservar ${zona.nombre}")
            .setView(layout)
            .setPositiveButton("Confirmar") { _, _ ->
                if (fechaSeleccionada.isEmpty() || horaSeleccionada.isEmpty()) {
                    Snackbar.make(binding.root, "Selecciona fecha y hora", Snackbar.LENGTH_SHORT).show()
                } else {
                    viewModel.crearReserva(fechaSeleccionada, horaSeleccionada, zona.id!!, aptoId)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}