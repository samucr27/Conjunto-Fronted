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
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FragmentZonasComunesAdminBinding
import com.example.conjuntoresidencial.ui.comun.mostrarRecurso
import com.example.conjuntoresidencial.ui.comun.accionesDeLista
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.controlesFlotantes
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.ui.comun.resumen
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

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
        configurarEncabezado(
            binding.encabezado, R.string.zonas_titulo, R.string.zonas_subtitulo, menu = R.menu.menu_lista,
            alElegir = accionesDeLista(binding.swipe) { viewModel.fetchZonas() }
        )

        val adaptador = ZonaAdapter()
        binding.rvZonasAdmin.layoutManager = LinearLayoutManager(requireContext())
        binding.rvZonasAdmin.adapter = adaptador
        binding.estado.vincular(binding.rvZonasAdmin)
        binding.swipe.setOnRefreshListener { viewModel.fetchZonas() }

        viewModel.zonas.observe(viewLifecycleOwner) { resource ->
            binding.estado.mostrarRecurso(
                resource, binding.swipe, R.drawable.ic_place, getString(R.string.zonas_vacio),
                reintentar = { viewModel.fetchZonas() }
            ) { lista ->
                adaptador.submitList(lista)
                binding.encabezado.ponerInfo(resumen(
                    R.plurals.resumen_zonas, lista.size,
                    R.plurals.resumen_reservables to lista.count { it.esReservable == true }
                ))
            }
        }

        viewModel.registro.observe(viewLifecycleOwner) { evento ->
            evento.obtenerSiNoManejado()?.let { error ->
                Snackbar.make(binding.root, error ?: getString(R.string.zona_registrada), Snackbar.LENGTH_LONG).show()
            }
        }

        // Botón flotante (encima del difuminado); se encoge a solo ícono al desplazar hacia abajo
        controlesFlotantes?.mostrarBotonFlotante(R.string.zonas_registrar, R.drawable.ic_add) { mostrarDialogRegistrar() }?.let { boton ->
            binding.rvZonasAdmin.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                    if (dy > 0) boton.shrink() else if (dy < 0) boton.extend()
                }
            })
        }

        if (viewModel.zonas.value == null) viewModel.fetchZonas()
    }

    private fun mostrarDialogRegistrar() {
        val vista = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_registrar_zona, null)
        val tilNombre = vista.findViewById<TextInputLayout>(R.id.tilNombreZona)
        val dialogo = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.zona_nueva_titulo)
            .setView(vista)
            .setPositiveButton(R.string.btn_registrar, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()

        // Se asigna después de show() para que el diálogo no se cierre si falta el nombre
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val nombre = vista.findViewById<TextInputEditText>(R.id.etNombreZona).text.toString().trim()
            if (nombre.isBlank()) {
                tilNombre.error = getString(R.string.zona_nombre_obligatorio)
                return@setOnClickListener
            }
            viewModel.registrarZona(
                nombre,
                vista.findViewById<TextInputEditText>(R.id.etEstadoZona).text.toString().trim(),
                vista.findViewById<TextInputEditText>(R.id.etDescripcionZona).text.toString().trim(),
                vista.findViewById<CheckBox>(R.id.cbEsReservable).isChecked,
                getString(R.string.zona_error_registrar)
            )
            dialogo.dismiss()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
