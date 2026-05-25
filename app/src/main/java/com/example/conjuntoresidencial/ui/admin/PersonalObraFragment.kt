package com.example.conjuntoresidencial.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.conjuntoresidencial.databinding.FragmentPersonalObraBinding
import com.example.conjuntoresidencial.util.Resource

class PersonalObraFragment : Fragment() {
    private var _binding: FragmentPersonalObraBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PersonalObraViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPersonalObraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvPersonalObra.layoutManager = LinearLayoutManager(requireContext())
        viewModel.fetchPersonal()

        viewModel.personal.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressPersonal.visibility = View.VISIBLE
                    binding.tvNoPersonal.visibility = View.GONE
                    binding.rvPersonalObra.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.progressPersonal.visibility = View.GONE
                    val lista = resource.data ?: emptyList()
                    if (lista.isEmpty()) {
                        binding.tvNoPersonal.visibility = View.VISIBLE
                        binding.rvPersonalObra.visibility = View.GONE
                    } else {
                        binding.tvNoPersonal.visibility = View.GONE
                        binding.rvPersonalObra.visibility = View.VISIBLE
                        binding.rvPersonalObra.adapter = PersonalAdapter(lista)
                    }
                }
                is Resource.Error -> {
                    binding.progressPersonal.visibility = View.GONE
                    binding.tvNoPersonal.visibility = View.VISIBLE
                    binding.tvNoPersonal.text = resource.message
                }
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}