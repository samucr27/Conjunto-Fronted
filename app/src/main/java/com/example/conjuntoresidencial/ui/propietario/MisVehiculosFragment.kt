package com.example.conjuntoresidencial.ui.propietario

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FragmentMisVehiculosBinding
import com.example.conjuntoresidencial.model.VehiculoDTO
import com.example.conjuntoresidencial.ui.comun.mostrarRecurso
import com.example.conjuntoresidencial.util.Opciones
import com.example.conjuntoresidencial.util.SessionManager
import com.example.conjuntoresidencial.util.comoLista
import com.example.conjuntoresidencial.ui.comun.accionesDeLista
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.controlesFlotantes
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.ui.comun.resumen
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class MisVehiculosFragment : Fragment() {
    private var _binding: FragmentMisVehiculosBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MisVehiculosViewModel by viewModels()
    private var aptoId: Long = -1L

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMisVehiculosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(
            binding.encabezado, R.string.vehiculos_titulo, R.string.vehiculos_subtitulo, menu = R.menu.menu_lista,
            alElegir = accionesDeLista(binding.swipe) { viewModel.fetchVehiculos(aptoId) }
        )
        aptoId = SessionManager(requireContext()).getApartamentoId()

        val adaptador = VehiculoAdapter { vehiculo -> confirmarEliminar(vehiculo) }
        binding.rvVehiculos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvVehiculos.adapter = adaptador
        binding.estado.vincular(binding.rvVehiculos)
        binding.swipe.setOnRefreshListener { viewModel.fetchVehiculos(aptoId) }

        viewModel.vehiculos.observe(viewLifecycleOwner) { resource ->
            binding.estado.mostrarRecurso(
                resource, binding.swipe, R.drawable.ic_car, getString(R.string.vehiculos_vacio),
                getString(R.string.vehiculos_vacio_mensaje),
                reintentar = { viewModel.fetchVehiculos(aptoId) }
            ) { lista ->
                adaptador.submitList(lista)
                binding.encabezado.ponerInfo(resumen(R.plurals.resumen_vehiculos, lista.size))
            }
        }

        viewModel.mensaje.observe(viewLifecycleOwner) { evento ->
            evento.obtenerSiNoManejado()?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }

        // El botón flotante se encoge a solo ícono al desplazar la lista hacia abajo
        binding.rvVehiculos.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) binding.btnAgregarVehiculo.shrink() else if (dy < 0) binding.btnAgregarVehiculo.extend()
            }
        })
        binding.btnAgregarVehiculo.setOnClickListener { mostrarDialogAgregar() }
        // El difuminado de abajo termina encima del botón flotante
        controlesFlotantes?.reservarAbajo(binding.btnAgregarVehiculo)

        if (viewModel.vehiculos.value == null) viewModel.fetchVehiculos(aptoId)
    }

    private fun mostrarDialogAgregar() {
        val vista = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_agregar_vehiculo, null)
        val tipoView = vista.findViewById<AutoCompleteTextView>(R.id.etTipo)
        tipoView.comoLista(Opciones.TIPOS_VEHICULO)
        val tilPlaca = vista.findViewById<TextInputLayout>(R.id.tilPlaca)
        val dialogo = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.vehiculo_nuevo)
            .setView(vista)
            .setPositiveButton(R.string.btn_guardar, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()

        // Se asigna después de show() para que el diálogo no se cierre si falta la placa
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val placa = vista.findViewById<TextInputEditText>(R.id.etPlaca).text.toString().trim()
            if (placa.isBlank()) {
                tilPlaca.error = getString(R.string.vehiculo_placa_obligatoria)
                return@setOnClickListener
            }
            val marca = vista.findViewById<TextInputEditText>(R.id.etMarca).text.toString().trim()
            val color = vista.findViewById<TextInputEditText>(R.id.etColor).text.toString().trim()
            viewModel.agregarVehiculo(
                VehiculoDTO(null, placa, marca, color, tipoView.text.toString(), aptoId),
                getString(R.string.vehiculo_error_guardar)
            )
            dialogo.dismiss()
        }
    }

    private fun confirmarEliminar(vehiculo: VehiculoDTO) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.vehiculo_eliminar_titulo)
            .setMessage(getString(R.string.vehiculo_eliminar_pregunta, vehiculo.placa))
            .setPositiveButton(R.string.vehiculo_eliminar) { _, _ ->
                viewModel.eliminarVehiculo(vehiculo, aptoId, getString(R.string.vehiculo_error_eliminar))
            }
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
