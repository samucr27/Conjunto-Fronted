package com.example.conjuntoresidencial.ui.residentes

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.FragmentMiHogarBinding
import com.example.conjuntoresidencial.model.CitofonosRequest
import com.example.conjuntoresidencial.model.ResidenteDTO
import com.example.conjuntoresidencial.model.VisitanteDTO
import com.example.conjuntoresidencial.ui.comun.accionesDeLista
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.confirmar
import com.example.conjuntoresidencial.ui.comun.ejecutarAccion
import com.example.conjuntoresidencial.ui.comun.elegirOpcion
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.util.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Mi hogar (P5): los dos teléfonos del citófono, las personas que viven en el apartamento y
 * los visitantes frecuentes. Lo gestiona quien vive ahí: el propietario o, si está arrendado,
 * el arrendatario.
 */
class MiHogarFragment : Fragment() {
    private var _binding: FragmentMiHogarBinding? = null
    private val binding get() = _binding!!
    private val sesion by lazy { SessionManager(requireContext()) }
    private val aptoId get() = sesion.getApartamentoId()

    private var citofono1: String? = null
    private var citofono2: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMiHogarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(binding.encabezado, R.string.menu_mi_hogar, R.string.residentes_hogar_subtitulo,
            menu = R.menu.menu_lista, alElegir = accionesDeLista(binding.swipe) { cargar() })
        binding.estado.vincular(binding.contenido)
        binding.swipe.setOnRefreshListener { cargar() }
        binding.btnEditarCitofonos.setOnClickListener { editarCitofonos() }
        binding.btnAgregarPersonaHogar.setOnClickListener { pedirPersona(null) }
        binding.btnAgregarVisitanteHogar.setOnClickListener { pedirVisitante(null) }
        cargar()
    }

    private fun cargar() {
        if (!binding.swipe.isRefreshing) binding.estado.mostrarCargando()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val (apto, residentes, visitantes) = coroutineScope {
                    val a = async { RetrofitClient.instance.getApartamentoById(aptoId) }
                    val r = async { RetrofitClient.instance.getResidentesPorApartamento(aptoId) }
                    val v = async { RetrofitClient.instance.getVisitantes(aptoId) }
                    Triple(a.await(), r.await(), v.await())
                }
                if (_binding == null) return@launch
                binding.swipe.isRefreshing = false
                if (!residentes.isSuccessful) {
                    binding.estado.mostrarError(RetrofitClient.mensajeDeError(residentes, getString(R.string.residentes_error_cargar))) { cargar() }
                    return@launch
                }
                citofono1 = apto.body()?.citofono1
                citofono2 = apto.body()?.citofono2
                binding.tvCitofono1.text = citofono1 ?: getString(R.string.hogar_sin_configurar)
                binding.tvCitofono2.text = citofono2 ?: getString(R.string.hogar_sin_configurar)
                // Las personas del hogar de quien vive aquí (las del propietario o las del arrendatario)
                val hogar = residentes.body().orEmpty().filter { it.tipoHabitante == sesion.getRol() && it.estado != "RECHAZADO" }
                pintarPersonas(hogar)
                pintarVisitantes(if (visitantes.isSuccessful) visitantes.body().orEmpty() else emptyList())
                binding.encabezado.ponerInfo(resources.getQuantityString(R.plurals.resumen_residentes, hogar.size, hogar.size))
                binding.estado.mostrarContenido()
            } catch (e: Exception) {
                if (_binding == null) return@launch
                binding.swipe.isRefreshing = false
                binding.estado.mostrarError(RetrofitClient.mensajeDeExcepcion(e)) { cargar() }
            }
        }
    }

    private fun pintarPersonas(lista: List<ResidenteDTO>) {
        pintar(binding.listaPersonasHogar, binding.tvSinPersonasHogar, lista.sortedByDescending { it.titular == true },
            textos = { p ->
                p.nombreCompleto to listOfNotNull(
                    if (p.titular == true) getString(R.string.hogar_titular) else null,
                    getString(R.string.registro_doc, p.identificacion).takeIf { !p.identificacion.startsWith("Sin") },
                    p.telefono
                ).joinToString(" · ")
            },
            // El titular se cambia en la administración; los demás, aquí
            alTocar = { p ->
                if (p.titular != true) elegirOpcion(p.nombreCompleto, listOf(
                    R.string.editar to { pedirPersona(p) },
                    R.string.residente_quitar to {
                        confirmar(getString(R.string.residente_quitar_titulo, p.nombreCompleto), null, R.string.residente_quitar) {
                            ejecutarAccion(getString(R.string.residente_quitado), { RetrofitClient.instance.deleteResidente(p.id!!) }) { cargar() }
                        }
                    }
                ))
            })
    }

    private fun pintarVisitantes(lista: List<VisitanteDTO>) {
        pintar(binding.listaVisitantesHogar, binding.tvSinVisitantesHogar, lista,
            textos = { v -> v.nombreCompleto to listOfNotNull(v.identificacion?.let { getString(R.string.registro_doc, it) }, v.telefono).joinToString(" · ") },
            alTocar = { v ->
                elegirOpcion(v.nombreCompleto, listOf(
                    R.string.editar to { pedirVisitante(v) },
                    R.string.residente_quitar to {
                        confirmar(getString(R.string.residente_quitar_titulo, v.nombreCompleto), null, R.string.residente_quitar) {
                            ejecutarAccion(getString(R.string.hogar_visitante_quitado), { RetrofitClient.instance.deleteVisitante(v.id!!) }) { cargar() }
                        }
                    }
                ))
            })
    }

    /** Una fila de dos líneas por elemento ([textos]); tocarla llama a [alTocar]. */
    private fun <T> pintar(contenedor: LinearLayout, vacio: TextView, lista: List<T>,
                           textos: (T) -> Pair<String, String>, alTocar: (T) -> Unit) {
        contenedor.removeAllViews()
        vacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
        lista.forEach { item ->
            val fila = layoutInflater.inflate(android.R.layout.simple_list_item_2, contenedor, false)
            val (linea1, linea2) = textos(item)
            fila.findViewById<TextView>(android.R.id.text1).text = linea1
            fila.findViewById<TextView>(android.R.id.text2).text = linea2
            fila.setOnClickListener { alTocar(item) }
            contenedor.addView(fila)
        }
    }

    private fun pedirPersona(actual: ResidenteDTO?) {
        pedirDatos(if (actual == null) R.string.registro_agregar_persona else R.string.residente_editar,
            actual?.nombreCompleto, actual?.identificacion?.takeIf { !it.startsWith("Sin") }, actual?.telefono) { nombre, doc, tel ->
            val datos = ResidenteDTO(actual?.id, nombre, tel, doc ?: "Sin documento", sesion.getRol() ?: "PROPIETARIO", aptoId)
            ejecutarAccion(getString(R.string.hogar_guardado), {
                if (actual == null) RetrofitClient.instance.createResidente(datos)
                else RetrofitClient.instance.updateResidente(actual.id!!, datos)
            }) { cargar() }
        }
    }

    private fun pedirVisitante(actual: VisitanteDTO?) {
        pedirDatos(if (actual == null) R.string.registro_agregar_visitante else R.string.hogar_editar_visitante,
            actual?.nombreCompleto, actual?.identificacion, actual?.telefono) { nombre, doc, tel ->
            val datos = VisitanteDTO(actual?.id, nombre, doc, tel, aptoId)
            ejecutarAccion(getString(R.string.hogar_guardado), {
                if (actual == null) RetrofitClient.instance.createVisitante(datos)
                else RetrofitClient.instance.updateVisitante(actual.id!!, datos)
            }) { cargar() }
        }
    }

    /** Nombre (obligatorio), documento y teléfono, con el mismo diálogo de residentes. */
    private fun pedirDatos(@StringRes titulo: Int, nombre: String?, doc: String?, tel: String?,
                           alGuardar: (String, String?, String?) -> Unit) {
        val vista = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_agregar_residente, null)
        vista.findViewById<View>(R.id.tilTipoResidente).visibility = View.GONE
        val etNombre = vista.findViewById<TextInputEditText>(R.id.etNombreResidente).apply { setText(nombre) }
        val etDoc = vista.findViewById<TextInputEditText>(R.id.etIdentificacionResidente).apply { setText(doc) }
        val etTel = vista.findViewById<TextInputEditText>(R.id.etTelefonoResidente).apply { setText(tel) }
        val dialogo = MaterialAlertDialogBuilder(requireContext())
            .setTitle(titulo).setView(vista)
            .setPositiveButton(R.string.btn_guardar, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val n = etNombre.text?.toString()?.trim().orEmpty()
            if (n.isBlank()) {
                vista.findViewById<TextInputLayout>(R.id.tilNombreResidente).error = getString(R.string.residente_nombre_obligatorio)
                return@setOnClickListener
            }
            dialogo.dismiss()
            alGuardar(n, etDoc.text?.toString()?.trim()?.ifBlank { null }, etTel.text?.toString()?.trim()?.ifBlank { null })
        }
    }

    private fun editarCitofonos() {
        val margen = (24 * resources.displayMetrics.density).toInt()
        fun campo(@StringRes hint: Int, valor: String?) = TextInputEditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_PHONE
            setText(valor)
        }.let { et -> et to TextInputLayout(requireContext()).apply { this.hint = getString(hint); addView(et) } }
        val (et1, til1) = campo(R.string.hogar_citofono1, citofono1)
        val (et2, til2) = campo(R.string.hogar_citofono2, citofono2)
        val caja = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(margen, margen / 2, margen, 0)
            addView(til1)
            addView(til2)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.hogar_citofonos)
            .setView(caja)
            .setPositiveButton(R.string.btn_guardar) { _, _ ->
                val pedido = CitofonosRequest(et1.text?.toString()?.trim()?.ifBlank { null }, et2.text?.toString()?.trim()?.ifBlank { null })
                ejecutarAccion(getString(R.string.hogar_citofonos_guardados), { RetrofitClient.instance.cambiarCitofonos(aptoId, pedido) }) { cargar() }
            }
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
