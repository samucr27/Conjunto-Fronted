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
        val session = SessionManager(requireContext())
        val aptoId = session.getApartamentoId()
        val torreApto = session.getTorreApto()
        viewModel.fetchZonas()

        binding.swipeRefreshZonas.setOnRefreshListener { viewModel.fetchZonas() }

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
                            mostrarDialogReserva(zona, aptoId, torreApto)
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

    private fun mostrarDialogReserva(zona: ZonaComunDTO, aptoId: Long, torreApto: String) {
        var fechaSeleccionada = ""
        var horaSeleccionada = ""
        var calFecha = Calendar.getInstance()

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
                val hoy = Calendar.getInstance()
                val picker = DatePickerDialog(requireContext(), { _, y, m, d ->
                    calFecha = Calendar.getInstance().apply { set(y, m, d) }
                    fechaSeleccionada = String.format("%04d-%02d-%02d", y, m + 1, d)
                    fechaText.text = "Fecha: $fechaSeleccionada"
                    horaSeleccionada = ""
                    horaText.text = "Hora: No seleccionada"
                }, hoy.get(Calendar.YEAR), hoy.get(Calendar.MONTH), hoy.get(Calendar.DAY_OF_MONTH))
                picker.datePicker.minDate = hoy.timeInMillis
                picker.show()
            }
        }

        val btnHora = com.google.android.material.button.MaterialButton(requireContext()).apply {
            text = "Seleccionar Hora"
            setOnClickListener {
                if (fechaSeleccionada.isEmpty()) {
                    Snackbar.make(binding.root, "Primero selecciona una fecha", Snackbar.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val ahora = Calendar.getInstance()
                TimePickerDialog(requireContext(), { _, h, min ->
                    val esHoy = calFecha.get(Calendar.YEAR) == ahora.get(Calendar.YEAR) &&
                            calFecha.get(Calendar.DAY_OF_YEAR) == ahora.get(Calendar.DAY_OF_YEAR)
                    if (esHoy && (h < ahora.get(Calendar.HOUR_OF_DAY) ||
                                (h == ahora.get(Calendar.HOUR_OF_DAY) && min <= ahora.get(Calendar.MINUTE)))) {
                        Snackbar.make(binding.root, "No puedes reservar en una hora pasada", Snackbar.LENGTH_SHORT).show()
                    } else {
                        horaSeleccionada = String.format("%02d:%02d", h, min)
                        horaText.text = "Hora: $horaSeleccionada"
                    }
                }, ahora.get(Calendar.HOUR_OF_DAY), ahora.get(Calendar.MINUTE), true).show()
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
                    viewModel.crearReserva(
                        fecha = fechaSeleccionada,
                        hora = horaSeleccionada,
                        zonaComunId = zona.id!!,
                        apartamentoId = aptoId,
                        nombreZona = zona.nombre,
                        torreApto = torreApto
                    )
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}