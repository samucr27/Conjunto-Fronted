package com.example.conjuntoresidencial.ui.residentes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.FragmentResidentesBinding
import com.example.conjuntoresidencial.model.ResidenteDTO
import com.example.conjuntoresidencial.model.RevisionResidente
import com.example.conjuntoresidencial.ui.admin.ResidenteAdapter
import com.example.conjuntoresidencial.ui.comun.accionesDeLista
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.controlesFlotantes
import com.example.conjuntoresidencial.ui.comun.mostrarRecurso
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.ui.comun.resumen
import com.example.conjuntoresidencial.util.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * "Mi hogar": quiénes viven en el apartamento. El propietario agrega arrendatarios (quedan
 * pendientes hasta que los aprueba la administración) y los puede quitar; el arrendatario solo
 * ve la lista. Con [pendientes] es la pantalla del administrador: tocar uno lo aprueba o rechaza.
 */
class ResidentesFragment : Fragment() {
    private var _binding: FragmentResidentesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ResidentesViewModel by viewModels()

    private val esPendientes get() = arguments?.getBoolean(ARG_PENDIENTES) == true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentResidentesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sesion = SessionManager(requireContext())
        val pendientes = esPendientes
        configurarEncabezado(
            binding.encabezado,
            if (pendientes) R.string.residentes_pendientes_titulo else R.string.menu_mi_hogar,
            if (pendientes) R.string.residentes_pendientes_subtitulo else R.string.residentes_hogar_subtitulo,
            menu = R.menu.menu_lista,
            alElegir = accionesDeLista(binding.swipe) { viewModel.recargar() }
        )

        val puedeGestionar = !pendientes && sesion.esPropietario()
        val adaptador = ResidenteAdapter(
            conApartamento = pendientes,
            onTocar = when {
                pendientes -> { r -> revisar(r) }
                puedeGestionar -> { r -> if (r.tipoHabitante == ARRENDATARIO) confirmarQuitar(r) }
                else -> null
            }
        )
        binding.rvResidentes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvResidentes.adapter = adaptador
        binding.estado.vincular(binding.rvResidentes)
        binding.swipe.setOnRefreshListener { viewModel.recargar() }

        if (puedeGestionar) {
            controlesFlotantes?.mostrarBotonFlotante(R.string.residente_agregar_arrendatario, R.drawable.ic_add) {
                agregarArrendatario(sesion.getApartamentoId())
            }?.let { boton ->
                binding.rvResidentes.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                        if (dy > 0) boton.shrink() else if (dy < 0) boton.extend()
                    }
                })
            }
        }

        viewModel.residentes.observe(viewLifecycleOwner) { resource ->
            binding.estado.mostrarRecurso(
                resource, binding.swipe, R.drawable.ic_people,
                getString(if (pendientes) R.string.residentes_pendientes_vacio else R.string.residentes_vacio),
                if (pendientes) getString(R.string.residentes_pendientes_vacio_mensaje)
                else if (puedeGestionar) getString(R.string.residentes_vacio_mensaje) else null,
                reintentar = { viewModel.recargar() }
            ) { lista ->
                adaptador.submitList(lista)
                binding.encabezado.ponerInfo(resumen(R.plurals.resumen_residentes, lista.size))
            }
        }
        viewModel.mensaje.observe(viewLifecycleOwner) { evento ->
            evento.obtenerSiNoManejado()?.let { aviso(it, Snackbar.LENGTH_LONG).show() }
        }

        if (viewModel.residentes.value == null) viewModel.cargar(pendientes, sesion.getApartamentoId())
    }

    /** Mismo formulario del administrador, sin el tipo (siempre arrendatario) y con su contraseña. */
    private fun agregarArrendatario(aptoId: Long) {
        val vista = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_agregar_residente, null)
        vista.findViewById<View>(R.id.tilTipoResidente).visibility = View.GONE
        val tilContrasena = vista.findViewById<TextInputLayout>(R.id.tilContrasenaResidente).apply { visibility = View.VISIBLE }
        val tilNombre = vista.findViewById<TextInputLayout>(R.id.tilNombreResidente)
        fun texto(id: Int) = vista.findViewById<TextInputEditText>(id).text?.toString()?.trim().orEmpty()

        val dialogo = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.residente_agregar_arrendatario)
            .setView(vista)
            .setPositiveButton(R.string.btn_guardar, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
        // Después de show() para que no se cierre si falta un dato
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val nombre = texto(R.id.etNombreResidente)
            val documento = texto(R.id.etIdentificacionResidente)
            val contrasena = vista.findViewById<TextInputEditText>(R.id.etContrasenaResidente).text?.toString().orEmpty()
            tilNombre.error = if (nombre.isBlank()) getString(R.string.residente_nombre_obligatorio) else null
            vista.findViewById<TextInputEditText>(R.id.etIdentificacionResidente).error =
                if (documento.isBlank()) getString(R.string.residente_documento_obligatorio) else null
            tilContrasena.error = if (contrasena.length < 6) getString(R.string.error_contrasena_corta) else null
            if (nombre.isBlank() || documento.isBlank() || contrasena.length < 6) return@setOnClickListener
            dialogo.dismiss()
            val nuevo = ResidenteDTO(null, nombre, texto(R.id.etTelefonoResidente).ifBlank { null }, documento,
                ARRENDATARIO, aptoId, contrasena = contrasena)
            viewModel.ejecutar(getString(R.string.residente_agregado_pendiente), getString(R.string.residente_error_guardar)) {
                RetrofitClient.instance.createResidente(nuevo)
            }
        }
    }

    private fun confirmarQuitar(r: ResidenteDTO) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.residente_quitar_titulo, r.nombreCompleto))
            .setMessage(R.string.residente_quitar_mensaje)
            .setPositiveButton(R.string.residente_quitar) { _, _ ->
                viewModel.ejecutar(getString(R.string.residente_quitado), getString(R.string.residente_error_accion)) {
                    RetrofitClient.instance.deleteResidente(r.id!!)
                }
            }
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
    }

    /** Administrador: aprobar, o rechazar con un motivo que le llega al apartamento. */
    private fun revisar(r: ResidenteDTO) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(r.nombreCompleto)
            .setMessage(getString(R.string.residente_revisar_mensaje, getString(R.string.apto_item_titulo, r.torre, r.apto),
                r.identificacion, r.telefono?.takeIf { it.isNotBlank() } ?: getString(R.string.residente_sin_telefono)))
            .setPositiveButton(R.string.residente_aprobar) { _, _ ->
                viewModel.ejecutar(getString(R.string.residente_aprobado), getString(R.string.residente_error_accion)) {
                    RetrofitClient.instance.revisarResidente(r.id!!, RevisionResidente(APROBADO))
                }
            }
            .setNegativeButton(R.string.residente_rechazar) { _, _ -> pedirMotivo(r) }
            .setNeutralButton(R.string.btn_cancelar, null)
            .show()
    }

    private fun pedirMotivo(r: ResidenteDTO) {
        val campo = TextInputEditText(requireContext())
        val til = TextInputLayout(requireContext()).apply {
            hint = getString(R.string.residente_motivo)
            addView(campo)
            val margen = (24 * resources.displayMetrics.density).toInt()
            setPadding(margen, margen / 2, margen, 0)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.residente_rechazar_titulo, r.nombreCompleto))
            .setView(til)
            .setPositiveButton(R.string.residente_rechazar) { _, _ ->
                val motivo = campo.text?.toString()?.trim()?.ifBlank { null }
                viewModel.ejecutar(getString(R.string.residente_rechazado), getString(R.string.residente_error_accion)) {
                    RetrofitClient.instance.revisarResidente(r.id!!, RevisionResidente(RECHAZADO, motivo))
                }
            }
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    companion object {
        private const val ARG_PENDIENTES = "pendientes"
        private const val ARRENDATARIO = "ARRENDATARIO"
        private const val APROBADO = "APROBADO"
        private const val RECHAZADO = "RECHAZADO"

        fun pendientes() = bundleOf(ARG_PENDIENTES to true)
    }
}
