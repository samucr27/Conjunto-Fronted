package com.example.conjuntoresidencial.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.conjuntoresidencial.databinding.FragmentControlObrasBinding
import com.example.conjuntoresidencial.util.Resource

class ControlObrasFragment : Fragment() {
    private var _binding: FragmentControlObrasBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ControlObrasViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentControlObrasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvObrasEnProceso.layoutManager = LinearLayoutManager(requireContext())
        viewModel.fetchObras()
        viewModel.obras.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressObrasProceso.visibility = View.VISIBLE
                    binding.tvNoObrasProceso.visibility = View.GONE
                    binding.rvObrasEnProceso.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.progressObrasProceso.visibility = View.GONE
                    val lista = resource.data ?: emptyList()
                    if (lista.isEmpty()) {
                        binding.tvNoObrasProceso.visibility = View.VISIBLE
                        binding.rvObrasEnProceso.visibility = View.GONE
                    } else {
                        binding.tvNoObrasProceso.visibility = View.GONE
                        binding.rvObrasEnProceso.visibility = View.VISIBLE
                        binding.rvObrasEnProceso.adapter = ObraAdapter(lista)
                    }
                }
                is Resource.Error -> {
                    binding.progressObrasProceso.visibility = View.GONE
                    binding.tvNoObrasProceso.visibility = View.VISIBLE
                    binding.tvNoObrasProceso.text = resource.message
                }
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}