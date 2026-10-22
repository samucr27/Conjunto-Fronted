package com.example.conjuntoresidencial.ui.admin

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

        val adaptador = ReservaAdapter(
            onAprobar = { reserva -> viewModel.actualizarEstado(reserva.id!!, "APROBADA") },
            onRechazar = { reserva -> viewModel.actualizarEstado(reserva.id!!, "RECHAZADA") }
        )
        binding.rvReservas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvReservas.adapter = adaptador
        binding.estado.vincular(binding.rvReservas)
        binding.swipe.setOnRefreshListener { viewModel.fetchReservas() }

        viewModel.reservas.observe(viewLifecycleOwner) { resource ->
            binding.estado.mostrarRecurso(
                resource, binding.swipe, R.drawable.ic_event, getString(R.string.reservas_vacio),
                reintentar = { viewModel.fetchReservas() }
            ) { lista ->
                adaptador.submitList(ReservaConZona.de(lista, viewModel.nombresZonas))
                binding.encabezado.ponerInfo(resumen(
                    R.plurals.resumen_reservas, lista.size,
                    R.plurals.resumen_pendientes to lista.count { it.estado == "PENDIENTE" }
                ))
            }
        }

        viewModel.mensaje.observe(viewLifecycleOwner) { evento ->
            evento.obtenerSiNoManejado()?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }
        if (viewModel.reservas.value == null) viewModel.fetchReservas()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
