package com.example.conjuntoresidencial.ui.admin

import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.model.Apartamento
import com.example.conjuntoresidencial.model.ApartamentoRequest
import com.example.conjuntoresidencial.ui.comun.confirmar
import com.example.conjuntoresidencial.ui.comun.ejecutarAccion
import com.example.conjuntoresidencial.ui.comun.elegirOpcion
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.FragmentDetalleApartamentoBinding
import com.example.conjuntoresidencial.model.AccesoResidente
import com.example.conjuntoresidencial.model.CambioContrasenaRequest
import com.example.conjuntoresidencial.model.ResidenteDTO
import com.example.conjuntoresidencial.model.VehiculoDTO
import com.example.conjuntoresidencial.util.Opciones
import com.example.conjuntoresidencial.util.comoLista
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.ui.comun.ponerTitulo
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.pedirVehiculo
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class DetalleApartamentoFragment : Fragment() {

    private var _binding: FragmentDetalleApartamentoBinding? = null
    private val binding get() = _binding!!
    private var aptoId: Long = -1L
    private val residentesAdapter = ResidenteAdapter(onTocar = { opcionesResidente(it) })
    private val vehiculosAdapter = VehiculoAdminAdapter { v ->
        elegirOpcion(v.placa ?: v.tipo, listOf(
            R.string.editar to { mostrarDialogVehiculo(v) },
            R.string.vehiculo_eliminar to {
                confirmar(getString(R.string.vehiculo_eliminar_titulo),
                    getString(R.string.vehiculo_eliminar_pregunta, v.placa ?: v.tipo.lowercase()), R.string.vehiculo_eliminar) {
                    ejecutarAccion(getString(R.string.vehiculo_eliminado), { RetrofitClient.instance.deleteVehiculo(v.id!!) }) {
                        recargarVehiculos()
                    }
                }
            }
        ))
    }
    private var apartamento: Apartamento? = null

    private var totalResidentes = 0
    private var totalVehiculos = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDetalleApartamentoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        aptoId = arguments?.getLong("aptoId") ?: -1L
        configurarEncabezado(binding.encabezado, R.string.detalle_titulo, R.string.detalle_subtitulo,
            menu = R.menu.menu_detalle_apartamento) { item ->
            when (item) {
                R.id.accion_editar_apartamento -> { apartamento?.let { editarApartamento(it) }; true }
                R.id.accion_eliminar_apartamento -> { apartamento?.let { eliminarApartamento(it) }; true }
                else -> false
            }
        }

        binding.rvResidentesApto.layoutManager = LinearLayoutManager(requireContext())
        binding.rvVehiculosApto.layoutManager = LinearLayoutManager(requireContext())
        binding.rvResidentesApto.adapter = residentesAdapter
        binding.rvVehiculosApto.adapter = vehiculosAdapter
        binding.estado.vincular(binding.contenido)

        binding.btnAgregarResidente.setOnClickListener { mostrarDialogResidente() }
        binding.btnAgregarVehiculoAdmin.setOnClickListener { mostrarDialogVehiculo() }
        binding.btnRestablecerContrasena.setOnClickListener {
            pedirContrasena(getString(R.string.titulo_restablecer_contrasena), getString(R.string.contrasena_restablecida),
                getString(R.string.error_restablecer_contrasena)) { nueva ->
                RetrofitClient.instance.restablecerContrasena(aptoId, CambioContrasenaRequest(nueva))
            }
        }

        cargarTodo()
    }

    // Todas las peticiones usan viewLifecycleOwner.lifecycleScope: se cancelan si el usuario
    // sale de la pantalla, en vez de tocar una vista que ya no existe.

    /** Apartamento, residentes y vehículos en paralelo; si falla el apartamento, pantalla de error. */
    private fun cargarTodo() {
        binding.estado.mostrarCargando()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // coroutineScope: si una petición falla, el error llega al catch en vez de cerrar la app
                val (apto, residentes, vehiculos) = coroutineScope {
                    val a = async { RetrofitClient.instance.getApartamentoById(aptoId) }
                    val r = async { RetrofitClient.instance.getResidentesPorApartamento(aptoId) }
                    val v = async { RetrofitClient.instance.getVehiculosPorApartamento(aptoId) }
                    Triple(a.await(), r.await(), v.await())
                }
                val datos = apto.body()
                if (!apto.isSuccessful || datos == null) {
                    binding.estado.mostrarError(
                        RetrofitClient.mensajeDeError(apto, getString(R.string.detalle_error_cargar))
                    ) { cargarTodo() }
                    return@launch
                }
                apartamento = datos
                binding.encabezado.ponerTitulo(getString(R.string.apto_item_titulo, datos.torre, datos.apto))
                mostrarResidentes(if (residentes.isSuccessful) residentes.body().orEmpty() else emptyList())
                mostrarVehiculos(if (vehiculos.isSuccessful) vehiculos.body().orEmpty() else emptyList())
                binding.estado.mostrarContenido()
            } catch (e: Exception) {
                binding.estado.mostrarError(RetrofitClient.mensajeDeExcepcion(e)) { cargarTodo() }
            }
        }
    }

    private fun mostrarResidentes(lista: List<ResidenteDTO>) {
        residentesAdapter.submitList(lista)
        binding.tvSinResidentes.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
        totalResidentes = lista.size
        ponerResumen()
    }

    private fun mostrarVehiculos(lista: List<VehiculoDTO>) {
        vehiculosAdapter.submitList(lista)
        binding.tvSinVehiculos.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
        totalVehiculos = lista.size
        ponerResumen()
    }

    /** Resumen del encabezado: "2 residentes · 2 vehículos". */
    private fun ponerResumen() {
        binding.encabezado.ponerInfo(listOf(
            resources.getQuantityString(R.plurals.resumen_residentes, totalResidentes, totalResidentes),
            resources.getQuantityString(R.plurals.resumen_vehiculos, totalVehiculos, totalVehiculos)
        ).joinToString(" · "))
    }

    private fun recargarResidentes() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getResidentesPorApartamento(aptoId)
                if (response.isSuccessful) mostrarResidentes(response.body().orEmpty())
                else avisar(RetrofitClient.mensajeDeError(response, getString(R.string.estado_error_titulo)))
            } catch (e: Exception) {
                avisar(RetrofitClient.mensajeDeExcepcion(e))
            }
        }
    }

    private fun recargarVehiculos() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getVehiculosPorApartamento(aptoId)
                if (response.isSuccessful) mostrarVehiculos(response.body().orEmpty())
                else avisar(RetrofitClient.mensajeDeError(response, getString(R.string.estado_error_titulo)))
            } catch (e: Exception) {
                avisar(RetrofitClient.mensajeDeExcepcion(e))
            }
        }
    }

    /** Tocar un residente: darle acceso con su documento (o cambiarlo), quitárselo o eliminarlo. */
    private fun opcionesResidente(r: ResidenteDTO) {
        val conAcceso = r.tieneAcceso == true
        val opciones = listOfNotNull(
            R.string.residente_dar_acceso.takeIf { !conAcceso },
            R.string.residente_cambiar_contrasena.takeIf { conAcceso },
            R.string.residente_quitar_acceso.takeIf { conAcceso },
            R.string.residente_editar,
            R.string.residente_eliminar
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(r.nombreCompleto)
            .setItems(opciones.map { getString(it) }.toTypedArray()) { _, i ->
                when (opciones[i]) {
                    R.string.residente_dar_acceso, R.string.residente_cambiar_contrasena ->
                        pedirContrasena(getString(R.string.residente_acceso_titulo, r.nombreCompleto),
                            getString(R.string.residente_acceso_listo), getString(R.string.residente_error_accion)) { nueva ->
                            RetrofitClient.instance.cambiarAccesoResidente(r.id!!, AccesoResidente(nueva))
                        }
                    R.string.residente_editar -> mostrarDialogResidente(r)
                    R.string.residente_quitar_acceso -> accionResidente {
                        RetrofitClient.instance.cambiarAccesoResidente(r.id!!, AccesoResidente(""))
                    }
                    else -> MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.residente_eliminar_titulo, r.nombreCompleto))
                        .setPositiveButton(R.string.residente_eliminar_si) { _, _ ->
                            accionResidente { RetrofitClient.instance.deleteResidente(r.id!!) }
                        }
                        .setNegativeButton(R.string.btn_cancelar, null)
                        .show()
                }
            }
            .show()
    }

    private fun accionResidente(peticion: suspend () -> retrofit2.Response<*>) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = peticion()
                if (response.isSuccessful) recargarResidentes()
                else avisar(RetrofitClient.mensajeDeError(response, getString(R.string.residente_error_accion)))
            } catch (_: Exception) {
                avisar(getString(R.string.error_red))
            }
        }
    }

    /** Pide una contraseña nueva (mínimo 6) y la guarda con [guardar]; al terminar recarga los residentes. */
    private fun pedirContrasena(titulo: String, exito: String, error: String,
                                guardar: suspend (String) -> retrofit2.Response<*>) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_restablecer_contrasena, null)
        val til = dialogView.findViewById<TextInputLayout>(R.id.tilNuevaContrasena)
        val et = dialogView.findViewById<TextInputEditText>(R.id.etNuevaContrasena)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(titulo)
            .setView(dialogView)
            .setPositiveButton(R.string.btn_guardar, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()

        // Se asigna después de show() para que el diálogo no se cierre si la validación falla
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { boton ->
            val nueva = et.text?.toString().orEmpty()
            if (nueva.length < 6) {
                til.error = getString(R.string.error_contrasena_corta)
                return@setOnClickListener
            }
            til.error = null
            boton.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                val mensaje = try {
                    val response = guardar(nueva)
                    if (response.isSuccessful) exito.also { recargarResidentes() }
                    else RetrofitClient.mensajeDeError(response, error)
                } catch (_: Exception) {
                    getString(R.string.error_red)
                }
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show()
                dialog.dismiss()
            }
        }
    }

    private fun mostrarDialogResidente(editando: ResidenteDTO? = null) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_agregar_residente, null)
        val tipoView = dialogView.findViewById<AutoCompleteTextView>(R.id.etTipoResidente)
        tipoView.comoLista(Opciones.TIPOS_HABITANTE)
        val tilNombre = dialogView.findViewById<TextInputLayout>(R.id.tilNombreResidente)
        editando?.let {
            dialogView.findViewById<TextInputEditText>(R.id.etNombreResidente).setText(it.nombreCompleto)
            dialogView.findViewById<TextInputEditText>(R.id.etTelefonoResidente).setText(it.telefono)
            dialogView.findViewById<TextInputEditText>(R.id.etIdentificacionResidente).setText(it.identificacion)
            tipoView.setText(it.tipoHabitante, false)
        }
        val dialogo = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (editando == null) R.string.residente_nuevo else R.string.residente_editar)
            .setView(dialogView)
            .setPositiveButton(R.string.btn_guardar, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()

        // Se asigna después de show() para que el diálogo no se cierre si falta el nombre
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val nombre = dialogView.findViewById<TextInputEditText>(R.id.etNombreResidente).text.toString().trim()
            if (nombre.isBlank()) {
                tilNombre.error = getString(R.string.residente_nombre_obligatorio)
                return@setOnClickListener
            }
            val telefono = dialogView.findViewById<TextInputEditText>(R.id.etTelefonoResidente).text.toString().trim()
            val identificacion = dialogView.findViewById<TextInputEditText>(R.id.etIdentificacionResidente).text.toString().trim()
            val tipo = tipoView.text.toString()
            dialogo.dismiss()
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val datos = ResidenteDTO(editando?.id, nombre, telefono, identificacion, tipo, aptoId)
                    val response = if (editando == null) RetrofitClient.instance.createResidente(datos)
                        else RetrofitClient.instance.updateResidente(editando.id!!, datos)
                    if (response.isSuccessful) recargarResidentes()
                    else avisar(RetrofitClient.mensajeDeError(response, getString(R.string.residente_error_guardar)))
                } catch (_: Exception) {
                    avisar(getString(R.string.error_red))
                }
            }
        }
    }

    /** El administrador registra directo (aprobado) y puede pasar el cupo del apartamento. */
    /** Torre y número; la contraseña tiene su propio botón. */
    private fun editarApartamento(a: Apartamento) {
        pedirApartamento(a) { torre, apto, _ ->
            ejecutarAccion(getString(R.string.apartamento_editado),
                { RetrofitClient.instance.updateApartamento(a.id, ApartamentoRequest(torre, apto)) }) {
                DirectorioApartamentosFragment.huboCambios()
                binding.encabezado.ponerTitulo(getString(R.string.apto_item_titulo, torre, apto))
                apartamento = a.copy(torre = torre, apto = apto)
            }
        }
    }

    private fun eliminarApartamento(a: Apartamento) {
        confirmar(getString(R.string.apartamento_eliminar_titulo, a.torre, a.apto),
            getString(R.string.apartamento_eliminar_mensaje), R.string.residente_eliminar_si) {
            ejecutarAccion(getString(R.string.apartamento_eliminado), { RetrofitClient.instance.deleteApartamento(a.id) }) {
                DirectorioApartamentosFragment.huboCambios()
                findNavController().popBackStack()
            }
        }
    }

    private fun mostrarDialogVehiculo(editando: VehiculoDTO? = null) {
        viewLifecycleOwner.lifecycleScope.launch {
            val cupos = try {
                RetrofitClient.instance.getCuposVehiculo().body()
            } catch (_: Exception) {
                null // sin conexión se usan los cupos por defecto; el guardado dirá si falla
            }
            if (_binding == null) return@launch
            pedirVehiculo(cupos, vehiculosAdapter.currentList, limitar = false, editando = editando) { nuevo ->
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val datos = VehiculoDTO(editando?.id, nuevo.placa, nuevo.marca, nuevo.color, nuevo.tipo, aptoId)
                        val response = if (editando == null) RetrofitClient.instance.createVehiculo(datos)
                            else RetrofitClient.instance.updateVehiculo(editando.id!!, datos)
                        if (response.isSuccessful) recargarVehiculos()
                        else avisar(RetrofitClient.mensajeDeError(response, getString(R.string.vehiculo_error_guardar)))
                    } catch (_: Exception) {
                        avisar(getString(R.string.error_red))
                    }
                }
            }
        }
    }

    private fun avisar(mensaje: String) {
        _binding?.let { aviso(mensaje, Snackbar.LENGTH_LONG).show() }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
