package com.example.conjuntoresidencial.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.conjuntoresidencial.databinding.FragmentReservasAdminBinding
import com.example.conjuntoresidencial.util.Resource

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
        binding.rvReservas.layoutManager = LinearLayoutManager(requireContext())
        viewModel.fetchReservas()

        binding.swipeRefreshReservasAdmin.setOnRefreshListener {
            viewModel.fetchReservas()
        }

        viewModel.reservas.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.tvNoReservasGlobal.visibility = View.GONE
                    binding.rvReservas.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefreshReservasAdmin.isRefreshing = false
                    val lista = resource.data ?: emptyList()
                    if (lista.isEmpty()) {
                        binding.tvNoReservasGlobal.visibility = View.VISIBLE
                        binding.rvReservas.visibility = View.GONE
                    } else {
                        binding.tvNoReservasGlobal.visibility = View.GONE
                        binding.rvReservas.visibility = View.VISIBLE
                        binding.rvReservas.adapter = ReservaAdapter(
                            lista,
                            onAprobar = { reserva ->
                                viewModel.actualizarEstado(reserva.id!!, "APROBADA")
                            },
                            onRechazar = { reserva ->
                                viewModel.actualizarEstado(reserva.id!!, "RECHAZADA")
                            }
                        )
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefreshReservasAdmin.isRefreshing = false
                    binding.tvNoReservasGlobal.visibility = View.VISIBLE
                    binding.tvNoReservasGlobal.text = resource.message
                }
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}