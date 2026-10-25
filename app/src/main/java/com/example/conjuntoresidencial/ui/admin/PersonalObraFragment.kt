package com.example.conjuntoresidencial.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FragmentPersonalObraBinding
import com.example.conjuntoresidencial.ui.comun.mostrarRecurso
import com.example.conjuntoresidencial.ui.comun.accionesDeLista
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.ui.comun.resumen

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
        configurarEncabezado(
            binding.encabezado, R.string.personal_titulo, R.string.personal_subtitulo, menu = R.menu.menu_lista,
            alElegir = accionesDeLista(binding.swipe) { viewModel.fetchPersonal() }
        )

        val adaptador = PersonalAdapter { p -> opcionesPersonal(p) { viewModel.fetchPersonal() } }
        binding.rvPersonalObra.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPersonalObra.adapter = adaptador
        binding.estado.vincular(binding.rvPersonalObra)
        binding.swipe.setOnRefreshListener { viewModel.fetchPersonal() }

        viewModel.personal.observe(viewLifecycleOwner) { resource ->
            binding.estado.mostrarRecurso(
                resource, binding.swipe, R.drawable.ic_people, getString(R.string.personal_vacio),
                reintentar = { viewModel.fetchPersonal() }
            ) { lista ->
                adaptador.submitList(lista)
                binding.encabezado.ponerInfo(resumen(
                    R.plurals.resumen_trabajadores, lista.size,
                    R.plurals.resumen_sin_arl to lista.count { !it.arlEstado }
                ))
            }
        }
        if (viewModel.personal.value == null) viewModel.fetchPersonal()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
