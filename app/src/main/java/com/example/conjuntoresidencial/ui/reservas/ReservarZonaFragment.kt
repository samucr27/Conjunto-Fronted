package com.example.conjuntoresidencial.ui.reservas

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FragmentReservarZonaBinding
import com.example.conjuntoresidencial.model.DisponibilidadDTO
import com.example.conjuntoresidencial.model.ReservaDTO
import com.example.conjuntoresidencial.ui.comun.EstadoVisual
import com.example.conjuntoresidencial.ui.comun.aparienciaDelTema
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.controlesFlotantes
import com.example.conjuntoresidencial.ui.comun.mostrarEstado
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.util.Formatos
import com.example.conjuntoresidencial.util.Resource
import com.example.conjuntoresidencial.util.SessionManager
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Reservar una zona común: el calendario del mes muestra de qué color es cada día (libre,
 * parcial, lleno); al elegir uno aparecen sus franjas de una hora y se toca la de inicio y la
 * de fin. Lo que ya reservaron otros se ve como "Ocupado" sin decir quién. La reserva queda
 * pendiente hasta que la administración la apruebe.
 */
class ReservarZonaFragment : Fragment() {
    private var _binding: FragmentReservarZonaBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ReservarZonaViewModel by viewModels()
    private val colombia = Locale("es", "CO")

    private val zonaId: Long get() = requireArguments().getLong(ARG_ZONA)
    private var boton: ExtendedFloatingActionButton? = null
    private var disponibilidad: DisponibilidadDTO? = null
    private var franjas: List<Agenda.Franja> = emptyList()

    private lateinit var dias: DiaAdapter
    private lateinit var franjasAdapter: FranjaAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReservarZonaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(binding.encabezado, requireArguments().getString(ARG_NOMBRE).orEmpty(), R.string.reservar_descripcion)

        ponerDiasDeLaSemana()
        ponerLeyenda()
        dias = DiaAdapter(::elegirDia)
        binding.rvDias.layoutManager = GridLayoutManager(requireContext(), 7)
        binding.rvDias.adapter = dias
        franjasAdapter = FranjaAdapter(::tocarFranja)
        binding.rvFranjas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFranjas.adapter = franjasAdapter

        val error = getString(R.string.reservar_error_disponibilidad)
        binding.btnMesAnterior.setOnClickListener { viewModel.moverMes(-1, zonaId, error) }
        binding.btnMesSiguiente.setOnClickListener { viewModel.moverMes(1, zonaId, error) }
        binding.etAsistentes.doAfterTextChanged {
            binding.tilAsistentes.error = null
            actualizarBoton()
        }

        boton = controlesFlotantes?.mostrarBotonFlotante(R.string.zona_reservar, R.drawable.ic_event) { reservar() }

        viewModel.disponibilidad.observe(viewLifecycleOwner) { resource ->
            binding.progresoDisponibilidad.visibility = if (resource is Resource.Loading) View.VISIBLE else View.GONE
            binding.tvErrorDisponibilidad.visibility = if (resource is Resource.Error) View.VISIBLE else View.GONE
            if (resource is Resource.Error) binding.tvErrorDisponibilidad.text = resource.message
            disponibilidad = (resource as? Resource.Success)?.data
            disponibilidad?.let(::mostrarZona)
            mostrarMes()
        }
        viewModel.resultado.observe(viewLifecycleOwner) { evento ->
            evento.siNoManejado { mensaje ->
                if (mensaje == null) {
                    // Toast: se ve mientras se vuelve a la lista de zonas
                    Toast.makeText(requireContext(), R.string.reserva_creada, Toast.LENGTH_LONG).show()
                    findNavController().navigateUp()
                } else {
                    aviso(mensaje).show()
                    viewModel.cargar(zonaId, error) // quizá otro la tomó: se actualiza el calendario
                }
            }
        }
        viewModel.enviando.observe(viewLifecycleOwner) { actualizarBoton() }

        if (viewModel.disponibilidad.value == null) viewModel.cargar(zonaId, error)
    }

    /** "Aforo de 40 personas · Abre de 8:00 a. m. a 11:00 p. m." en el encabezado. */
    private fun mostrarZona(d: DisponibilidadDTO) {
        val partes = listOfNotNull(
            d.aforo?.let { resources.getQuantityString(R.plurals.reservar_aforo, it, it) },
            if (d.horaApertura != null || d.horaCierre != null) getString(R.string.reservar_horario,
                Formatos.hora(d.horaApertura ?: "00:00"), Formatos.hora(d.horaCierre ?: "23:59")) else null
        )
        binding.encabezado.ponerInfo(partes.joinToString(" · ").ifBlank { null })
        binding.tilAsistentes.helperText = d.aforo?.let { resources.getQuantityString(R.plurals.reservar_maximo, it, it) }
    }

    private fun mostrarMes() {
        val calendario = Calendar.getInstance().apply { clear(); set(viewModel.anio, viewModel.mes, 1) }
        binding.tvMes.text = SimpleDateFormat("LLLL yyyy", colombia).format(calendario.time)
            .replaceFirstChar { it.titlecase(colombia) }
        binding.btnMesAnterior.isEnabled = viewModel.puedeRetroceder
        binding.btnMesSiguiente.isEnabled = viewModel.puedeAvanzar
        binding.btnMesAnterior.alpha = if (viewModel.puedeRetroceder) 1f else 0.3f
        binding.btnMesSiguiente.alpha = if (viewModel.puedeAvanzar) 1f else 0.3f

        val hoy = Calendar.getInstance()
        val hoyIso = iso(hoy)
        val ahora = hoy.get(Calendar.HOUR_OF_DAY) * 60 + hoy.get(Calendar.MINUTE)
        // Lunes primero: huecos antes del día 1
        val huecos = (calendario.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val ultimo = calendario.getActualMaximum(Calendar.DAY_OF_MONTH)
        val celdas = MutableList<DiaCalendario?>(huecos) { null }
        for (numero in 1..ultimo) {
            calendario.set(Calendar.DAY_OF_MONTH, numero)
            val fecha = iso(calendario)
            val estado = disponibilidad?.let {
                Agenda.estadoDia(franjasDe(it, fecha, if (fecha == hoyIso) ahora else null), pasado = fecha < hoyIso)
            }
            celdas += DiaCalendario(numero, fecha, estado, fecha == hoyIso, fecha == viewModel.diaElegido,
                descripcionDia(calendario, estado))
        }
        dias.submitList(celdas)
        mostrarFranjas()
    }

    private fun franjasDe(d: DisponibilidadDTO, fecha: String, ahora: Int?): List<Agenda.Franja> {
        val ocupadas = d.dias.firstOrNull { it.fecha == fecha }?.ocupadas.orEmpty().mapNotNull { f ->
            val inicio = Agenda.minutos(f.inicio) ?: return@mapNotNull null
            val fin = Agenda.minutos(f.fin) ?: return@mapNotNull null
            Agenda.Ocupada(inicio, fin, f.propia == true)
        }
        return Agenda.franjas(Agenda.minutos(d.horaApertura), Agenda.minutos(d.horaCierre), ocupadas, ahora)
    }

    private fun elegirDia(dia: DiaCalendario) {
        if (viewModel.diaElegido == dia.fecha) return
        viewModel.diaElegido = dia.fecha
        viewModel.seleccion = null
        mostrarMes()
        // Que se vea el horario del día recién elegido
        binding.desplazable.post { binding.desplazable.smoothScrollTo(0, binding.tarjetaHorario.top) }
    }

    private fun mostrarFranjas() {
        val d = disponibilidad
        val fecha = viewModel.diaElegido
        val visible = d != null && fecha != null
        binding.tarjetaHorario.visibility = if (visible) View.VISIBLE else View.GONE
        binding.tilAsistentes.visibility = if (visible) View.VISIBLE else View.GONE
        if (!visible) {
            actualizarBoton()
            return
        }
        val hoy = Calendar.getInstance()
        val ahora = hoy.get(Calendar.HOUR_OF_DAY) * 60 + hoy.get(Calendar.MINUTE)
        franjas = franjasDe(d!!, fecha!!, if (fecha == iso(hoy)) ahora else null)
        binding.tvDiaElegido.text = nombreDia(fecha)
        franjasAdapter.mostrar(franjas, viewModel.seleccion)
        mostrarResumen()
    }

    private fun tocarFranja(indice: Int) {
        viewModel.seleccion = Agenda.elegir(franjas, viewModel.seleccion, indice, MAXIMO_FRANJAS)
        franjasAdapter.mostrar(franjas, viewModel.seleccion)
        mostrarResumen()
    }

    private fun mostrarResumen() {
        val rango = viewModel.seleccion
        binding.tvResumenHorario.text = if (rango == null) getString(R.string.reservar_toca_inicio) else {
            val inicio = franjas[rango.first].inicio
            val fin = franjas[rango.last].fin
            getString(R.string.reservar_resumen, Formatos.hora(Agenda.hora(inicio)),
                Formatos.hora(Agenda.hora(fin % Agenda.DIA_COMPLETO)), duracion(fin - inicio))
        }
        actualizarBoton()
    }

    private fun duracion(minutos: Int): String {
        val horas = minutos / 60
        val resto = minutos % 60
        return if (resto == 0) resources.getQuantityString(R.plurals.reservar_horas, horas, horas)
            else getString(R.string.reservar_horas_minutos, horas, resto)
    }

    private fun actualizarBoton() {
        val listo = viewModel.seleccion != null && viewModel.diaElegido != null &&
            binding.etAsistentes.text?.toString()?.toIntOrNull() != null && viewModel.enviando.value != true
        boton?.isEnabled = listo
        boton?.alpha = if (listo) 1f else 0.5f
    }

    private fun reservar() {
        val rango = viewModel.seleccion ?: return
        val fecha = viewModel.diaElegido ?: return
        val asistentes = binding.etAsistentes.text?.toString()?.toIntOrNull()
        val aforo = disponibilidad?.aforo
        when {
            asistentes == null || asistentes < 1 -> {
                binding.tilAsistentes.error = getString(R.string.reservar_asistentes_minimo)
                return
            }
            aforo != null && asistentes > aforo -> {
                binding.tilAsistentes.error = resources.getQuantityString(R.plurals.reservar_aforo_excedido, aforo, aforo)
                return
            }
        }
        val fin = franjas[rango.last].fin
        viewModel.reservar(
            ReservaDTO(
                null, fecha, Agenda.hora(franjas[rango.first].inicio), "PENDIENTE", zonaId,
                SessionManager(requireContext()).getApartamentoId(),
                horaFin = Agenda.hora(if (fin >= Agenda.DIA_COMPLETO) Agenda.DIA_COMPLETO - 1 else fin),
                asistentes = asistentes
            ),
            getString(R.string.reserva_error_crear)
        )
    }

    private fun ponerDiasDeLaSemana() {
        val iniciales = resources.getStringArray(R.array.dias_semana_iniciales)
        iniciales.forEach { inicial ->
            binding.filaDiasSemana.addView(TextView(requireContext()).apply {
                text = inicial
                gravity = Gravity.CENTER
                aparienciaDelTema(com.google.android.material.R.attr.textAppearanceLabelMedium,
                    com.google.android.material.R.attr.colorOnSurfaceVariant)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
        }
    }

    private fun ponerLeyenda() {
        listOf(Agenda.EstadoDia.LIBRE to R.string.dia_libre, Agenda.EstadoDia.PARCIAL to R.string.dia_parcial,
            Agenda.EstadoDia.LLENO to R.string.dia_lleno).forEach { (estado, texto) ->
            binding.leyenda.addView(TextView(requireContext()).apply { mostrarEstado(EstadoVisual(texto, estado.tono())) })
        }
    }

    /** TalkBack: "sábado 25 de octubre, con horas libres". */
    private fun descripcionDia(dia: Calendar, estado: Agenda.EstadoDia?): String {
        val nombre = SimpleDateFormat("EEEE d 'de' MMMM", colombia).format(dia.time)
        val texto = when (estado) {
            Agenda.EstadoDia.LIBRE -> R.string.dia_libre_desc
            Agenda.EstadoDia.PARCIAL -> R.string.dia_parcial_desc
            Agenda.EstadoDia.LLENO -> R.string.dia_lleno_desc
            Agenda.EstadoDia.PASADO -> R.string.dia_pasado_desc
            null -> R.string.dia_cargando_desc
        }
        return "$nombre, ${getString(texto)}"
    }

    // "2026-10-25" → "Sábado 25 de octubre"
    private fun nombreDia(fecha: String): String {
        val partes = fecha.split("-").map { it.toInt() }
        val c = Calendar.getInstance().apply { clear(); set(partes[0], partes[1] - 1, partes[2]) }
        return SimpleDateFormat("EEEE d 'de' MMMM", colombia).format(c.time).replaceFirstChar { it.titlecase(colombia) }
    }

    private fun iso(c: Calendar) = String.format(Locale.US, "%04d-%02d-%02d",
        c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))

    override fun onDestroyView() {
        super.onDestroyView()
        boton = null
        _binding = null
    }

    companion object {
        const val ARG_ZONA = "zonaId"
        const val ARG_NOMBRE = "zonaNombre"
        // Lo máximo que deja el backend (app.reservas.duracion-maxima-horas)
        private const val MAXIMO_FRANJAS = 8

        fun argumentos(zonaId: Long, nombre: String) = bundleOf(ARG_ZONA to zonaId, ARG_NOMBRE to nombre)
    }
}
