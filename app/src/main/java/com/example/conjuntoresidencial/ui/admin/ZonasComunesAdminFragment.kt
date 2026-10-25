package com.example.conjuntoresidencial.ui.admin

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.DialogRegistrarZonaBinding
import com.example.conjuntoresidencial.databinding.FragmentZonasComunesAdminBinding
import com.example.conjuntoresidencial.model.ZonaComunDTO
import com.example.conjuntoresidencial.ui.comun.mostrarRecurso
import com.example.conjuntoresidencial.ui.comun.accionesDeLista
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.controlesFlotantes
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.ui.comun.resumen
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.reservas.Agenda
import com.example.conjuntoresidencial.util.Formatos
import com.example.conjuntoresidencial.util.limpiarErrorAlEscribir
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat

/**
 * Zonas comunes del administrador: registrar una (botón flotante) o tocar una para editarla.
 * Cada zona reservable puede tener aforo (personas por reserva) y horario para reservar.
 */
class ZonasComunesAdminFragment : Fragment() {
    private var _binding: FragmentZonasComunesAdminBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ZonasAdminViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentZonasComunesAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(
            binding.encabezado, R.string.zonas_titulo, R.string.zonas_subtitulo, menu = R.menu.menu_lista,
            alElegir = accionesDeLista(binding.swipe) { viewModel.fetchZonas() }
        )

        val adaptador = ZonaAdapter(onTocar = ::mostrarDialogZona)
        binding.rvZonasAdmin.layoutManager = LinearLayoutManager(requireContext())
        binding.rvZonasAdmin.adapter = adaptador
        binding.estado.vincular(binding.rvZonasAdmin)
        binding.swipe.setOnRefreshListener { viewModel.fetchZonas() }

        viewModel.zonas.observe(viewLifecycleOwner) { resource ->
            binding.estado.mostrarRecurso(
                resource, binding.swipe, R.drawable.ic_place, getString(R.string.zonas_vacio),
                reintentar = { viewModel.fetchZonas() }
            ) { lista ->
                adaptador.submitList(lista)
                binding.encabezado.ponerInfo(resumen(
                    R.plurals.resumen_zonas, lista.size,
                    R.plurals.resumen_reservables to lista.count { it.esReservable == true }
                ))
            }
        }

        viewModel.registro.observe(viewLifecycleOwner) { evento ->
            evento.obtenerSiNoManejado()?.let { aviso(it).show() }
        }

        // Botón flotante (encima del difuminado); se encoge a solo ícono al desplazar hacia abajo
        controlesFlotantes?.mostrarBotonFlotante(R.string.zonas_registrar, R.drawable.ic_add) { mostrarDialogZona(null) }?.let { boton ->
            binding.rvZonasAdmin.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                    if (dy > 0) boton.shrink() else if (dy < 0) boton.extend()
                }
            })
        }

        if (viewModel.zonas.value == null) viewModel.fetchZonas()
    }

    /** Registrar (zona null) o editar una zona, con su aforo y horario. */
    private fun mostrarDialogZona(zona: ZonaComunDTO?) {
        val vista = DialogRegistrarZonaBinding.inflate(layoutInflater)
        listOf(vista.tilNombreZona, vista.tilAforo, vista.tilCierre).forEach { it.limpiarErrorAlEscribir() }
        // Horas en el formato del backend ("08:00"); en pantalla, "8:00 a. m."
        var apertura: String? = zona?.horaApertura?.take(5)
        var cierre: String? = zona?.horaCierre?.take(5)
        if (zona != null) {
            vista.etNombreZona.setText(zona.nombre)
            vista.etEstadoZona.setText(zona.estadoRecibido)
            vista.etDescripcionZona.setText(zona.descripcionEstado)
            vista.cbEsReservable.isChecked = zona.esReservable == true
            zona.aforo?.let { vista.etAforo.setText(it.toString()) }
        }
        vista.etApertura.setText(apertura?.let(Formatos::hora))
        vista.etCierre.setText(cierre?.let(Formatos::hora))
        vista.etApertura.setOnClickListener {
            elegirHora(apertura ?: "08:00", R.string.zona_apertura) { apertura = it; vista.etApertura.setText(Formatos.hora(it)) }
        }
        vista.etCierre.setOnClickListener {
            elegirHora(cierre ?: "22:00", R.string.zona_cierre) {
                cierre = it
                vista.etCierre.setText(Formatos.hora(it))
                vista.tilCierre.error = null
            }
        }
        // Sin horario: se borran las dos horas
        vista.tilApertura.setEndIconOnClickListener { apertura = null; vista.etApertura.text = null }
        vista.tilCierre.setEndIconOnClickListener { cierre = null; vista.etCierre.text = null }

        val dialogo = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (zona == null) R.string.zona_nueva_titulo else R.string.zona_editar_titulo)
            .setView(vista.root)
            .setPositiveButton(if (zona == null) R.string.btn_registrar else R.string.btn_guardar, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()

        // Se asigna después de show() para que el diálogo no se cierre si falta algo
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val nombre = vista.etNombreZona.text.toString().trim()
            val aforoTexto = vista.etAforo.text.toString().trim()
            val aforo = aforoTexto.toIntOrNull()
            vista.tilNombreZona.error = if (nombre.isBlank()) getString(R.string.zona_nombre_obligatorio) else null
            vista.tilAforo.error = if (aforoTexto.isNotEmpty() && (aforo == null || aforo < 1)) getString(R.string.zona_aforo_minimo) else null
            val horarioMal = apertura != null && cierre != null &&
                (Agenda.minutos(cierre) ?: 0) <= (Agenda.minutos(apertura) ?: 0)
            vista.tilCierre.error = if (horarioMal) getString(R.string.zona_cierre_despues) else null
            if (nombre.isBlank() || vista.tilAforo.error != null || horarioMal) return@setOnClickListener
            viewModel.guardarZona(
                ZonaComunDTO(
                    zona?.id, nombre, vista.etEstadoZona.text.toString().trim(),
                    vista.etDescripcionZona.text.toString().trim(), vista.cbEsReservable.isChecked,
                    aforo = aforo, horaApertura = apertura, horaCierre = cierre
                ),
                getString(if (zona == null) R.string.zona_registrada else R.string.zona_actualizada),
                getString(R.string.zona_error_registrar)
            )
            dialogo.dismiss()
        }
    }

    private fun elegirHora(inicial: String, titulo: Int, alElegir: (String) -> Unit) {
        val minutos = Agenda.minutos(inicial) ?: 8 * 60
        val selector = MaterialTimePicker.Builder()
            .setTimeFormat(if (DateFormat.is24HourFormat(requireContext())) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
            .setHour(minutos / 60)
            .setMinute(minutos % 60)
            .setTitleText(titulo)
            .build()
        selector.addOnPositiveButtonClickListener { alElegir(Agenda.hora(selector.hour * 60 + selector.minute)) }
        selector.show(childFragmentManager, "hora")
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
