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
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.pedirVehiculo
import com.example.conjuntoresidencial.util.Resource
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

        // Por categoría: cada tipo con lo que tiene o "No asignado" (P5)
        val adaptador = VehiculosPorTipoAdapter(onEliminar = { confirmarEliminar(it) }, onEditar = { mostrarDialogEditar(it) })
        fun pintar(lista: List<VehiculoDTO>) =
            adaptador.submitList(FilaVehiculo.de(viewModel.cupos ?: Opciones.CUPOS_POR_DEFECTO, lista))
        viewModel.cuposCargados.observe(viewLifecycleOwner) {
            (viewModel.vehiculos.value as? Resource.Success)?.data?.let { lista -> pintar(lista) }
        }
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
                pintar(lista)
                binding.encabezado.ponerInfo(resumen(
                    R.plurals.resumen_vehiculos, lista.size,
                    R.plurals.resumen_por_aprobar to lista.count { it.estado == "PENDIENTE" }
                ))
            }
        }

        viewModel.mensaje.observe(viewLifecycleOwner) { evento ->
            evento.obtenerSiNoManejado()?.let {
                aviso(it, Snackbar.LENGTH_LONG).show()
            }
        }

        // Botón flotante (encima del difuminado); se encoge a solo ícono al desplazar hacia abajo
        controlesFlotantes?.mostrarBotonFlotante(R.string.vehiculos_agregar, R.drawable.ic_add) { mostrarDialogAgregar() }?.let { boton ->
            binding.rvVehiculos.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                    if (dy > 0) boton.shrink() else if (dy < 0) boton.extend()
                }
            })
        }

        if (viewModel.vehiculos.value == null) viewModel.fetchVehiculos(aptoId)
        if (viewModel.cupos == null) viewModel.cargarCupos(aptoId)
    }

    /** Tipos con lo que le queda de cupo al apartamento; queda por aprobar del administrador. */
    private fun mostrarDialogAgregar() {
        val actuales = (viewModel.vehiculos.value as? Resource.Success)?.data.orEmpty()
        pedirVehiculo(viewModel.cupos, actuales, limitar = true) { nuevo ->
            viewModel.agregarVehiculo(
                VehiculoDTO(null, nuevo.placa, nuevo.marca, nuevo.color, nuevo.tipo, aptoId),
                getString(R.string.vehiculo_enviado), getString(R.string.vehiculo_error_guardar)
            )
        }
    }

    /** Lo editado vuelve a esperar la aprobación de la administración. */
    private fun mostrarDialogEditar(vehiculo: VehiculoDTO) {
        val actuales = (viewModel.vehiculos.value as? Resource.Success)?.data.orEmpty()
        pedirVehiculo(viewModel.cupos, actuales, limitar = true, editando = vehiculo) { nuevo ->
            viewModel.editarVehiculo(
                vehiculo.copy(placa = nuevo.placa, marca = nuevo.marca, color = nuevo.color, tipo = nuevo.tipo),
                getString(R.string.vehiculo_editado), getString(R.string.vehiculo_error_guardar)
            )
        }
    }

    private fun confirmarEliminar(vehiculo: VehiculoDTO) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.vehiculo_eliminar_titulo)
            .setMessage(getString(R.string.vehiculo_eliminar_pregunta, vehiculo.placa ?: vehiculo.tipo.lowercase()))
            .setPositiveButton(R.string.vehiculo_eliminar) { _, _ ->
                viewModel.eliminarVehiculo(vehiculo, aptoId, getString(R.string.vehiculo_error_eliminar))
            }
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
