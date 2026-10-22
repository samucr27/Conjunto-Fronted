package com.example.conjuntoresidencial.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FragmentControlObrasBinding
import com.example.conjuntoresidencial.ui.comun.mostrarRecurso
import com.example.conjuntoresidencial.ui.comun.accionesDeLista
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.ui.comun.resumen
import com.google.android.material.snackbar.Snackbar

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
        configurarEncabezado(
            binding.encabezado, R.string.obras_titulo, R.string.obras_subtitulo, menu = R.menu.menu_lista,
            alElegir = accionesDeLista(binding.swipe) { viewModel.fetchObras() }
        )

        // Un solo adaptador: submitList conserva el scroll y solo redibuja lo que cambió
        val adaptador = ObraConPersonalAdapter { obraId, nuevoEstado -> viewModel.cambiarEstado(obraId, nuevoEstado) }
        binding.rvObrasEnProceso.layoutManager = LinearLayoutManager(requireContext())
        binding.rvObrasEnProceso.adapter = adaptador
        binding.estado.vincular(binding.rvObrasEnProceso)
        binding.swipe.setOnRefreshListener { viewModel.fetchObras() }

        viewModel.obras.observe(viewLifecycleOwner) { resource ->
            binding.estado.mostrarRecurso(
                resource, binding.swipe, R.drawable.ic_build, getString(R.string.obras_vacio),
                reintentar = { viewModel.fetchObras() }
            ) { lista ->
                adaptador.submitList(lista.map { obra ->
                    ObraConPersonal(obra, obra.id?.let { viewModel.personalPorObra[it] }.orEmpty())
                })
                binding.encabezado.ponerInfo(resumen(
                    R.plurals.resumen_obras, lista.size,
                    R.plurals.resumen_en_proceso to lista.count { it.estadoObra == "EN_PROCESO" }
                ))
            }
        }

        viewModel.mensaje.observe(viewLifecycleOwner) { evento ->
            evento.obtenerSiNoManejado()?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }
        if (viewModel.obras.value == null) viewModel.fetchObras()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
