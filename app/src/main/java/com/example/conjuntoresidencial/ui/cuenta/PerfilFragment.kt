package com.example.conjuntoresidencial.ui.cuenta

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.DialogCambiarContrasenaBinding
import com.example.conjuntoresidencial.databinding.FragmentPerfilBinding
import com.example.conjuntoresidencial.model.CambioPropiaContrasenaRequest
import com.example.conjuntoresidencial.seguridad.AccesoBiometrico
import com.example.conjuntoresidencial.ui.auth.credenciales
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.util.SessionManager
import com.example.conjuntoresidencial.util.limpiarErrorAlEscribir
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/** Cuenta → Perfil: datos de la sesión y cambio de la propia contraseña. */
class PerfilFragment : Fragment() {
    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sesion = SessionManager(requireContext())
        configurarEncabezado(binding.encabezado, R.string.cuenta_perfil, R.string.perfil_descripcion)

        val rol = getString(when {
            sesion.esContador() -> R.string.rol_contador
            sesion.esPrincipal() -> R.string.equipo_principal
            sesion.esAdmin() -> R.string.rol_asistente
            else -> R.string.rol_residente
        })
        binding.filaRol.estado = rol
        if (sesion.esAdmin()) {
            binding.filaIdentidad.titulo = getString(R.string.cuenta_usuario)
            binding.filaIdentidad.estado = sesion.getUsuario()
        } else {
            binding.filaIdentidad.titulo = getString(R.string.cuenta_apartamento)
            binding.filaIdentidad.estado = getString(R.string.panel_residente_titulo, sesion.getTorre(), sesion.getApto())
        }
        binding.encabezado.ponerInfo(listOf(rol, binding.filaIdentidad.estado).joinToString(" · "))
        // Las filas de datos solo informan: no se pueden tocar
        listOf(binding.filaRol, binding.filaIdentidad).forEach { it.isClickable = false; it.isFocusable = true }
        // La contraseña del apartamento la comparten sus residentes
        binding.tvNotaContrasena.visibility = if (sesion.esAdmin()) View.GONE else View.VISIBLE

        binding.filaCambiarContrasena.setOnClickListener { pedirNuevaContrasena() }
    }

    private fun pedirNuevaContrasena() {
        val vista = DialogCambiarContrasenaBinding.inflate(layoutInflater)
        listOf(vista.tilContrasenaActual, vista.tilContrasenaNueva, vista.tilContrasenaRepetir)
            .forEach { it.limpiarErrorAlEscribir() }
        val dialogo = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.perfil_cambiar_contrasena)
            .setView(vista.root)
            .setPositiveButton(R.string.btn_cambiar, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
        // Se asigna después de show() para que el diálogo no se cierre si algo falta
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val actual = vista.etContrasenaActual.text.toString()
            val nueva = vista.etContrasenaNueva.text.toString()
            val repetida = vista.etContrasenaRepetir.text.toString()
            vista.tilContrasenaActual.error = if (actual.isEmpty()) getString(R.string.contrasena_falta_actual) else null
            vista.tilContrasenaNueva.error = if (nueva.length < MIN_CONTRASENA) getString(R.string.contrasena_muy_corta) else null
            vista.tilContrasenaRepetir.error = if (repetida != nueva) getString(R.string.contrasena_no_coinciden) else null
            if (actual.isEmpty() || nueva.length < MIN_CONTRASENA || repetida != nueva) return@setOnClickListener
            cambiar(actual, nueva, dialogo, vista)
        }
    }

    private fun cambiar(actual: String, nueva: String, dialogo: AlertDialog, vista: DialogCambiarContrasenaBinding) {
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            val error = try {
                val respuesta = RetrofitClient.comun.cambiarContrasena(CambioPropiaContrasenaRequest(actual, nueva))
                if (respuesta.isSuccessful) null
                else RetrofitClient.mensajeDeError(respuesta, getString(R.string.contrasena_error_cambiar))
            } catch (e: Exception) {
                RetrofitClient.mensajeDeExcepcion(e)
            }
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
            if (error != null) {
                vista.tilContrasenaActual.error = error
                return@launch
            }
            dialogo.dismiss()
            aviso(R.string.contrasena_cambiada).show()
            actualizarHuella(nueva)
        }
    }

    /** Si la huella abre esta cuenta, guarda la contraseña nueva (o la desactiva si no se confirma). */
    private fun actualizarHuella(nueva: String) {
        val contexto = requireContext()
        val credenciales = SessionManager(contexto).credenciales(nueva)
        if (AccesoBiometrico.cuentaGuardada(contexto) != credenciales.cuenta(contexto)) return
        aviso(R.string.biometria_reconfirmar).show()
        AccesoBiometrico.activar(this, credenciales) { activada ->
            if (!activada) {
                AccesoBiometrico.desactivar(contexto)
                if (_binding != null) aviso(R.string.biometria_desactivada).show()
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    private companion object {
        const val MIN_CONTRASENA = 6
    }
}
