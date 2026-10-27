package com.example.conjuntoresidencial.ui.admin

import com.example.conjuntoresidencial.ui.comun.BarraBusqueda
import com.example.conjuntoresidencial.ui.comun.repetir
import com.example.conjuntoresidencial.util.map
import com.example.conjuntoresidencial.ui.comun.elegirOpcion
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.util.Formatos
import com.example.conjuntoresidencial.ui.comun.confirmar
import com.example.conjuntoresidencial.ui.comun.ejecutarAccion
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FragmentReservasAdminBinding
import com.example.conjuntoresidencial.ui.comun.mostrarRecurso
import com.example.conjuntoresidencial.ui.comun.accionesDeLista
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.ui.comun.resumen
import com.example.conjuntoresidencial.ui.comun.aviso
import com.google.android.material.snackbar.Snackbar

class ReservasAdminFragment : Fragment() {
    private var _binding: FragmentReservasAdminBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ReservasAdminViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReservasAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(
            binding.encabezado, R.string.reservas_titulo, R.string.reservas_subtitulo, menu = R.menu.menu_lista,
            alElegir = accionesDeLista(binding.swipe) { viewModel.fetchReservas() }
        )

        // Abierta desde una notificación: esa reserva va resaltada y se desplaza hasta ella
        val resaltada = arguments?.getLong(ARG_RESALTAR)?.takeIf { arguments?.containsKey(ARG_RESALTAR) == true }
        var desplazada = false
        val adaptador = ReservaAdapter(
            onAprobar = { reserva -> viewModel.actualizarEstado(reserva.id!!, "APROBADA") },
            onRechazar = { reserva -> viewModel.actualizarEstado(reserva.id!!, "RECHAZADA") },
            resaltada = resaltada,
            onTocar = { (reserva, zona) ->
                val cuando = Formatos.horario(reserva.fecha, reserva.hora, reserva.horaFin)
                elegirOpcion("$zona · $cuando", listOfNotNull<Pair<Int, () -> Unit>>(
                    (R.string.reserva_aprobar to { viewModel.actualizarEstado(reserva.id!!, "APROBADA") }).takeIf { reserva.estado != "APROBADA" },
                    (R.string.reserva_rechazar to { viewModel.actualizarEstado(reserva.id!!, "RECHAZADA") }).takeIf { reserva.estado != "RECHAZADA" },
                    R.string.residente_eliminar_si to {
                        confirmar(getString(R.string.reserva_eliminar_titulo), getString(R.string.reserva_cancelar_mensaje, zona, cuando),
                            R.string.residente_eliminar_si) {
                            ejecutarAccion(getString(R.string.reserva_eliminada), { RetrofitClient.instance.deleteReserva(reserva.id!!) }) {
                                viewModel.fetchReservas()
                            }
                        }
                    }
                ))
            }
        )
        ponerFiltros()
        binding.rvReservas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvReservas.adapter = adaptador
        binding.estado.vincular(binding.rvReservas)
        binding.swipe.setOnRefreshListener { viewModel.fetchReservas() }

        val barra = BarraBusqueda(this, binding.busqueda, R.string.busqueda_reservas) { viewModel.reservas.repetir() }
        barra.vincular(binding.swipe, binding.rvReservas)
        viewModel.reservas.observe(viewLifecycleOwner) { todo ->
            val resource = todo.map { lista ->
                lista.filter { r ->
                    barra.coincide(viewModel.nombresZonas[r.zonaComunId], com.example.conjuntoresidencial.util.Formatos.fecha(r.fecha), r.fecha)
                }
            }
            binding.estado.mostrarRecurso(
                resource, binding.swipe, R.drawable.ic_event,
                if (barra.activa) getString(R.string.busqueda_sin_resultados) else getString(R.string.reservas_vacio),
                reintentar = { viewModel.fetchReservas() }
            ) { todas ->
                // P5: filtro por estado (chips)
                val lista = todas.filter { filtroEstado == null || it.estado == filtroEstado }
                adaptador.submitList(ReservaConZona.de(lista, viewModel.nombresZonas)) {
                    val posicion = lista.indexOfFirst { it.id == resaltada }
                    if (!desplazada && posicion >= 0) {
                        desplazada = true
                        binding.rvReservas.scrollToPosition(posicion)
                    }
                }
                binding.encabezado.ponerInfo(resumen(
                    R.plurals.resumen_reservas, lista.size,
                    R.plurals.resumen_pendientes to lista.count { it.estado == "PENDIENTE" }
                ))
            }
        }

        viewModel.mensaje.observe(viewLifecycleOwner) { evento ->
            evento.obtenerSiNoManejado()?.let {
                aviso(it, Snackbar.LENGTH_LONG).show()
            }
        }
        if (viewModel.reservas.value == null) viewModel.fetchReservas()
    }

    private var filtroEstado: String? = null

    /** Todas, pendientes, aprobadas o rechazadas. */
    private fun ponerFiltros() {
        binding.chipsEstado.removeAllViews()
        listOf<Pair<Int, String?>>(R.string.reservas_todas to null, R.string.reservas_pendientes to "PENDIENTE",
            R.string.reservas_aprobadas to "APROBADA", R.string.reservas_rechazadas to "RECHAZADA").forEach { (texto, estado) ->
            binding.chipsEstado.addView(com.google.android.material.chip.Chip(requireContext(), null,
                com.google.android.material.R.attr.chipStyle).apply {
                setText(texto)
                isCheckable = true
                isChecked = estado == filtroEstado
                setOnClickListener {
                    filtroEstado = estado
                    ponerFiltros()
                    viewModel.repintar()
                }
            })
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    companion object {
        /** Id de la reserva que se abrió desde una notificación. */
        const val ARG_RESALTAR = "reservaResaltada"
    }
}
