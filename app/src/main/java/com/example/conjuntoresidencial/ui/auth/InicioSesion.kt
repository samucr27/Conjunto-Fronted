package com.example.conjuntoresidencial.ui.auth

import android.text.InputFilter
import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.conjuntoresidencial.model.ReenviarCodigo
import com.example.conjuntoresidencial.model.VerificarCodigo
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.ConfigApp
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.model.LoginAdminRequest
import com.example.conjuntoresidencial.model.LoginApartamentoRequest
import com.example.conjuntoresidencial.model.LoginBiometria
import com.example.conjuntoresidencial.model.LoginResidenteRequest
import com.example.conjuntoresidencial.model.LoginResponse
import com.example.conjuntoresidencial.seguridad.AccesoBiometrico
import com.example.conjuntoresidencial.util.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import retrofit2.Response
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.controlesFlotantes

/**
 * Lo común al entrar (con contraseña o con huella): guarda la sesión y va al panel del rol,
 * vaciando el historial para que "atrás" no vuelva al login. Si se entró con contraseña y el
 * celular tiene huella o rostro, ofrece usarlos la próxima vez (una vez por cuenta).
 */
fun Fragment.completarInicioSesion(respuesta: LoginResponse, credenciales: AccesoBiometrico.Credenciales?) {
    val contexto = requireContext()
    // P7 bloque 5: celular nuevo para la cuenta. La respuesta no trae token ni rol: primero el código
    respuesta.verificacionId?.let { id ->
        pedirCodigo(id, respuesta.correoOculto, credenciales)
        return
    }
    // P7: cada app es para unos roles (la recepción, solo la portería; la administración, no los residentes)
    if (respuesta.rol !in ConfigApp.rolesPermitidos) {
        aviso(R.string.login_rol_no_permitido).show()
        return
    }
    SessionManager(contexto).guardar(respuesta, credenciales?.documento)
    // Toast y no aviso: se muestra mientras ya se abre el panel
    respuesta.sesionCerradaEn?.let {
        Toast.makeText(contexto, getString(R.string.verificacion_sesion_cerrada_en, it), Toast.LENGTH_LONG).show()
    }
    val irAlPanel = {
        // La portería solo tiene su pantalla de visitas; en la recepción, primero el turno (P7)
        val inicio = if (respuesta.rol == "PORTERIA") ConfigApp.destinoBloqueo ?: R.id.porteriaFragment else R.id.inicioFragment
        findNavController().navigate(inicio, null, NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build())
    }
    val cuenta = credenciales?.cuenta(contexto)
    if (credenciales == null || cuenta == null || !AccesoBiometrico.conviene(contexto, cuenta)) {
        irAlPanel()
        return
    }
    MaterialAlertDialogBuilder(contexto)
        .setIcon(R.drawable.ic_lock)
        .setTitle(R.string.biometria_ofrecer_titulo)
        .setMessage(getString(R.string.biometria_ofrecer_mensaje, cuenta))
        .setCancelable(false)
        .setPositiveButton(R.string.biometria_activar) { _, _ ->
            AccesoBiometrico.activar(this, credenciales) { activada ->
                // Toast y no aviso: se muestra mientras ya se abre el panel
                if (activada) Toast.makeText(contexto, R.string.biometria_activada, Toast.LENGTH_SHORT).show()
                irAlPanel()
            }
        }
        .setNegativeButton(R.string.ahora_no) { _, _ ->
            AccesoBiometrico.noVolverAPreguntar(contexto, cuenta)
            irAlPanel()
        }
        .show()
}

/**
 * P7 bloque 5: la primera vez en este celular el servidor envía un código al correo de la cuenta.
 * El diálogo sigue abierto si el código no coincide; "Reenviar" pide otro (uno por minuto).
 */
private fun Fragment.pedirCodigo(verificacionId: String, correo: String?, credenciales: AccesoBiometrico.Credenciales?) {
    val contexto = requireContext()
    val campo = TextInputEditText(contexto).apply {
        inputType = InputType.TYPE_CLASS_NUMBER
        filters = arrayOf(InputFilter.LengthFilter(6))
        imeOptions = EditorInfo.IME_ACTION_DONE
    }
    val margen = (24 * resources.displayMetrics.density).toInt()
    val til = TextInputLayout(contexto).apply {
        hint = getString(R.string.verificacion_codigo)
        addView(campo)
        setPadding(margen, margen / 2, margen, 0)
    }
    val dialogo = MaterialAlertDialogBuilder(contexto)
        .setIcon(R.drawable.ic_lock)
        .setTitle(R.string.verificacion_titulo)
        .setMessage(getString(R.string.verificacion_mensaje, correo ?: getString(R.string.verificacion_tu_correo)))
        .setView(til)
        .setCancelable(false)
        .setPositiveButton(R.string.verificacion_entrar, null)
        .setNeutralButton(R.string.verificacion_reenviar, null)
        .setNegativeButton(R.string.btn_cancelar, null)
        .show()
    // Si la pantalla del login se va (entró, giró el celular), el diálogo no queda huérfano encima
    viewLifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, evento ->
        if (evento == Lifecycle.Event.ON_DESTROY) dialogo.dismiss()
    })
    val entrar = dialogo.getButton(AlertDialog.BUTTON_POSITIVE)
    val reenviar = dialogo.getButton(AlertDialog.BUTTON_NEUTRAL)
    campo.setOnEditorActionListener { _, accion, _ -> (accion == EditorInfo.IME_ACTION_DONE).also { if (it) entrar.performClick() } }
    campo.requestFocus()
    entrar.setOnClickListener {
        val codigo = campo.text?.toString()?.trim().orEmpty()
        if (codigo.length != 6) {
            til.error = getString(R.string.verificacion_codigo_corto)
            return@setOnClickListener
        }
        entrar.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            val resultado = try {
                val r = RetrofitClient.comun.verificarCodigo(VerificarCodigo(verificacionId, codigo))
                val sesion = r.body()
                if (r.isSuccessful && sesion != null) Result.success(sesion)
                else Result.failure(Exception(RetrofitClient.mensajeDeError(r, getString(R.string.verificacion_error))))
            } catch (e: Exception) {
                Result.failure(Exception(RetrofitClient.mensajeDeExcepcion(e)))
            }
            entrar.isEnabled = true
            resultado.onSuccess { sesion ->
                dialogo.dismiss()
                if (view != null) completarInicioSesion(sesion, credenciales)
            }.onFailure { e ->
                til.error = e.message
                campo.setText("")
            }
        }
    }
    reenviar.setOnClickListener {
        reenviar.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            val mensaje = try {
                val r = RetrofitClient.comun.reenviarCodigo(ReenviarCodigo(verificacionId))
                if (r.isSuccessful) getString(R.string.verificacion_reenviado, r.body()?.correoOculto ?: correo.orEmpty())
                else RetrofitClient.mensajeDeError(r, getString(R.string.verificacion_error))
            } catch (e: Exception) {
                RetrofitClient.mensajeDeExcepcion(e)
            }
            reenviar.isEnabled = true
            til.error = null
            til.helperText = mensaje
        }
    }
}

/** Inicia sesión con unas credenciales guardadas (huella) o confirmadas (Ajustes). */
suspend fun iniciarSesion(credenciales: AccesoBiometrico.Credenciales): Response<LoginResponse> =
    if (credenciales.esAdmin) {
        RetrofitClient.comun.loginAdmin(LoginAdminRequest(credenciales.usuario.orEmpty(), credenciales.contrasena))
    } else if (credenciales.documento != null) {
        RetrofitClient.comun.loginResidente(LoginResidenteRequest(credenciales.documento, credenciales.contrasena))
    } else if (credenciales.torre != null) {
        // P5: el titular con torre, apto y rol
        RetrofitClient.comun.loginResidente(LoginResidenteRequest(
            contrasena = credenciales.contrasena, torre = credenciales.torre, apto = credenciales.apto, rol = credenciales.rol))
    } else {
        RetrofitClient.comun.loginApartamento(
            LoginApartamentoRequest(credenciales.torre.orEmpty(), credenciales.apto.orEmpty(), credenciales.contrasena)
        )
    }

/** Las credenciales de la sesión actual con [contrasena] (para activar la huella desde Ajustes). */
fun SessionManager.credenciales(contrasena: String) =
    when {
        esAdmin() -> AccesoBiometrico.Credenciales(ROL_ADMIN, usuario = getUsuario(), contrasena = contrasena)
        getDocumento() != null -> AccesoBiometrico.Credenciales(getRol() ?: "PROPIETARIO", contrasena = contrasena, documento = getDocumento())
        else -> AccesoBiometrico.Credenciales(getRol() ?: "PROPIETARIO", torre = getTorre(), apto = getApto(), contrasena = contrasena)
    }

private const val ROL_ADMIN = "ADMIN"

/** La huella se pide sola una vez por apertura de la app; después, con el botón. */
private object HuellaOfrecida { var valor = false }

/**
 * P5: la huella va en la pantalla de inicio de sesión (ya no hay selector de entrada). [boton]
 * solo se ve si hay una cuenta guardada; [cargando] bloquea el formulario mientras entra.
 */
fun Fragment.ofrecerHuella(boton: com.google.android.material.button.MaterialButton, cargando: (Boolean) -> Unit) {
    val cuenta = AccesoBiometrico.cuentaGuardada(requireContext())
    boton.visibility = if (cuenta != null) android.view.View.VISIBLE else android.view.View.GONE
    if (cuenta == null) return
    boton.text = getString(R.string.biometria_entrar_cuenta, cuenta)
    boton.setOnClickListener { entrarConHuella(boton, cargando) }
    if (!HuellaOfrecida.valor) {
        HuellaOfrecida.valor = true
        controlesFlotantes?.alTerminarCarga { if (view != null) entrarConHuella(boton, cargando) }
    }
}

private fun Fragment.entrarConHuella(boton: com.google.android.material.button.MaterialButton, cargando: (Boolean) -> Unit) {
    AccesoBiometrico.entrar(this, alObtener = { activacion ->
        cargando(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // P6: se canjea la credencial opaca del celular por una sesión; la contraseña no existe aquí
                val respuesta = RetrofitClient.comun.loginBiometria(LoginBiometria(activacion.dispositivoId, activacion.credencial))
                val datos = respuesta.body()
                when {
                    respuesta.isSuccessful && datos != null -> {
                        completarInicioSesion(datos, null)
                        return@launch
                    }
                    // La contraseña cambió, la huella se quitó desde otro celular o la cuenta ya no existe
                    respuesta.code() == 401 -> {
                        val cuentaVieja = AccesoBiometrico.cuentaGuardada(requireContext()).orEmpty()
                        AccesoBiometrico.desactivar(requireContext())
                        aviso(getString(R.string.biometria_contrasena_cambio, cuentaVieja)).show()
                        boton.visibility = android.view.View.GONE
                    }
                    else -> aviso(RetrofitClient.mensajeDeError(respuesta, getString(R.string.biometria_error_datos))).show()
                }
            } catch (e: Exception) {
                aviso(RetrofitClient.mensajeDeExcepcion(e)).show()
            }
            cargando(false)
        }
    }, alFallar = { mensaje ->
        if (view == null) return@entrar
        mensaje?.let { aviso(it).show() }
        if (AccesoBiometrico.cuentaGuardada(requireContext()) == null) boton.visibility = android.view.View.GONE
    })
}
