package com.example.conjuntoresidencial.ui.propietario

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.FragmentMisObrasBinding
import com.example.conjuntoresidencial.model.ObraDTO
import com.example.conjuntoresidencial.util.Resource
import com.example.conjuntoresidencial.util.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        binding.rvMisObras.layoutManager = LinearLayoutManager(requireContext())
        aptoId = SessionManager(requireContext()).getApartamentoId()
        viewModel.fetchObras(aptoId)

        binding.swipeRefreshMisObras.setOnRefreshListener {
            viewModel.fetchObras(aptoId)
        }

        viewModel.obras.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressMisObras.visibility = View.VISIBLE
                    binding.tvNoMisObras.visibility = View.GONE
                    binding.rvMisObras.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.progressMisObras.visibility = View.GONE
                    binding.swipeRefreshMisObras.isRefreshing = false
                    val lista = resource.data ?: emptyList()
                    if (lista.isEmpty()) {
                        binding.tvNoMisObras.visibility = View.VISIBLE
                        binding.rvMisObras.visibility = View.GONE
                    } else {
                        binding.tvNoMisObras.visibility = View.GONE
                        binding.rvMisObras.visibility = View.VISIBLE
                        binding.rvMisObras.adapter = MisObrasAdapter(
                            lista,
                            onDetalle = { obra -> mostrarDetalleObra(obra) },
                            onFinalizar = { obra -> confirmarFinalizar(obra) }
                        )
                    }
                }
                is Resource.Error -> {
                    binding.progressMisObras.visibility = View.GONE
                    binding.swipeRefreshMisObras.isRefreshing = false
                    binding.tvNoMisObras.visibility = View.VISIBLE
                    binding.tvNoMisObras.text = resource.message
                }
            }
        }
    }

    private fun mostrarDetalleObra(obra: ObraDTO) {
        // Cargar personal de esa obra desde el backend
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getPersonalPorObra(obra.id!!)
                withContext(Dispatchers.Main) {
                    val personal = if (response.isSuccessful) response.body() ?: emptyList() else emptyList()

                    val personalTexto = if (personal.isEmpty()) {
                        "Sin personal registrado"
                    } else {
                        personal.joinToString("\n") { p ->
                            val arl = if (p.arlEstado) "✓ ARL" else "✗ Sin ARL"
                            "• ${p.nombreTrabajador} — CC ${p.cedula} — $arl"
                        }
                    }

                    AlertDialog.Builder(requireContext())
                        .setTitle(obra.descripcionAdecuacion)
                        .setMessage(
                            "Estado: ${obra.estadoObra}\n" +
                                    "Inicio: ${obra.fechaInicio}\n" +
                                    "Fin estimado: ${obra.fechaFinEstimada}\n\n" +
                                    "Personal externo:\n$personalTexto"
                        )
                        .setPositiveButton("Cerrar", null)
                        .show()
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    AlertDialog.Builder(requireContext())
                        .setTitle(obra.descripcionAdecuacion)
                        .setMessage(
                            "Estado: ${obra.estadoObra}\n" +
                                    "Inicio: ${obra.fechaInicio}\n" +
                                    "Fin estimado: ${obra.fechaFinEstimada}"
                        )
                        .setPositiveButton("Cerrar", null)
                        .show()
                }
            }
        }
    }

    private fun confirmarFinalizar(obra: ObraDTO) {
        AlertDialog.Builder(requireContext())
            .setTitle("Finalizar obra")
            .setMessage("¿Confirmas que la obra \"${obra.descripcionAdecuacion}\" ha finalizado?")
            .setPositiveButton("Sí, finalizar") { _, _ ->
                viewModel.finalizarObra(obra.id!!, obra)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}