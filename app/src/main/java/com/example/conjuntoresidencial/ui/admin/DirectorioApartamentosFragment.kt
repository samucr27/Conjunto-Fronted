package com.example.conjuntoresidencial.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FragmentDirectorioApartamentosBinding
import com.example.conjuntoresidencial.util.Resource

class DirectorioApartamentosFragment : Fragment() {

    private var _binding: FragmentDirectorioApartamentosBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DirectorioViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDirectorioApartamentosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvApartamentos.layoutManager = LinearLayoutManager(requireContext())
        viewModel.fetchApartamentos()

        viewModel.aptos.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressAptos.visibility = View.VISIBLE
                    binding.tvNoAptos.visibility = View.GONE
                    binding.rvApartamentos.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.progressAptos.visibility = View.GONE
                    val lista = resource.data ?: emptyList()
                    if (lista.isEmpty()) {
                        binding.tvNoAptos.visibility = View.VISIBLE
                        binding.rvApartamentos.visibility = View.GONE
                    } else {
                        binding.tvNoAptos.visibility = View.GONE
                        binding.rvApartamentos.visibility = View.VISIBLE
                        binding.rvApartamentos.adapter = ApartamentoAdapter(lista) { apto ->
                            val bundle = Bundle().apply { putLong("aptoId", apto.id) }
                            findNavController().navigate(
                                R.id.action_directorioApartamentosFragment_to_detalleApartamentoFragment,
                                bundle
                            )
                        }
                    }
                }
                is Resource.Error -> {
                    binding.progressAptos.visibility = View.GONE
                    binding.tvNoAptos.visibility = View.VISIBLE
                    binding.tvNoAptos.text = resource.message
                }
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}