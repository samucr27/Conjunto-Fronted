package com.example.conjuntoresidencial.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.conjuntoresidencial.databinding.FragmentCarteraAdminBinding
import com.example.conjuntoresidencial.util.Resource
import com.google.android.material.snackbar.Snackbar

class CarteraAdminFragment : Fragment() {

    private var _binding: FragmentCarteraAdminBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CarteraAdminViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCarteraAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvCarteraAdmin.layoutManager = LinearLayoutManager(requireContext())

        viewModel.cartera.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                // El layout aún no tiene indicador de carga ni estado vacío (pendiente de P3)
                is Resource.Loading -> Unit
                is Resource.Success ->
                    binding.rvCarteraAdmin.adapter = CarteraAdminAdapter(resource.data.orEmpty())
                is Resource.Error ->
                    Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_LONG).show()
            }
        }

        // Al girar la pantalla se reutiliza lo ya cargado
        if (viewModel.cartera.value == null) viewModel.fetchCartera()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
