package com.example.conjuntoresidencial.ui.admin

import com.example.conjuntoresidencial.ui.obras.opcionesPersonal

import com.example.conjuntoresidencial.ui.obras.PersonalAdapter

import com.example.conjuntoresidencial.ui.comun.BarraBusqueda
import com.example.conjuntoresidencial.ui.comun.repetir
import com.example.conjuntoresidencial.util.map
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

        val barra = BarraBusqueda(this, binding.busqueda, R.string.busqueda_personal, listOf(
            R.string.busqueda_todos to null, R.string.busqueda_por_aprobar to "PENDIENTE", R.string.busqueda_sin_certificado to "SIN_ARL"
        )) { viewModel.personal.repetir() }
        barra.vincular(binding.swipe, binding.rvPersonalObra)
        viewModel.personal.observe(viewLifecycleOwner) { todo ->
            val resource = todo.map { lista ->
                lista.filter { p ->
                    barra.coincide(p.nombreTrabajador, p.cedula, p.torrePiso, p.aptoNum) && when (barra.filtro) {
                        "PENDIENTE" -> p.aprobacion == "PENDIENTE"
                        "SIN_ARL" -> p.tieneCertificado != true
                        else -> true
                    }
                }
            }
            binding.estado.mostrarRecurso(
                resource, binding.swipe, R.drawable.ic_people,
                if (barra.activa || barra.filtro != null) getString(R.string.busqueda_sin_resultados) else getString(R.string.personal_vacio),
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
