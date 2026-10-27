package com.example.conjuntoresidencial.ui.propietario

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.ui.obras.ObraDetalleFragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FragmentMisObrasBinding
import com.example.conjuntoresidencial.model.ObraDTO
import com.example.conjuntoresidencial.ui.comun.mostrarRecurso
import com.example.conjuntoresidencial.util.SessionManager
import com.example.conjuntoresidencial.ui.comun.accionesDeLista
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.ui.comun.resumen
import com.example.conjuntoresidencial.ui.comun.aviso
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class MisObrasFragment : Fragment() {
    private var _binding: FragmentMisObrasBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MisObrasViewModel by viewModels()
    private var aptoId: Long = -1L

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMisObrasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(
            binding.encabezado, R.string.menu_mis_obras, R.string.mis_obras_subtitulo, menu = R.menu.menu_lista,
            alElegir = accionesDeLista(binding.swipe) { viewModel.fetchObras(aptoId) }
        )
        aptoId = SessionManager(requireContext()).getApartamentoId()

        val adaptador = MisObrasAdapter(
            onDetalle = { obra ->
                findNavController().navigate(R.id.obraDetalleFragment, bundleOf(ObraDetalleFragment.ARG_OBRA to obra.id))
            },
            onFinalizar = { obra -> confirmarFinalizar(obra) }
        )
        binding.rvMisObras.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMisObras.adapter = adaptador
        binding.estado.vincular(binding.rvMisObras)
        binding.swipe.setOnRefreshListener { viewModel.fetchObras(aptoId) }

        viewModel.obras.observe(viewLifecycleOwner) { resource ->
            binding.estado.mostrarRecurso(
                resource, binding.swipe, R.drawable.ic_build, getString(R.string.mis_obras_vacio),
                getString(R.string.mis_obras_vacio_mensaje),
                reintentar = { viewModel.fetchObras(aptoId) }
            ) { lista ->
                adaptador.submitList(lista)
                binding.encabezado.ponerInfo(resumen(
                    R.plurals.resumen_obras, lista.size,
                    R.plurals.resumen_en_proceso to lista.count { it.estadoObra == "EN_PROCESO" }
                ))
            }
        }

        viewModel.mensaje.observe(viewLifecycleOwner) { evento ->
            evento.obtenerSiNoManejado()?.let {
                aviso(it, Snackbar.LENGTH_LONG).show()
            }
        }

        // También al volver del detalle (la vista se crea otra vez): la obra pudo cambiar
        viewModel.fetchObras(aptoId)
    }

    private fun confirmarFinalizar(obra: ObraDTO) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.obra_finalizar_titulo)
            .setMessage(getString(R.string.obra_finalizar_pregunta, obra.descripcionAdecuacion))
            .setPositiveButton(R.string.obra_finalizar_si) { _, _ ->
                viewModel.finalizarObra(obra.id!!, obra, getString(R.string.obra_error_finalizar))
            }
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
