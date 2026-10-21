package com.example.conjuntoresidencial.ui.propietario

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
import com.example.conjuntoresidencial.util.configurarBarra

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
        configurarBarra(binding.barra.toolbar, R.string.menu_mis_reservas, R.string.menu_mis_reservas_sub)
        val aptoId = SessionManager(requireContext()).getApartamentoId()

        // Sin callbacks: el residente solo ve el estado, no aprueba ni rechaza
        val adaptador = ReservaAdapter()
        binding.rvReservasPropietario.layoutManager = LinearLayoutManager(requireContext())
        binding.rvReservasPropietario.adapter = adaptador
        binding.estado.vincular(binding.rvReservasPropietario)
        binding.swipe.setOnRefreshListener { viewModel.fetchReservas(aptoId) }

        viewModel.reservas.observe(viewLifecycleOwner) { resource ->
            binding.estado.mostrarRecurso(
                resource, binding.swipe, R.drawable.ic_event, getString(R.string.mis_reservas_vacio),
                getString(R.string.mis_reservas_vacio_mensaje),
                reintentar = { viewModel.fetchReservas(aptoId) }
            ) { adaptador.submitList(ReservaConZona.de(it, viewModel.nombresZonas)) }
        }

        if (viewModel.reservas.value == null) viewModel.fetchReservas(aptoId)
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
