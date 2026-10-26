package com.example.conjuntoresidencial.ui.propietario

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
import com.example.conjuntoresidencial.databinding.FragmentMisReservasBinding
import com.example.conjuntoresidencial.ui.admin.ReservaAdapter
import com.example.conjuntoresidencial.ui.admin.ReservaConZona
import com.example.conjuntoresidencial.ui.comun.mostrarRecurso
import com.example.conjuntoresidencial.util.SessionManager
import com.example.conjuntoresidencial.ui.comun.accionesDeLista
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.ui.comun.resumen

class MisReservasFragment : Fragment() {
    private var _binding: FragmentMisReservasBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MisReservasViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMisReservasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val aptoId = SessionManager(requireContext()).getApartamentoId()
        configurarEncabezado(
            binding.encabezado, R.string.menu_mis_reservas, R.string.menu_mis_reservas_sub,
            menu = R.menu.menu_lista,
            alElegir = accionesDeLista(binding.swipe) { viewModel.fetchReservas(aptoId) }
        )

        // El residente no aprueba ni rechaza; tocar una que no ha empezado la cancela
        val adaptador = ReservaAdapter(onTocar = { (reserva, zona) ->
            if (reserva.estado == "RECHAZADA") return@ReservaAdapter
            val cuando = Formatos.horario(reserva.fecha, reserva.hora, reserva.horaFin)
            confirmar(getString(R.string.reserva_cancelar_titulo), getString(R.string.reserva_cancelar_mensaje, zona, cuando),
                R.string.reserva_cancelar) {
                ejecutarAccion(getString(R.string.reserva_cancelada), { RetrofitClient.instance.deleteReserva(reserva.id!!) }) {
                    viewModel.fetchReservas(aptoId)
                }
            }
        })
        binding.rvReservasPropietario.layoutManager = LinearLayoutManager(requireContext())
        binding.rvReservasPropietario.adapter = adaptador
        binding.estado.vincular(binding.rvReservasPropietario)
        binding.swipe.setOnRefreshListener { viewModel.fetchReservas(aptoId) }

        viewModel.reservas.observe(viewLifecycleOwner) { resource ->
            binding.estado.mostrarRecurso(
                resource, binding.swipe, R.drawable.ic_event, getString(R.string.mis_reservas_vacio),
                getString(R.string.mis_reservas_vacio_mensaje),
                reintentar = { viewModel.fetchReservas(aptoId) }
            ) { lista ->
                adaptador.submitList(ReservaConZona.de(lista, viewModel.nombresZonas))
                binding.encabezado.ponerInfo(resumen(
                    R.plurals.resumen_reservas, lista.size,
                    R.plurals.resumen_pendientes to lista.count { it.estado == "PENDIENTE" }
                ))
            }
        }

        if (viewModel.reservas.value == null) viewModel.fetchReservas(aptoId)
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
