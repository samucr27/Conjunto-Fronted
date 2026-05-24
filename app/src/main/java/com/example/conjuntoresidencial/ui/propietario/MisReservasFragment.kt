package com.example.conjuntoresidencial.ui.propietario

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.conjuntoresidencial.databinding.FragmentMisReservasBinding
import com.example.conjuntoresidencial.ui.admin.ReservaAdapter
import com.example.conjuntoresidencial.util.Resource
import com.example.conjuntoresidencial.util.SessionManager

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
        binding.rvReservasPropietario.layoutManager = LinearLayoutManager(requireContext())
        val aptoId = SessionManager(requireContext()).getApartamentoId()
        viewModel.fetchReservas(aptoId)

        binding.swipeRefreshReservas.setOnRefreshListener {
            viewModel.fetchReservas(aptoId)
        }

        viewModel.reservas.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressReservasProp.visibility = View.VISIBLE
                    binding.tvNoReservasProp.visibility = View.GONE
                    binding.rvReservasPropietario.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.progressReservasProp.visibility = View.GONE
                    binding.swipeRefreshReservas.isRefreshing = false
                    val lista = resource.data ?: emptyList()
                    if (lista.isEmpty()) {
                        binding.tvNoReservasProp.visibility = View.VISIBLE
                        binding.rvReservasPropietario.visibility = View.GONE
                    } else {
                        binding.tvNoReservasProp.visibility = View.GONE
                        binding.rvReservasPropietario.visibility = View.VISIBLE
                        binding.rvReservasPropietario.adapter = ReservaAdapter(lista)
                    }
                }
                is Resource.Error -> {
                    binding.progressReservasProp.visibility = View.GONE
                    binding.swipeRefreshReservas.isRefreshing = false
                    binding.tvNoReservasProp.visibility = View.VISIBLE
                    binding.tvNoReservasProp.text = resource.message
                }
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}