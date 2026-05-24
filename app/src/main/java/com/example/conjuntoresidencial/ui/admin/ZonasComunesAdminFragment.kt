package com.example.conjuntoresidencial.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FragmentZonasComunesAdminBinding
import com.example.conjuntoresidencial.util.Resource
import com.google.android.material.textfield.TextInputEditText

class ZonasComunesAdminFragment : Fragment() {
    private var _binding: FragmentZonasComunesAdminBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ZonasAdminViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentZonasComunesAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvZonasAdmin.layoutManager = LinearLayoutManager(requireContext())
        viewModel.fetchZonas()

        binding.swipeRefreshZonasAdmin.setOnRefreshListener {
            viewModel.fetchZonas()
        }

        viewModel.zonas.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> binding.rvZonasAdmin.visibility = View.GONE
                is Resource.Success -> {
                    binding.swipeRefreshZonasAdmin.isRefreshing = false
                    binding.rvZonasAdmin.visibility = View.VISIBLE
                    binding.rvZonasAdmin.adapter = ZonaAdapter(resource.data ?: emptyList())
                }
                is Resource.Error -> {
                    binding.swipeRefreshZonasAdmin.isRefreshing = false
                    binding.rvZonasAdmin.visibility = View.GONE
                }
            }
        }

        binding.btnRegistrarZona.setOnClickListener {
            val dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_registrar_zona, null)
            AlertDialog.Builder(requireContext())
                .setTitle("Nueva Zona Común")
                .setView(dialogView)
                .setPositiveButton("Registrar") { _, _ ->
                    val nombre = dialogView.findViewById<TextInputEditText>(R.id.etNombreZona).text.toString()
                    val estado = dialogView.findViewById<TextInputEditText>(R.id.etEstadoZona).text.toString()
                    val descripcion = dialogView.findViewById<TextInputEditText>(R.id.etDescripcionZona).text.toString()
                    val esReservable = dialogView.findViewById<CheckBox>(R.id.cbEsReservable).isChecked
                    if (nombre.isNotBlank()) {
                        viewModel.registrarZona(nombre, estado, descripcion, esReservable)
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}