package com.example.conjuntoresidencial.ui.propietario

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FragmentZonasComunesPropietarioBinding
import com.example.conjuntoresidencial.model.ZonaComunDTO
import com.example.conjuntoresidencial.ui.admin.ZonaAdapter
import com.example.conjuntoresidencial.ui.comun.mostrarRecurso
import com.example.conjuntoresidencial.util.Formatos
import com.example.conjuntoresidencial.util.SessionManager
import com.example.conjuntoresidencial.ui.comun.accionesDeLista
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.ui.comun.resumen
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.Calendar
import java.util.Locale

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
        configurarEncabezado(
            binding.encabezado, R.string.menu_zonas, R.string.zonas_res_subtitulo, menu = R.menu.menu_lista,
            alElegir = accionesDeLista(binding.swipe) { viewModel.fetchZonas() }
        )
        val aptoId = SessionManager(requireContext()).getApartamentoId()

        // Con callback: las zonas reservables muestran el botón "Reservar"
        val adaptador = ZonaAdapter { zona -> mostrarDialogReserva(zona, aptoId) }
        binding.rvZonasPropietario.layoutManager = LinearLayoutManager(requireContext())
        binding.rvZonasPropietario.adapter = adaptador
        binding.estado.vincular(binding.rvZonasPropietario)
        binding.swipe.setOnRefreshListener { viewModel.fetchZonas() }

        viewModel.zonas.observe(viewLifecycleOwner) { resource ->
            binding.estado.mostrarRecurso(
                resource, binding.swipe, R.drawable.ic_place, getString(R.string.zonas_res_vacio),
                reintentar = { viewModel.fetchZonas() }
            ) { lista ->
                adaptador.submitList(lista)
                binding.encabezado.ponerInfo(resumen(
                    R.plurals.resumen_zonas, lista.size,
                    R.plurals.resumen_reservables to lista.count { it.esReservable == true }
                ))
            }
        }

        viewModel.reserva.observe(viewLifecycleOwner) { evento ->
            evento.obtenerSiNoManejado()?.let { error ->
                Snackbar.make(binding.root, error ?: getString(R.string.reserva_creada), Snackbar.LENGTH_LONG).show()
            }
        }

        if (viewModel.zonas.value == null) viewModel.fetchZonas()
    }

    private fun mostrarDialogReserva(zona: ZonaComunDTO, aptoId: Long) {
        val vista = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_reservar_zona, null)
        val tilFecha = vista.findViewById<TextInputLayout>(R.id.tilFechaReserva)
        val tilHora = vista.findViewById<TextInputLayout>(R.id.tilHoraReserva)
        val etFecha = vista.findViewById<TextInputEditText>(R.id.etFechaReserva)
        val etHora = vista.findViewById<TextInputEditText>(R.id.etHoraReserva)

        // Valores en el formato del backend ("2026-10-23", "14:00"); en pantalla, formato local
        var fecha: Calendar? = null
        var fechaIso = ""
        var horaIso = ""

        etFecha.setOnClickListener {
            val hoy = Calendar.getInstance()
            val inicial = fecha ?: hoy
            DatePickerDialog(requireContext(), { _, y, m, d ->
                fecha = Calendar.getInstance().apply { set(y, m, d) }
                fechaIso = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
                etFecha.setText(Formatos.fecha(fechaIso))
                tilFecha.error = null
                // Si ya había hora y con la nueva fecha queda en el pasado, se pide otra vez
                if (horaIso.isNotEmpty() && enElPasado(fecha!!, horaIso)) {
                    horaIso = ""
                    etHora.text = null
                }
            }, inicial.get(Calendar.YEAR), inicial.get(Calendar.MONTH), inicial.get(Calendar.DAY_OF_MONTH)).apply {
                datePicker.minDate = hoy.timeInMillis - 1000
            }.show()
        }

        etHora.setOnClickListener {
            val elegida = fecha
            if (elegida == null) {
                tilFecha.error = getString(R.string.reserva_fecha_obligatoria)
                return@setOnClickListener
            }
            val ahora = Calendar.getInstance()
            TimePickerDialog(requireContext(), { _, h, min ->
                val hora = String.format(Locale.US, "%02d:%02d", h, min)
                if (enElPasado(elegida, hora)) {
                    tilHora.error = getString(R.string.reserva_hora_pasada)
                } else {
                    horaIso = hora
                    etHora.setText(Formatos.hora(hora))
                    tilHora.error = null
                }
            }, (ahora.get(Calendar.HOUR_OF_DAY) + 1) % 24, 0, DateFormat.is24HourFormat(requireContext())).show()
        }

        val dialogo = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.reserva_titulo, zona.nombre))
            .setView(vista)
            .setPositiveButton(R.string.zona_reservar, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()

        // Se asigna después de show() para que el diálogo no se cierre si falta un dato
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            tilFecha.error = if (fechaIso.isEmpty()) getString(R.string.reserva_fecha_obligatoria) else null
            tilHora.error = if (horaIso.isEmpty()) getString(R.string.reserva_hora_obligatoria) else null
            if (fechaIso.isEmpty() || horaIso.isEmpty()) return@setOnClickListener
            viewModel.crearReserva(fechaIso, horaIso, zona.id!!, aptoId, getString(R.string.reserva_error_crear))
            dialogo.dismiss()
        }
    }

    /** true si la fecha elegida a esa hora ("HH:mm") ya pasó. El backend también lo valida. */
    private fun enElPasado(fecha: Calendar, hora: String): Boolean {
        val (h, m) = hora.split(":").map { it.toInt() }
        val momento = (fecha.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0)
        }
        return momento.before(Calendar.getInstance())
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
