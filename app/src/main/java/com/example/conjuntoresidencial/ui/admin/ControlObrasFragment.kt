package com.example.conjuntoresidencial.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FragmentControlObrasBinding
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.debajoDeLosBotones
import com.example.conjuntoresidencial.ui.comun.mostrarRecurso
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.ui.comun.resumen
import com.example.conjuntoresidencial.ui.obras.ObraDetalleFragment
import com.example.conjuntoresidencial.ui.propietario.MisObrasAdapter
import com.example.conjuntoresidencial.ui.propietario.RegistrarObraFragment
import com.google.android.material.chip.Chip

/**
 * Obras (administración, P5): las de cada apartamento o las del conjunto, buscando por torre y apto.
 * Las finalizadas van en su propia pantalla (menú ⋮); al tocar una se administra en su detalle.
 */
class ControlObrasFragment : Fragment() {
    private var _binding: FragmentControlObrasBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ControlObrasViewModel by viewModels()
    private val finalizadas get() = arguments?.getBoolean(ARG_FINALIZADAS) == true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentControlObrasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(
            binding.encabezado,
            if (finalizadas) R.string.obras_finalizadas else R.string.obras_titulo,
            if (finalizadas) R.string.obras_finalizadas_sub else R.string.obras_subtitulo,
            menu = if (finalizadas) R.menu.menu_lista else R.menu.menu_obras_admin
        ) { id ->
            when (id) {
                R.id.accion_actualizar -> { binding.swipe.isRefreshing = true; cargar(); true }
                R.id.accion_finalizadas -> {
                    findNavController().navigate(R.id.controlObrasFragment, bundleOf(ARG_FINALIZADAS to true)); true
                }
                R.id.accion_obra_conjunto -> {
                    findNavController().navigate(R.id.registrarObraFragment, bundleOf(RegistrarObraFragment.ARG_CONJUNTO to true)); true
                }
                else -> false
            }
        }

        val adaptador = MisObrasAdapter(
            onDetalle = { obra ->
                findNavController().navigate(R.id.obraDetalleFragment, bundleOf(ObraDetalleFragment.ARG_OBRA to obra.id))
            },
            onFinalizar = null,
            conLugar = true
        )
        binding.rvObrasEnProceso.layoutManager = LinearLayoutManager(requireContext())
        binding.rvObrasEnProceso.adapter = adaptador
        binding.estado.vincular(binding.rvObrasEnProceso)
        binding.swipe.setOnRefreshListener { cargar() }
        // La lista no es hija directa del swipe: solo se actualiza al jalar desde arriba de la lista
        binding.swipe.setOnChildScrollUpCallback { _, _ -> binding.rvObrasEnProceso.canScrollVertically(-1) }
        debajoDeLosBotones(binding.cabeceraObras)

        mostrarAlcances()
        binding.etBuscarObra.setText(viewModel.texto)
        binding.etBuscarObra.doAfterTextChanged {
            viewModel.texto = it?.toString().orEmpty()
            if (viewModel.obras.value != null) viewModel.filtrar()
        }

        viewModel.obras.observe(viewLifecycleOwner) { resource ->
            binding.estado.mostrarRecurso(
                resource, binding.swipe, R.drawable.ic_build,
                getString(when {
                    viewModel.hayBusqueda -> R.string.obras_sin_resultados
                    finalizadas -> R.string.obras_finalizadas_vacio
                    else -> R.string.obras_vacio
                }),
                reintentar = { cargar() }
            ) { lista ->
                adaptador.submitList(lista)
                binding.encabezado.ponerInfo(resumen(
                    R.plurals.resumen_obras, lista.size,
                    R.plurals.obras_personal_pendiente to lista.sumOf { it.personalPendiente ?: 0 }
                ))
            }
        }
        // También al volver del detalle (la vista se crea otra vez): la obra pudo cambiar
        cargar()
    }

    private fun cargar() = viewModel.fetchObras(finalizadas)

    private fun mostrarAlcances() {
        binding.chipsAlcance.removeAllViews()
        listOf(R.string.obras_apartamentos to ControlObrasViewModel.APARTAMENTOS,
            R.string.obras_conjunto to ControlObrasViewModel.CONJUNTO).forEach { (texto, alcance) ->
            binding.chipsAlcance.addView(Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle).apply {
                setText(texto)
                isCheckable = true
                isChecked = alcance == viewModel.alcance
                setOnClickListener {
                    if (viewModel.alcance == alcance) return@setOnClickListener
                    viewModel.alcance = alcance
                    cargar()
                }
            })
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    companion object {
        const val ARG_FINALIZADAS = "finalizadas"
    }
}
