package com.example.conjuntoresidencial.ui.plataforma

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.os.Build
import android.os.PersistableBundle
import android.text.InputType
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.api.instance
import com.example.conjuntoresidencial.model.AccesoPrincipal
import com.example.conjuntoresidencial.model.CambioEstadoConjunto
import com.example.conjuntoresidencial.model.CambioPropiaContrasenaRequest
import com.example.conjuntoresidencial.model.ConjuntoPlataforma
import com.example.conjuntoresidencial.model.DatosConjunto
import com.example.conjuntoresidencial.model.NuevoConjunto
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.confirmar
import com.example.conjuntoresidencial.ui.comun.pedirTexto
import com.example.conjuntoresidencial.util.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * P8 bloque 6: lo que la plataforma hace sobre un conjunto (crear o editar, restablecer el acceso
 * del principal, suspender o reactivar) y sobre su sesión (contraseña, salir). Antes vivía en la
 * lista de conjuntos; ahora lo usan también el panel y el detalle del conjunto. [alTerminar] se
 * llama cuando el cambio quedó guardado, para volver a cargar la pantalla.
 */

// Lo mismo que valida el servidor (PlataformaDTO.NuevoConjunto)
private val USUARIO = Regex("[A-Za-z0-9._-]{3,60}")
private const val ESPERA_LOGOUT_MS = 4_000L

// Los 33 del servidor; se piden al abrir el formulario la primera vez
private var departamentos: List<String>? = null

/** Nuevo conjunto ([editando] null: también su administrador principal) o editar sus datos. */
internal fun Fragment.formularioConjunto(editando: ConjuntoPlataforma?, alTerminar: () -> Unit) {
    val lista = departamentos
    if (lista == null) {
        viewLifecycleOwner.lifecycleScope.launch {
            val r = try { RetrofitClient.instance.departamentos() } catch (e: Exception) { null }
            if (view == null) return@launch
            val cuerpo = r?.body()
            if (r?.isSuccessful == true && !cuerpo.isNullOrEmpty()) {
                departamentos = cuerpo
                formularioConjunto(editando, alTerminar)
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
    val maxDispositivos = campo(R.id.tilMaxDispositivos, R.id.etMaxDispositivos)
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
        editando.maxDispositivos?.let { maxDispositivos.second.setText(it.toString()) }
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
        val maximo = texto(maxDispositivos).toIntOrNull()
        maxDispositivos.first.error = if (maximo == null || maximo !in 1..10) getString(R.string.plataforma_max_dispositivos_invalido) else null
        if (listOf(nombre, municipio, usuario, nombrePrincipal, correo, maxDispositivos).any { it.first.error != null } || tilDepartamento.error != null) {
            return@setOnClickListener
        }
        val dir = texto(direccion).ifEmpty { null }
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            val error = try {
                if (editando == null) {
                    val r = RetrofitClient.instance.crear(NuevoConjunto(texto(nombre), departamento.text.toString(), texto(municipio),
                        dir, texto(usuario), texto(nombrePrincipal), texto(correo).ifEmpty { null }, maximo))
                    val acceso = r.body()
                    if (r.isSuccessful && acceso != null) null.also { dialogo.dismiss(); mostrarAcceso(acceso, true); alTerminar() }
                    else RetrofitClient.mensajeDeError(r, getString(R.string.plataforma_error_guardar))
                } else {
                    val r = RetrofitClient.instance.editar(editando.id,
                        DatosConjunto(texto(nombre), departamento.text.toString(), texto(municipio), dir, maximo))
                    if (r.isSuccessful) null.also { dialogo.dismiss(); aviso(R.string.plataforma_guardado).show(); alTerminar() }
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
internal fun Fragment.mostrarAcceso(acceso: AccesoPrincipal, nuevo: Boolean) {
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

internal fun Fragment.restablecerPrincipal(c: ConjuntoPlataforma, alTerminar: () -> Unit) {
    confirmar(getString(R.string.plataforma_restablecer_titulo),
        getString(R.string.plataforma_restablecer_mensaje, c.principal ?: getString(R.string.plataforma_sin_principal), c.nombre),
        R.string.plataforma_restablecer_confirmar) {
        viewLifecycleOwner.lifecycleScope.launch {
            val error = try {
                val r = RetrofitClient.instance.restablecerPrincipal(c.id)
                val acceso = r.body()
                if (r.isSuccessful && acceso != null) null.also { mostrarAcceso(acceso, false); alTerminar() }
                else RetrofitClient.mensajeDeError(r, getString(R.string.plataforma_error_guardar))
            } catch (e: Exception) {
                RetrofitClient.mensajeDeExcepcion(e)
            }
            if (error != null && view != null) aviso(error).show()
        }
    }
}

/** Suspender pide el motivo (queda en la auditoría): nadie del conjunto podrá entrar. */
internal fun Fragment.cambiarEstadoConjunto(c: ConjuntoPlataforma, alTerminar: () -> Unit) {
    val activar = !c.activo
    pedirTexto(getString(if (activar) R.string.plataforma_reactivar_titulo else R.string.plataforma_suspender_titulo, c.nombre),
        R.string.plataforma_motivo, null, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES,
        validar = { if (it.isBlank()) getString(R.string.plataforma_falta_motivo) else null }) { motivo ->
        viewLifecycleOwner.lifecycleScope.launch {
            val mensaje = try {
                val r = RetrofitClient.instance.cambiarEstado(c.id, CambioEstadoConjunto(activar, motivo))
                if (r.isSuccessful) getString(if (activar) R.string.plataforma_reactivado else R.string.plataforma_suspendido).also { alTerminar() }
                else RetrofitClient.mensajeDeError(r, getString(R.string.plataforma_error_guardar))
            } catch (e: Exception) {
                RetrofitClient.mensajeDeExcepcion(e)
            }
            if (view != null) aviso(mensaje).show()
        }
    }
}

/** Como Cuenta → Perfil: la actual y la nueva. El servidor devuelve un token nuevo. */
internal fun Fragment.cambiarContrasenaPlataforma() {
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

internal fun Fragment.confirmarSalirPlataforma() {
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

/** El nombre de cada acción de la auditoría, en el orden de los filtros. */
internal val ACCIONES_AUDITORIA: List<Pair<String, Int>> = listOf(
    "CREAR_CONJUNTO" to R.string.plataforma_evento_crear,
    "EDITAR_CONJUNTO" to R.string.plataforma_evento_editar,
    "SUSPENDER_CONJUNTO" to R.string.plataforma_evento_suspender,
    "REACTIVAR_CONJUNTO" to R.string.plataforma_evento_reactivar,
    "RESTABLECER_PRINCIPAL" to R.string.plataforma_evento_restablecer,
    "INICIO_SESION" to R.string.plataforma_evento_inicio
)

internal fun nombreAccion(accion: String): Int =
    ACCIONES_AUDITORIA.firstOrNull { it.first == accion }?.second ?: R.string.plataforma_evento_otro
