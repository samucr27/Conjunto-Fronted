package com.example.conjuntoresidencial.ui.admin

import android.os.Bundle
import android.text.InputType
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.FragmentEditarZonaBinding
import com.example.conjuntoresidencial.model.BloqueoZona
import com.example.conjuntoresidencial.model.DiaHorarioZona
import com.example.conjuntoresidencial.model.HorarioSemanal
import com.example.conjuntoresidencial.model.ZonaComunDTO
import com.example.conjuntoresidencial.ui.comun.*
import com.example.conjuntoresidencial.ui.reservas.Agenda
import com.example.conjuntoresidencial.util.Formatos
import com.example.conjuntoresidencial.util.limpiarErrorAlEscribir
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Crear o editar una zona común (administración, P5): datos, aforo y horario general; el
 * horario de cada día de la semana (si se activa) y las fechas bloqueadas.
 */
class EditarZonaFragment : Fragment() {
    private var _binding: FragmentEditarZonaBinding? = null
    private val binding get() = _binding!!
    private val zonaId: Long? get() = arguments?.getLong(ARG_ZONA)?.takeIf { it > 0 }

    private var apertura: String? = null
    private var cierre: String? = null

    /** Horario de cada día (1 = lunes): abierto, apertura y cierre ("08:00"). */
    private data class Dia(var abierto: Boolean, var apertura: String, var cierre: String)
    private val dias = (1..7).associateWith { Dia(it <= 5, "08:00", "20:00") }.toMutableMap()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditarZonaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val nueva = zonaId == null
        configurarEncabezado(binding.encabezado, if (nueva) R.string.zona_nueva_titulo else R.string.zona_editar_titulo,
            R.string.zona_editor_descripcion)
        val d = binding.datosZona
        listOf(d.tilNombreZona, d.tilAforo, d.tilCierre).forEach { it.limpiarErrorAlEscribir() }
        d.etApertura.setOnClickListener { elegirHora(apertura ?: "08:00", R.string.zona_apertura) { apertura = it; d.etApertura.setText(Formatos.hora(it)) } }
        d.etCierre.setOnClickListener { elegirHora(cierre ?: "22:00", R.string.zona_cierre) { cierre = it; d.etCierre.setText(Formatos.hora(it)) } }
        d.tilApertura.setEndIconOnClickListener { apertura = null; d.etApertura.text = null }
        d.tilCierre.setEndIconOnClickListener { cierre = null; d.etCierre.text = null }

        binding.swHorarioPorDia.setOnCheckedChangeListener { _, activo -> binding.contenedorDias.visibility = if (activo) View.VISIBLE else View.GONE }
        binding.btnBloquearFecha.setOnClickListener { bloquearFecha() }
        // Una zona nueva primero se guarda; luego se le ponen fechas bloqueadas
        binding.tarjetaBloqueos.visibility = if (nueva) View.GONE else View.VISIBLE
        binding.btnEliminarZona.visibility = if (nueva) View.GONE else View.VISIBLE
        binding.btnEliminarZona.setOnClickListener { eliminar() }
        controlesFlotantes?.mostrarBotonFlotante(R.string.btn_guardar, R.drawable.ic_check_circle) { guardar() }

        pintarDias()
        binding.contenedorDias.visibility = View.GONE
        if (!nueva) cargar()
    }

    private fun cargar() {
        val id = zonaId ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val zona = RetrofitClient.instance.getZonaComunById(id).body()
                val horario = RetrofitClient.instance.getHorarioZona(id).body()
                if (_binding == null) return@launch
                zona?.let { z ->
                    val d = binding.datosZona
                    d.etNombreZona.setText(z.nombre)
                    d.etEstadoZona.setText(z.estadoRecibido)
                    d.etDescripcionZona.setText(z.descripcionEstado)
                    d.cbEsReservable.isChecked = z.esReservable == true
                    z.aforo?.let { d.etAforo.setText(it.toString()) }
                    apertura = z.horaApertura?.take(5)
                    cierre = z.horaCierre?.take(5)
                    d.etApertura.setText(apertura?.let(Formatos::hora))
                    d.etCierre.setText(cierre?.let(Formatos::hora))
                }
                horario?.let { h ->
                    if (h.semanal.isNotEmpty()) {
                        dias.values.forEach { it.abierto = false }
                        h.semanal.forEach { s -> dias[s.diaSemana] = Dia(true, s.apertura.take(5), s.cierre.take(5)) }
                        binding.swHorarioPorDia.isChecked = true
                    }
                    pintarDias()
                    pintarBloqueos(h.bloqueos)
                }
            } catch (e: Exception) {
                if (_binding != null) aviso(RetrofitClient.mensajeDeExcepcion(e)).show()
            }
        }
    }

    /** Una fila por día: interruptor (abre o no) y las dos horas. */
    private fun pintarDias() {
        binding.contenedorDias.removeAllViews()
        val nombres = resources.getStringArray(R.array.dias_semana)
        (1..7).forEach { n ->
            val dia = dias.getValue(n)
            val fila = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL }
            val sw = MaterialSwitch(requireContext()).apply {
                text = nombres[n - 1]
                isChecked = dia.abierto
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                minHeight = (48 * resources.displayMetrics.density).toInt()
            }
            fun boton(valor: String, titulo: Int, alCambiar: (String) -> Unit) =
                MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                    text = Formatos.hora(valor)
                    isEnabled = dia.abierto
                    setOnClickListener { elegirHora(valor, titulo) { h -> alCambiar(h); pintarDias() } }
                }
            val bAbre = boton(dia.apertura, R.string.zona_apertura) { dia.apertura = it }
            val bCierra = boton(dia.cierre, R.string.zona_cierre) { dia.cierre = it }
            sw.setOnCheckedChangeListener { _, si -> dia.abierto = si; bAbre.isEnabled = si; bCierra.isEnabled = si }
            fila.addView(sw)
            fila.addView(bAbre)
            fila.addView(TextView(requireContext()).apply { text = " – " })
            fila.addView(bCierra)
            binding.contenedorDias.addView(fila)
        }
    }

    private fun pintarBloqueos(lista: List<BloqueoZona>) {
        binding.contenedorBloqueos.removeAllViews()
        binding.tvSinBloqueos.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
        lista.forEach { b ->
            val fila = layoutInflater.inflate(android.R.layout.simple_list_item_2, binding.contenedorBloqueos, false)
            fila.findViewById<TextView>(android.R.id.text1).text = Formatos.fecha(b.fecha)
            fila.findViewById<TextView>(android.R.id.text2).text = b.motivo ?: getString(R.string.zona_editor_tocar_desbloquear)
            fila.setOnClickListener {
                confirmar(getString(R.string.zona_editor_desbloquear_titulo, Formatos.fecha(b.fecha)), null, R.string.zona_editor_desbloquear) {
                    ejecutarAccion(getString(R.string.zona_editor_desbloqueada),
                        { RetrofitClient.instance.desbloquearFecha(zonaId!!, b.id!!) }) { cargar() }
                }
            }
            binding.contenedorBloqueos.addView(fila)
        }
    }

    private fun bloquearFecha() {
        val selector = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.zona_editor_bloquear)
            .setCalendarConstraints(CalendarConstraints.Builder().setValidator(DateValidatorPointForward.now()).build())
            .build()
        selector.addOnPositiveButtonClickListener { millis ->
            val fecha = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(millis)
            pedirTexto(getString(R.string.zona_editor_motivo_titulo, Formatos.fecha(fecha)), R.string.residente_motivo, null,
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, validar = { null }) { motivo ->
                ejecutarAccion(getString(R.string.zona_editor_bloqueada),
                    { RetrofitClient.instance.bloquearFecha(zonaId!!, BloqueoZona(null, fecha, motivo.ifBlank { null })) }) { cargar() }
            }
        }
        selector.show(childFragmentManager, "fecha")
    }

    private fun guardar() {
        val d = binding.datosZona
        val nombre = d.etNombreZona.text.toString().trim()
        val aforoTexto = d.etAforo.text.toString().trim()
        val aforo = aforoTexto.toIntOrNull()
        d.tilNombreZona.error = if (nombre.isBlank()) getString(R.string.zona_nombre_obligatorio) else null
        d.tilAforo.error = if (aforoTexto.isNotEmpty() && (aforo == null || aforo < 1)) getString(R.string.zona_aforo_minimo) else null
        val horarioMal = apertura != null && cierre != null && (Agenda.minutos(cierre) ?: 0) <= (Agenda.minutos(apertura) ?: 0)
        d.tilCierre.error = if (horarioMal) getString(R.string.zona_cierre_despues) else null
        val diaMal = binding.swHorarioPorDia.isChecked && dias.values.any { it.abierto && (Agenda.minutos(it.cierre) ?: 0) <= (Agenda.minutos(it.apertura) ?: 0) }
        if (diaMal) aviso(R.string.zona_editor_dia_mal).show()
        if (nombre.isBlank() || d.tilAforo.error != null || horarioMal || diaMal) return
        val zona = ZonaComunDTO(zonaId, nombre, d.etEstadoZona.text.toString().trim(), d.etDescripcionZona.text.toString().trim(),
            d.cbEsReservable.isChecked, aforo = aforo, horaApertura = apertura, horaCierre = cierre)
        val semanal = if (binding.swHorarioPorDia.isChecked) dias.filter { it.value.abierto }
            .map { (n, dia) -> DiaHorarioZona(n, dia.apertura, dia.cierre) } else emptyList()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val r = if (zonaId == null) RetrofitClient.instance.createZonaComun(zona) else RetrofitClient.instance.updateZonaComun(zonaId!!, zona)
                val guardada = r.body()
                if (!r.isSuccessful || guardada?.id == null) {
                    aviso(RetrofitClient.mensajeDeError(r, getString(R.string.zona_error_registrar))).show()
                    return@launch
                }
                val h = RetrofitClient.instance.guardarHorarioZona(guardada.id, HorarioSemanal(semanal))
                if (_binding == null) return@launch
                if (!h.isSuccessful) {
                    aviso(RetrofitClient.mensajeDeError(h, getString(R.string.zona_error_registrar))).show()
                    return@launch
                }
                findNavController().previousBackStackEntry?.savedStateHandle?.set(GUARDADA, true)
                findNavController().popBackStack()
            } catch (e: Exception) {
                if (_binding != null) aviso(RetrofitClient.mensajeDeExcepcion(e)).show()
            }
        }
    }

    private fun eliminar() {
        val id = zonaId ?: return
        confirmar(getString(R.string.zona_eliminar_titulo, binding.datosZona.etNombreZona.text), getString(R.string.zona_eliminar_mensaje),
            R.string.residente_eliminar_si) {
            ejecutarAccion(getString(R.string.zona_eliminada), { RetrofitClient.instance.deleteZona(id) }) {
                findNavController().previousBackStackEntry?.savedStateHandle?.set(GUARDADA, true)
                findNavController().popBackStack()
            }
        }
    }

    private fun elegirHora(inicial: String, titulo: Int, alElegir: (String) -> Unit) {
        val minutos = Agenda.minutos(inicial) ?: 8 * 60
        val selector = MaterialTimePicker.Builder()
            .setTimeFormat(if (DateFormat.is24HourFormat(requireContext())) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
            .setHour(minutos / 60).setMinute(minutos % 60).setTitleText(titulo).build()
        selector.addOnPositiveButtonClickListener { alElegir(Agenda.hora(selector.hour * 60 + selector.minute)) }
        selector.show(childFragmentManager, "hora")
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    companion object {
        const val GUARDADA = "zona_guardada"
        private const val ARG_ZONA = "zonaId"
        fun argumentos(zonaId: Long?) = bundleOf(ARG_ZONA to (zonaId ?: 0L))
    }
}
