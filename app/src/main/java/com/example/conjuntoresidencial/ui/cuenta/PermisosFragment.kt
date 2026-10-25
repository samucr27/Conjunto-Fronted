package com.example.conjuntoresidencial.ui.cuenta

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FragmentPermisosBinding
import com.example.conjuntoresidencial.seguridad.AccesoBiometrico
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado

/**
 * Cuenta → Permisos: lo que la app usa del celular y su estado. Tocar un permiso lleva a
 * donde se cambia (los ajustes del sistema; la app no puede concederlos sola).
 */
class PermisosFragment : Fragment() {
    private var _binding: FragmentPermisosBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPermisosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(binding.encabezado, R.string.cuenta_permisos, R.string.permisos_descripcion)
        binding.filaPermisoNotificaciones.setOnClickListener { abrir(intentNotificaciones()) }
        binding.filaPermisoBiometria.setOnClickListener {
            when (AccesoBiometrico.estado(requireContext())) {
                AccesoBiometrico.Estado.CONFIGURADA -> findNavController().navigate(R.id.ajustesFragment)
                AccesoBiometrico.Estado.SIN_REGISTRAR -> abrir(intentRegistrarHuella())
                AccesoBiometrico.Estado.NO_DISPONIBLE -> Unit
            }
        }
        binding.filaAjustesApp.setOnClickListener {
            abrir(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", requireContext().packageName, null)))
        }
    }

    // Se actualiza al volver de los ajustes del sistema
    override fun onResume() {
        super.onResume()
        val contexto = requireContext()
        val notificaciones = NotificationManagerCompat.from(contexto).areNotificationsEnabled()
        binding.filaPermisoNotificaciones.estado =
            getString(if (notificaciones) R.string.permiso_permitido else R.string.permiso_no_permitido)
        val biometria = AccesoBiometrico.estado(contexto)
        binding.filaPermisoBiometria.estado = getString(when (biometria) {
            AccesoBiometrico.Estado.CONFIGURADA -> R.string.permiso_configurada
            AccesoBiometrico.Estado.SIN_REGISTRAR -> R.string.permiso_sin_registrar
            AccesoBiometrico.Estado.NO_DISPONIBLE -> R.string.permiso_no_disponible
        })
        binding.filaPermisoBiometria.isEnabled = biometria != AccesoBiometrico.Estado.NO_DISPONIBLE
    }

    private fun intentNotificaciones(): Intent {
        val paquete = requireContext().packageName
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, paquete)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", paquete, null))
        }
    }

    // Android 11+ abre directo el registro de huella o rostro; antes, los ajustes de seguridad
    private fun intentRegistrarHuella(): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_BIOMETRIC_ENROLL)
                .putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, BIOMETRIC_STRONG or BIOMETRIC_WEAK)
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }

    private fun abrir(intent: Intent) {
        // Algunos fabricantes no tienen todas las pantallas de ajustes: se cae a la de la app
        try {
            startActivity(intent)
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", requireContext().packageName, null)))
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
