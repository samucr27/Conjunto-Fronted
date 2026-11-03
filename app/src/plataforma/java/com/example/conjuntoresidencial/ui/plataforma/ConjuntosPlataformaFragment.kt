package com.example.conjuntoresidencial.ui.plataforma

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.text.InputType
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.api.instance
import com.example.conjuntoresidencial.databinding.FragmentListaPlataformaBinding
import com.example.conjuntoresidencial.model.AccesoPrincipal
import com.example.conjuntoresidencial.model.CambioEstadoConjunto
import com.example.conjuntoresidencial.model.CambioPropiaContrasenaRequest
import com.example.conjuntoresidencial.model.ConjuntoPlataforma
import com.example.conjuntoresidencial.model.DatosConjunto
import com.example.conjuntoresidencial.model.NuevoConjunto
import com.example.conjuntoresidencial.ui.comun.DiffPorId
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.confirmar
import com.example.conjuntoresidencial.ui.comun.controlesFlotantes
import com.example.conjuntoresidencial.ui.comun.elegirOpcion
import com.example.conjuntoresidencial.ui.comun.mostrarRecurso
import com.example.conjuntoresidencial.ui.comun.pedirTexto
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.util.Resource
import com.example.conjuntoresidencial.util.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.Response

/**
 * P7: los conjuntos de la plataforma. Crear uno (con su administrador principal), editar sus
 * datos, ver sus cuentas de administración, restablecer la contraseña del principal, suspenderlo
 * o reactivarlo y ver su auditoría. Los datos de los residentes no se ven desde aquí.
 */
class ConjuntosPlataformaFragment : Fragment() {

    private var _binding: FragmentListaPlataformaBinding? = null
    private val binding get() = _binding!!
    private val adaptador = ConjuntoAdapter { opciones(it) }
    // Los 33 del servidor; se piden al abrir el formulario la primera vez
    private var departamentos: List<String>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentListaPlataformaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(binding.encabezado, R.string.plataforma_conjuntos_titulo, R.string.plataforma_conjuntos_descripcion,
            atras = false, menu = R.menu.menu_conjuntos_plataforma) { id ->
            when (id) {
                R.id.accion_actualizar -> { binding.swipe.isRefreshing = true; cargar(); true }
                R.id.accion_auditoria -> { findNavController().navigate(R.id.auditoriaPlataformaFragment); true }
                R.id.accion_contrasena -> { cambiarContrasena(); true }
                R.id.accion_cerrar_sesion -> { confirmarSalir(); true }
                else -> false
            }
        }
        binding.lista.layoutManager = LinearLayoutManager(requireContext())
        binding.lista.adapter = adaptador
        binding.estado.vincular(binding.lista)
        binding.swipe.setOnRefreshListener { cargar() }
        controlesFlotantes?.mostrarBotonFlotante(R.string.plataforma_nuevo, R.drawable.ic_add) { formulario(null) }
        // Es la primera pantalla: "atrás" pregunta si cerrar la sesión
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { confirmarSalir() }
        cargar()
    }

    private fun cargar() {
        viewLifecycleOwner.lifecycleScope.launch {
            val resource = try {
                val r = RetrofitClient.instance.conjuntos()
                if (r.isSuccessful) Resource.Success(r.body().orEmpty())
                else Resource.Error(RetrofitClient.mensajeDeError(r, getString(R.string.plataforma_error_cargar)))
            } catch (e: Exception) {
                Resource.Error(RetrofitClient.mensajeDeExcepcion(e))
            }
            if (_binding == null) return@launch
            binding.estado.mostrarRecurso(resource, binding.swipe, R.drawable.ic_apartment,
                getString(R.string.plataforma_vacio), reintentar = { cargar() }) { lista ->
                adaptador.submitList(lista)
                val suspendidos = lista.count { !it.activo }
                binding.encabezado.ponerInfo((listOf(resources.getQuantityString(R.plurals.plataforma_resumen_conjuntos, lista.size, lista.size)) +
                    if (suspendidos > 0) listOf(resources.getQuantityString(R.plurals.plataforma_resumen_suspendidos, suspendidos, suspendidos))
                    else emptyList()).joinToString(" · "))
            }
            if (resource is Resource.Error) binding.encabezado.ponerInfo(null)
        }
    }

    private fun opciones(c: ConjuntoPlataforma) {
        elegirOpcion(c.nombre, listOf(
            R.string.plataforma_editar to { formulario(c) },
            R.string.plataforma_administracion to { verCuentas(c) },
            R.string.plataforma_restablecer to { restablecer(c) },
            (if (c.activo) R.string.plataforma_suspender else R.string.plataforma_reactivar) to { cambiarEstado(c) },
            R.string.plataforma_auditoria to {
                findNavController().navigate(R.id.auditoriaPlataformaFragment,
                    bundleOf(AuditoriaPlataformaFragment.ARG_CONJUNTO to c.id, AuditoriaPlataformaFragment.ARG_TITULO to c.nombre))
            }
        ))
    }

    /** Nuevo conjunto ([editando] null: también su administrador principal) o editar sus datos. */
    private fun formulario(editando: ConjuntoPlataforma?) {
        val lista = departamentos
        if (lista == null) {
            viewLifecycleOwner.lifecycleScope.launch {
                val r = try { RetrofitClient.instance.departamentos() } catch (e: Exception) { null }
                if (_binding == null) return@launch
                val cuerpo = r?.body()
                if (r?.isSuccessful == true && !cuerpo.isNullOrEmpty()) {
                    departamentos = cuerpo
                    formulario(editando)
                } else {
                    aviso(R.string.plataforma_error_departamentos).show()
                }
            }
            return
        }
        val vista = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_conjunto_plataforma, null)
        fun campo(til: Int, et: Int) = vista.findViewById<TextInputLayout>(til) to vista.findViewById<TextInputEditText>(et)
        val nombre = campo(R.id.tilNombre, R.id.etNombre)
        val municipio = campo(R.id.tilMunicipio, R.id.etMunicipio)
        val direccion = campo(R.id.tilDireccion, R.id.etDireccion)
        val usuario = campo(R.id.tilUsuario, R.id.etUsuario)
        val nombrePrincipal = campo(R.id.tilNombrePrincipal, R.id.etNombrePrincipal)
        val correo = campo(R.id.tilCorreo, R.id.etCorreo)
        val tilDepartamento = vista.findViewById<TextInputLayout>(R.id.tilDepartamento)
        val departamento = vista.findViewById<AutoCompleteTextView>(R.id.etDepartamento).apply {
            setAdapter(ArrayAdapter(context, android.R.layout.simple_list_item_1, lista))
            setOnItemClickListener { _, _, _, _ -> tilDepartamento.error = null }
        }
        vista.findViewById<View>(R.id.grupoPrincipal).isVisible = editando == null
        if (editando != null) {
            nombre.second.setText(editando.nombre)
            departamento.setText(editando.departamento, false)
            municipio.second.setText(editando.municipio)
            direccion.second.setText(editando.direccion)
        }
        val dialogo = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (editando == null) R.string.plataforma_nuevo else R.string.plataforma_editar)
            .setView(vista)
            .setPositiveButton(R.string.btn_guardar, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            fun texto(c: Pair<TextInputLayout, TextInputEditText>) = c.second.text?.toString()?.trim().orEmpty()
            val obligatorio = getString(R.string.equipo_dato_obligatorio)
            val revisar = mutableListOf(nombre, municipio)
            if (editando == null) revisar += listOf(usuario, nombrePrincipal)
            revisar.forEach { it.first.error = if (texto(it).isEmpty()) obligatorio else null }
            tilDepartamento.error = if (departamento.text.toString() !in lista) getString(R.string.plataforma_elige_departamento) else null
            if (editando == null) {
                if (texto(usuario).isNotEmpty() && !USUARIO.matches(texto(usuario))) usuario.first.error = getString(R.string.plataforma_usuario_invalido)
                correo.first.error = if (texto(correo).isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(texto(correo)).matches())
                    getString(R.string.equipo_correo_invalido) else null
            }
            if (listOf(nombre, municipio, usuario, nombrePrincipal, correo).any { it.first.error != null } || tilDepartamento.error != null) {
                return@setOnClickListener
            }
            val dir = texto(direccion).ifEmpty { null }
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                val error = try {
                    if (editando == null) {
                        val r = RetrofitClient.instance.crear(NuevoConjunto(texto(nombre), departamento.text.toString(), texto(municipio),
                            dir, texto(usuario), texto(nombrePrincipal), texto(correo).ifEmpty { null }))
                        val acceso = r.body()
                        if (r.isSuccessful && acceso != null) null.also { dialogo.dismiss(); mostrarAcceso(acceso, true); cargar() }
                        else RetrofitClient.mensajeDeError(r, getString(R.string.plataforma_error_guardar))
                    } else {
                        val r = RetrofitClient.instance.editar(editando.id,
                            DatosConjunto(texto(nombre), departamento.text.toString(), texto(municipio), dir))
                        if (r.isSuccessful) null.also { dialogo.dismiss(); aviso(R.string.plataforma_guardado).show(); cargar() }
                        else RetrofitClient.mensajeDeError(r, getString(R.string.plataforma_error_guardar))
                    }
                } catch (e: Exception) {
                    RetrofitClient.mensajeDeExcepcion(e)
                }
                if (error != null && view != null) {
                    dialogo.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                    aviso(error).show()
                }
            }
        }
    }

    /** El usuario y la contraseña temporal del principal: solo se ven esta vez. */
    private fun mostrarAcceso(acceso: AccesoPrincipal, nuevo: Boolean) {
        val dialogo = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (nuevo) R.string.plataforma_acceso_creado else R.string.plataforma_acceso_restablecido)
            .setMessage(getString(R.string.plataforma_acceso_mensaje, acceso.usuario, acceso.contrasenaTemporal))
            .setPositiveButton(R.string.plataforma_listo, null)
            .setNeutralButton(R.string.plataforma_copiar, null)
            .setCancelable(false)
            .show()
        // Copiar no cierra el diálogo: se puede volver a leer
        dialogo.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            val portapapeles = requireContext().getSystemService(ClipboardManager::class.java)
            val clip = ClipData.newPlainText(getString(R.string.login_contrasena), acceso.contrasenaTemporal)
            // Android 13+: no se muestra la contraseña en la vista previa del portapapeles
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                clip.description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
            }
            portapapeles?.setPrimaryClip(clip)
            aviso(R.string.plataforma_copiada).show()
        }
    }

    private fun verCuentas(c: ConjuntoPlataforma) {
        viewLifecycleOwner.lifecycleScope.launch {
            val r = try { RetrofitClient.instance.cuentas(c.id) } catch (e: Exception) {
                aviso(RetrofitClient.mensajeDeExcepcion(e)).show(); return@launch
            }
            if (view == null) return@launch
            val cuentas = r.body()
            if (!r.isSuccessful || cuentas == null) {
                aviso(RetrofitClient.mensajeDeError(r, getString(R.string.plataforma_error_cargar))).show()
                return@launch
            }
            val filas = cuentas.map { cuenta ->
                val rol = getString(when {
                    cuenta.rol == "CONTADOR" -> R.string.rol_contador
                    cuenta.rol == "PORTERIA" -> R.string.rol_porteria
                    cuenta.principal -> R.string.equipo_principal
                    else -> R.string.rol_asistente
                })
                val estado = if (cuenta.activo) "" else " · " + getString(R.string.plataforma_de_baja)
                "${cuenta.nombreCompleto ?: cuenta.usuario}\n$rol · ${cuenta.usuario}$estado"
            }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.plataforma_administracion_de, c.nombre))
                .apply {
                    if (filas.isEmpty()) setMessage(R.string.plataforma_sin_cuentas)
                    else setItems(filas.toTypedArray(), null)
                }
                .setPositiveButton(R.string.btn_cerrar, null)
                .show()
        }
    }

    private fun restablecer(c: ConjuntoPlataforma) {
        confirmar(getString(R.string.plataforma_restablecer_titulo),
            getString(R.string.plataforma_restablecer_mensaje, c.principal ?: getString(R.string.plataforma_sin_principal), c.nombre),
            R.string.plataforma_restablecer_confirmar) {
            viewLifecycleOwner.lifecycleScope.launch {
                val error = try {
                    val r = RetrofitClient.instance.restablecerPrincipal(c.id)
                    val acceso = r.body()
                    if (r.isSuccessful && acceso != null) null.also { mostrarAcceso(acceso, false); cargar() }
                    else RetrofitClient.mensajeDeError(r, getString(R.string.plataforma_error_guardar))
                } catch (e: Exception) {
                    RetrofitClient.mensajeDeExcepcion(e)
                }
                if (error != null && view != null) aviso(error).show()
            }
        }
    }

    /** Suspender pide el motivo (queda en la auditoría) y avisa que nadie del conjunto podrá entrar. */
    private fun cambiarEstado(c: ConjuntoPlataforma) {
        val activar = !c.activo
        pedirTexto(getString(if (activar) R.string.plataforma_reactivar_titulo else R.string.plataforma_suspender_titulo, c.nombre),
            R.string.plataforma_motivo, null, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES,
            validar = { if (it.isBlank()) getString(R.string.plataforma_falta_motivo) else null }) { motivo ->
            ejecutar(getString(if (activar) R.string.plataforma_reactivado else R.string.plataforma_suspendido)) {
                RetrofitClient.instance.cambiarEstado(c.id, CambioEstadoConjunto(activar, motivo))
            }
        }
    }

    private fun ejecutar(exito: String, peticion: suspend () -> Response<*>) {
        viewLifecycleOwner.lifecycleScope.launch {
            val mensaje = try {
                val r = peticion()
                if (r.isSuccessful) exito.also { cargar() }
                else RetrofitClient.mensajeDeError(r, getString(R.string.plataforma_error_guardar))
            } catch (e: Exception) {
                RetrofitClient.mensajeDeExcepcion(e)
            }
            if (view != null) aviso(mensaje).show()
        }
    }

    /** Como Cuenta → Perfil: la actual y la nueva. El servidor devuelve un token nuevo. */
    private fun cambiarContrasena() {
        val contexto = requireContext()
        fun campo(hint: Int) = TextInputLayout(contexto).apply {
            this.hint = getString(hint)
            endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
            addView(TextInputEditText(contexto).apply { inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD })
        }
        val actual = campo(R.string.plataforma_contrasena_actual)
        val nueva = campo(R.string.plataforma_contrasena_nueva)
        val margen = (24 * resources.displayMetrics.density).toInt()
        val vista = LinearLayout(contexto).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(margen, margen / 2, margen, 0)
            addView(actual)
            addView(nueva)
        }
        val dialogo = MaterialAlertDialogBuilder(contexto)
            .setTitle(R.string.perfil_cambiar_contrasena)
            .setView(vista)
            .setPositiveButton(R.string.btn_guardar, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val a = actual.editText?.text?.toString().orEmpty()
            val n = nueva.editText?.text?.toString().orEmpty()
            actual.error = if (a.isEmpty()) getString(R.string.login_falta_contrasena) else null
            nueva.error = if (n.length < 6) getString(R.string.error_contrasena_corta) else null
            if (actual.error != null || nueva.error != null) return@setOnClickListener
            viewLifecycleOwner.lifecycleScope.launch {
                val error = try {
                    val r = RetrofitClient.instance.cambiarContrasena(CambioPropiaContrasenaRequest(a, n))
                    val sesion = r.body()
                    if (r.isSuccessful && sesion != null) {
                        SessionManager(contexto).guardar(sesion)
                        null
                    } else RetrofitClient.mensajeDeError(r, getString(R.string.plataforma_error_guardar))
                } catch (e: Exception) {
                    RetrofitClient.mensajeDeExcepcion(e)
                }
                if (error == null) {
                    dialogo.dismiss()
                    if (view != null) aviso(R.string.plataforma_contrasena_cambiada).show()
                } else {
                    actual.error = error
                }
            }
        }
    }

    private fun confirmarSalir() {
        confirmar(getString(R.string.cerrar_sesion), getString(R.string.cerrar_sesion_pregunta), R.string.cerrar_sesion_confirmar) {
            val contexto = requireContext().applicationContext
            viewLifecycleOwner.lifecycleScope.launch {
                // El token deja de valer en el servidor; sin conexión se sale igual
                withTimeoutOrNull(ESPERA_LOGOUT_MS) {
                    try { RetrofitClient.instance.cerrarSesion() } catch (_: Exception) { }
                }
                SessionManager(contexto).clearSession()
                findNavController().navigate(R.id.loginPlataformaFragment, null,
                    NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class ConjuntoAdapter(private val onTocar: (ConjuntoPlataforma) -> Unit) :
        ListAdapter<ConjuntoPlataforma, ConjuntoAdapter.VH>(DiffPorId { it.id }) {
        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val nombre: TextView = v.findViewById(android.R.id.text1)
            val detalle: TextView = v.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false))

        override fun onBindViewHolder(h: VH, position: Int) {
            val c = getItem(position)
            val contexto = h.itemView.context
            h.nombre.text = if (c.activo) c.nombre else contexto.getString(R.string.plataforma_nombre_suspendido, c.nombre)
            val apartamentos = contexto.resources.getQuantityString(R.plurals.plataforma_apartamentos, c.apartamentos.toInt(), c.apartamentos.toInt())
            h.detalle.text = contexto.getString(R.string.plataforma_fila_detalle, c.municipio, c.departamento, apartamentos,
                c.principal ?: contexto.getString(R.string.plataforma_sin_principal))
            h.itemView.alpha = if (c.activo) 1f else 0.6f
            h.itemView.setOnClickListener { onTocar(c) }
        }
    }

    companion object {
        private const val ESPERA_LOGOUT_MS = 4_000L
        // Lo mismo que valida el servidor (PlataformaDTO.NuevoConjunto)
        private val USUARIO = Regex("[A-Za-z0-9._-]{3,60}")
    }
}
