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
import com.example.conjuntoresidencial.ui.comun.mostrarRecurso
import com.example.conjuntoresidencial.util.configurarBarra

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
        configurarBarra(binding.barra.toolbar, R.string.directorio_titulo, R.string.directorio_subtitulo)

        val adaptador = ApartamentoAdapter { apto ->
            val bundle = Bundle().apply { putLong("aptoId", apto.id) }
            findNavController().navigate(R.id.action_directorioApartamentosFragment_to_detalleApartamentoFragment, bundle)
        }
        binding.rvApartamentos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvApartamentos.adapter = adaptador
        binding.estado.vincular(binding.rvApartamentos)
        binding.swipe.setOnRefreshListener { viewModel.fetchApartamentos() }

        viewModel.aptos.observe(viewLifecycleOwner) { resource ->
            binding.estado.mostrarRecurso(
                resource, binding.swipe, R.drawable.ic_apartment, getString(R.string.directorio_vacio),
                reintentar = { viewModel.fetchApartamentos() }
            ) { adaptador.submitList(it) }
        }
        // Al volver del detalle o girar la pantalla se reutiliza lo ya cargado
        if (viewModel.aptos.value == null) viewModel.fetchApartamentos()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
