package com.example.conjuntoresidencial.ui.auth

import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.model.LoginAdminRequest
import com.example.conjuntoresidencial.model.LoginApartamentoRequest
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
    SessionManager(contexto).guardar(respuesta, credenciales?.documento)
    val irAlPanel = {
        findNavController().navigate(R.id.inicioFragment, null, NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build())
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

/** Inicia sesión con unas credenciales guardadas (huella) o confirmadas (Ajustes). */
suspend fun iniciarSesion(credenciales: AccesoBiometrico.Credenciales): Response<LoginResponse> =
    if (credenciales.esAdmin) {
        RetrofitClient.instance.loginAdmin(LoginAdminRequest(credenciales.usuario.orEmpty(), credenciales.contrasena))
    } else if (credenciales.documento != null) {
        RetrofitClient.instance.loginResidente(LoginResidenteRequest(credenciales.documento, credenciales.contrasena))
    } else if (credenciales.torre != null) {
        // P5: el titular con torre, apto y rol
        RetrofitClient.instance.loginResidente(LoginResidenteRequest(
            contrasena = credenciales.contrasena, torre = credenciales.torre, apto = credenciales.apto, rol = credenciales.rol))
    } else {
        RetrofitClient.instance.loginApartamento(
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
    AccesoBiometrico.entrar(this, alObtener = { credenciales ->
        cargando(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val respuesta = iniciarSesion(credenciales)
                val datos = respuesta.body()
                when {
                    respuesta.isSuccessful && datos != null -> {
                        completarInicioSesion(datos, null)
                        return@launch
                    }
                    // La contraseña cambió desde otro celular o la restableció la administración
                    respuesta.code() == 401 -> {
                        val cuentaVieja = credenciales.cuenta(requireContext())
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
