package com.example.conjuntoresidencial.ui.obras

import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.DialogPersonalBinding
import com.example.conjuntoresidencial.databinding.FragmentObraDetalleBinding
import com.example.conjuntoresidencial.databinding.ItemPersonalFilaBinding
import com.example.conjuntoresidencial.model.EstadoRequest
import com.example.conjuntoresidencial.model.ObraDTO
import com.example.conjuntoresidencial.model.PersonalObraDTO
import com.example.conjuntoresidencial.model.RevisionObra
import com.example.conjuntoresidencial.model.RevisionPersonal
import com.example.conjuntoresidencial.ui.admin.editarPersonal
import com.example.conjuntoresidencial.ui.comun.Estados
import com.example.conjuntoresidencial.ui.comun.accionesDeLista
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.confirmar
import com.example.conjuntoresidencial.ui.comun.ejecutarAccion
import com.example.conjuntoresidencial.ui.comun.elegirOpcion
import com.example.conjuntoresidencial.ui.comun.mostrarEstado
import com.example.conjuntoresidencial.ui.comun.pedirTexto
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.ui.comun.ponerTitulo
import com.example.conjuntoresidencial.ui.propietario.RegistrarObraFragment
import com.example.conjuntoresidencial.util.Formatos
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Detalle de una obra en las dos apps (P5). El propietario la edita toda mientras no esté aprobada;
 * ya aprobada solo cambia su estado y su personal (los trabajadores nuevos esperan aprobación).
 * La administración la aprueba o rechaza, revisa la ARL de cada trabajador y la puede editar o borrar.
 */
class ObraDetalleFragment : Fragment() {

    private var _binding: FragmentObraDetalleBinding? = null
    private val binding get() = _binding!!
    private val obraId get() = requireArguments().getLong(ARG_OBRA)
    private val esAdmin get() = resources.getBoolean(R.bool.app_admin)
    private var obra: ObraDTO? = null
    private var personal: List<PersonalObraDTO> = emptyList()

    // Trabajador al que se le sube el certificado; sobrevive a que el sistema cierre la app al elegir el archivo
    private var trabajadorPorSubir: Long?
        get() = arguments?.getLong(ARG_TRABAJADOR)?.takeIf { it > 0 }
        set(valor) { arguments = (arguments ?: Bundle()).apply { putLong(ARG_TRABAJADOR, valor ?: 0) } }

    // Diálogo de trabajador nuevo que espera el certificado elegido
    private var alElegirCertificado: ((CertificadoArl) -> Unit)? = null

    private val elegirArchivo = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val enDialogo = alElegirCertificado
        val trabajador = trabajadorPorSubir
        alElegirCertificado = null
        trabajadorPorSubir = null
        val certificado = uri?.let { leerCertificado(it) } ?: return@registerForActivityResult
        when {
            enDialogo != null -> enDialogo(certificado)
            trabajador != null -> ejecutarAccion(getString(R.string.arl_subido),
                { RetrofitClient.instance.subirCertificadoArl(trabajador, certificado.parte()) }) { cargar() }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentObraDetalleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(
            binding.encabezado, R.string.obras_titulo, R.string.obra_detalle_subtitulo, menu = R.menu.menu_lista,
            alElegir = accionesDeLista(binding.swipe) { cargar() }
        )
        binding.estado.vincular(binding.contenido)
        binding.swipe.setOnRefreshListener { cargar() }
        binding.btnAnadirTrabajador.setOnClickListener { nuevoTrabajador() }
        binding.btnEditarObra.setOnClickListener {
            findNavController().navigate(R.id.registrarObraFragment, bundleOf(RegistrarObraFragment.ARG_OBRA to obraId))
        }
        binding.btnEstadoObra.setOnClickListener { cambiarEstado() }
        binding.btnAprobarObra.setOnClickListener { aprobarObra() }
        binding.btnRechazarObra.setOnClickListener { rechazarObra() }
        binding.btnEliminarObra.setOnClickListener { eliminarObra() }
        // También al volver del editor: la vista se crea otra vez
        cargar()
    }

    /** Obra y su personal en paralelo; si falla la obra, pantalla de error. */
    private fun cargar() {
        if (obra == null) binding.estado.mostrarCargando()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val (o, p) = coroutineScope {
                    val a = async { RetrofitClient.instance.getObra(obraId) }
                    val b = async { RetrofitClient.instance.getPersonalPorObra(obraId) }
                    a.await() to b.await()
                }
                binding.swipe.isRefreshing = false
                val datos = o.body()
                if (!o.isSuccessful || datos == null) {
                    binding.estado.mostrarError(RetrofitClient.mensajeDeError(o, getString(R.string.obra_error_cargar))) { cargar() }
                    return@launch
                }
                obra = datos
                personal = if (p.isSuccessful) p.body().orEmpty() else emptyList()
                mostrar(datos)
                binding.estado.mostrarContenido()
            } catch (e: Exception) {
                binding.swipe.isRefreshing = false
                binding.estado.mostrarError(RetrofitClient.mensajeDeExcepcion(e)) { cargar() }
            }
        }
    }

    private fun mostrar(o: ObraDTO) {
        binding.encabezado.ponerTitulo(
            if (o.torre == null) getString(R.string.obra_del_conjunto) else getString(R.string.apto_item_titulo, o.torre, o.apto)
        )
        binding.tvDescripcion.text = o.descripcionAdecuacion
        binding.tvFechas.text = Formatos.rango(o.fechaInicio, o.fechaFinEstimada)
        binding.tvEstado.mostrarEstado(Estados.obra(o.estadoObra))
        binding.tvAprobacion.mostrarEstado(Estados.aprobacion(o.aprobacion))
        val rechazada = o.aprobacion == RECHAZADA
        binding.tvMotivo.isVisible = rechazada && !o.motivoRechazo.isNullOrBlank()
        binding.tvMotivo.text = o.motivoRechazo?.let { getString(R.string.obra_motivo, it) }
        val aprobada = aprobada(o)
        binding.tvAyuda.setText(when {
            esAdmin -> R.string.obra_ayuda_admin
            aprobada -> R.string.obra_ayuda_aprobada
            rechazada -> R.string.obra_ayuda_rechazada
            else -> R.string.obra_ayuda_pendiente
        })
        binding.btnAprobarObra.isVisible = esAdmin && !aprobada
        binding.btnRechazarObra.isVisible = esAdmin && o.aprobacion == PENDIENTE
        // Antes de aprobarla el propietario cambia el estado desde el editor, con lo demás
        binding.btnEstadoObra.isVisible = esAdmin || aprobada
        binding.btnEditarObra.isVisible = esAdmin || !aprobada
        binding.btnEliminarObra.isVisible = esAdmin || !aprobada
        mostrarPersonal()
    }

    /** Una fila por trabajador: nombre, cédula, si tiene ARL y su aprobación; al tocarla, sus opciones. */
    private fun mostrarPersonal() {
        binding.contenedorPersonal.removeAllViews()
        val fondo = TypedValue().also {
            requireContext().theme.resolveAttribute(android.R.attr.selectableItemBackground, it, true)
        }.resourceId
        personal.forEach { p ->
            val fila = ItemPersonalFilaBinding.inflate(layoutInflater, binding.contenedorPersonal, true)
            fila.tvNombreFila.text = p.nombreTrabajador
            fila.tvCedulaFila.text = listOfNotNull(
                getString(R.string.personal_cedula, p.cedula),
                getString(if (p.tieneCertificado == true) R.string.arl_adjunto else R.string.arl_sin_adjunto),
                p.motivoRechazo?.takeIf { p.aprobacion == RECHAZADO }?.let { getString(R.string.obra_motivo, it) }
            ).joinToString("\n")
            fila.tvArlFila.mostrarEstado(Estados.aprobacion(p.aprobacion))
            fila.root.setBackgroundResource(fondo)
            fila.root.isClickable = true
            fila.root.isFocusable = true
            fila.root.setOnClickListener { opcionesTrabajador(p) }
        }
        binding.tvSinPersonal.isVisible = personal.isEmpty()
        binding.encabezado.ponerInfo(
            resources.getQuantityString(R.plurals.obra_resumen_personal, personal.size, personal.size)
        )
    }

    private fun opcionesTrabajador(p: PersonalObraDTO) {
        val o = obra ?: return
        val id = p.id ?: return
        val tieneCertificado = p.tieneCertificado == true
        val opciones = listOfNotNull<Pair<Int, () -> Unit>>(
            (R.string.arl_ver to { abrirCertificado(id) }).takeIf { tieneCertificado },
            (if (tieneCertificado) R.string.arl_cambiar else R.string.arl_subir) to {
                trabajadorPorSubir = id
                elegirArchivo.launch(CertificadoArl.TIPOS)
            },
            (R.string.personal_aprobar to { revisarTrabajador(p, true) }).takeIf { esAdmin && p.aprobacion != APROBADO },
            (R.string.personal_rechazar to { revisarTrabajador(p, false) }).takeIf { esAdmin && p.aprobacion == PENDIENTE },
            // Ya aprobada la obra, el propietario solo añade o quita personal
            (R.string.editar to { editarPersonal(p) { cargar() } }).takeIf { esAdmin || !aprobada(o) },
            R.string.personal_eliminar to {
                confirmar(getString(R.string.personal_eliminar_titulo, p.nombreTrabajador), null, R.string.residente_eliminar_si) {
                    ejecutarAccion(getString(R.string.personal_eliminado), { RetrofitClient.instance.deletePersonalObra(id) }) { cargar() }
                }
            }
        )
        elegirOpcion(p.nombreTrabajador, opciones)
    }

    private fun revisarTrabajador(p: PersonalObraDTO, aprobado: Boolean) {
        if (aprobado) {
            ejecutarAccion(getString(R.string.personal_aprobado_ok),
                { RetrofitClient.instance.revisarPersonal(p.id!!, RevisionPersonal(true)) }) { cargar() }
            return
        }
        pedirMotivo(getString(R.string.personal_rechazar)) { motivo ->
            ejecutarAccion(getString(R.string.personal_rechazado_ok),
                { RetrofitClient.instance.revisarPersonal(p.id!!, RevisionPersonal(false, motivo)) }) { cargar() }
        }
    }

    private fun aprobarObra() {
        val mensaje = getString(R.string.obra_aprobar_mensaje).takeIf { personal.any { it.aprobacion == PENDIENTE } }
        confirmar(getString(R.string.obra_aprobar_titulo), mensaje, R.string.obra_aprobar) {
            ejecutarAccion(getString(R.string.obra_aprobada_ok),
                { RetrofitClient.instance.revisarObra(obraId, RevisionObra(true)) }) { cargar() }
        }
    }

    private fun rechazarObra() {
        pedirMotivo(getString(R.string.obra_rechazar)) { motivo ->
            ejecutarAccion(getString(R.string.obra_rechazada_ok),
                { RetrofitClient.instance.revisarObra(obraId, RevisionObra(false, motivo)) }) { cargar() }
        }
    }

    private fun pedirMotivo(titulo: String, alAceptar: (String) -> Unit) {
        pedirTexto(titulo, R.string.obra_motivo_hint, null,
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_MULTI_LINE,
            validar = { if (it.isBlank()) getString(R.string.obra_motivo_obligatorio) else null }, alAceptar = alAceptar)
    }

    private fun cambiarEstado() {
        val o = obra ?: return
        val elegir = { estado: String ->
            ejecutarAccion(getString(R.string.obra_estado_cambiado), {
                // La administración usa su ruta de estado; el propietario la de editar (con la obra aprobada solo cambia esto)
                if (esAdmin) RetrofitClient.instance.cambiarEstadoObra(obraId, EstadoRequest(estado))
                else RetrofitClient.instance.updateObra(obraId, o.copy(estadoObra = estado))
            }) { cargar() }
        }
        elegirOpcion(getString(R.string.obra_cambiar_estado), listOf(
            R.string.estado_en_proceso to { elegir("EN_PROCESO") },
            R.string.estado_finalizada to { elegir("FINALIZADA") }
        ))
    }

    private fun eliminarObra() {
        val o = obra ?: return
        confirmar(getString(R.string.obra_eliminar_titulo), getString(R.string.obra_eliminar_mensaje, o.descripcionAdecuacion),
            R.string.residente_eliminar_si) {
            ejecutarAccion(getString(R.string.obra_eliminada), { RetrofitClient.instance.deleteObra(obraId) }) {
                findNavController().popBackStack()
            }
        }
    }

    /** Nombre, cédula y certificado ARL; el del propietario queda pendiente de aprobación. */
    private fun nuevoTrabajador() {
        val vista = DialogPersonalBinding.inflate(layoutInflater)
        vista.switchArl.isVisible = false
        vista.btnArlPersonal.isVisible = true
        var certificado: CertificadoArl? = null
        vista.btnArlPersonal.setOnClickListener {
            alElegirCertificado = { c ->
                certificado = c
                vista.btnArlPersonal.setText(R.string.arl_elegido)
                vista.btnArlPersonal.setIconResource(R.drawable.ic_check_circle)
            }
            elegirArchivo.launch(CertificadoArl.TIPOS)
        }
        val dialogo = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.personal_nuevo)
            .setView(vista.root)
            .setPositiveButton(R.string.btn_guardar, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val nombre = vista.etNombrePersonal.text?.toString()?.trim().orEmpty()
            val cedula = vista.etCedulaPersonal.text?.toString()?.trim().orEmpty()
            vista.tilNombrePersonal.error = if (nombre.isBlank()) getString(R.string.personal_nombre_obligatorio) else null
            vista.tilCedulaPersonal.error = if (cedula.isBlank()) getString(R.string.personal_cedula_obligatoria) else null
            if (nombre.isBlank() || cedula.isBlank()) return@setOnClickListener
            dialogo.dismiss()
            guardarTrabajador(PersonalObraDTO(null, nombre, cedula, certificado != null, obraId), certificado)
        }
    }

    private fun guardarTrabajador(dto: PersonalObraDTO, certificado: CertificadoArl?) {
        viewLifecycleOwner.lifecycleScope.launch {
            val mensaje = try {
                val r = RetrofitClient.instance.createPersonalObra(dto)
                val creado = r.body()
                val id = creado?.id
                when {
                    !r.isSuccessful || id == null ->
                        RetrofitClient.mensajeDeError(r, getString(R.string.personal_error_guardar))
                    certificado != null &&
                        !RetrofitClient.instance.subirCertificadoArl(id, certificado.parte()).isSuccessful ->
                        getString(R.string.arl_error_subir)
                    creado.aprobacion == PENDIENTE -> getString(R.string.personal_agregado_pendiente)
                    else -> getString(R.string.personal_agregado)
                }
            } catch (e: Exception) {
                RetrofitClient.mensajeDeExcepcion(e)
            }
            aviso(mensaje).show()
            cargar()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    companion object {
        const val ARG_OBRA = "obraId"
        private const val ARG_TRABAJADOR = "trabajadorPorSubir"
        const val PENDIENTE = "PENDIENTE"
        const val APROBADO = "APROBADO"
        const val RECHAZADA = "RECHAZADA"
        const val RECHAZADO = "RECHAZADO"

        /** Las de antes de P5 no traen aprobación: estaban aprobadas. */
        fun aprobada(o: ObraDTO) = o.aprobacion == null || o.aprobacion == "APROBADA"
    }
}
