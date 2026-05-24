package com.example.conjuntoresidencial.ui.propietario

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.conjuntoresidencial.databinding.FragmentMisObrasBinding
import com.example.conjuntoresidencial.model.ObraDTO
import com.example.conjuntoresidencial.util.Resource
import com.example.conjuntoresidencial.util.SessionManager

class MisObrasFragment : Fragment() {
    private var _binding: FragmentMisObrasBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MisObrasViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMisObrasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvMisObras.layoutManager = LinearLayoutManager(requireContext())
        val aptoId = SessionManager(requireContext()).getApartamentoId()
        viewModel.fetchObras(aptoId)

        binding.swipeRefreshMisObras.setOnRefreshListener {
            viewModel.fetchObras(aptoId)
        }

        viewModel.obras.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressMisObras.visibility = View.VISIBLE
                    binding.tvNoMisObras.visibility = View.GONE
                    binding.rvMisObras.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.progressMisObras.visibility = View.GONE
                    binding.swipeRefreshMisObras.isRefreshing = false
                    val lista = resource.data ?: emptyList()
                    if (lista.isEmpty()) {
                        binding.tvNoMisObras.visibility = View.VISIBLE
                        binding.rvMisObras.visibility = View.GONE
                    } else {
                        binding.tvNoMisObras.visibility = View.GONE
                        binding.rvMisObras.visibility = View.VISIBLE
                        binding.rvMisObras.adapter = MisObrasAdapter(lista) { obra ->
                            mostrarDetalleObra(obra)
                        }
                    }
                }
                is Resource.Error -> {
                    binding.progressMisObras.visibility = View.GONE
                    binding.swipeRefreshMisObras.isRefreshing = false
                    binding.tvNoMisObras.visibility = View.VISIBLE
                    binding.tvNoMisObras.text = resource.message
                }
            }
        }
    }

    private fun mostrarDetalleObra(obra: ObraDTO) {
        AlertDialog.Builder(requireContext())
            .setTitle(obra.descripcionAdecuacion)
            .setMessage(
                "Estado: ${obra.estadoObra}\n" +
                        "Inicio: ${obra.fechaInicio}\n" +
                        "Fin estimado: ${obra.fechaFinEstimada}"
            )
            .setPositiveButton("Cerrar", null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}