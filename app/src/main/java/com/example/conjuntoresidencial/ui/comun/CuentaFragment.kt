package com.example.conjuntoresidencial.ui.comun

import androidx.lifecycle.lifecycleScope
import com.example.conjuntoresidencial.api.RetrofitClient
import kotlinx.coroutines.launch
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.BuildConfig
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FragmentCuentaBinding
import com.example.conjuntoresidencial.util.SessionManager
import com.example.conjuntoresidencial.util.confirmarCerrarSesion
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Pestaña Cuenta para los dos roles, como los ajustes de One UI: Perfil, Ajustes y Permisos;
 * Legal, Ayuda y Acerca de; y cerrar sesión. Cada fila abre su pantalla (ui/cuenta).
 */
class CuentaFragment : Fragment() {
    private var _binding: FragmentCuentaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCuentaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sesion = SessionManager(requireContext())
        val rol = getString(when {
            sesion.esContador() -> R.string.rol_contador
            sesion.esPrincipal() -> R.string.equipo_principal
            sesion.esAdmin() -> R.string.rol_asistente
            else -> R.string.rol_residente
        })
        val identidad = if (sesion.esAdmin()) sesion.getUsuario()
            else getString(R.string.panel_residente_titulo, sesion.getTorre(), sesion.getApto())

        // Pestaña de la barra inferior: sin botón para volver
        configurarEncabezado(binding.encabezado, R.string.pestana_cuenta, R.string.cuenta_descripcion, atras = false)
        binding.encabezado.ponerInfo(listOf(rol, identidad).filter { it.isNotBlank() }.joinToString(" · "))
        // P5: el residente ve si su apartamento está arrendado
        if (!sesion.esAdmin()) {
            viewLifecycleOwner.lifecycleScope.launch {
                val arrendado = runCatching {
                    RetrofitClient.instance.getApartamentoById(sesion.getApartamentoId()).body()?.arrendado
                }.getOrNull() ?: return@launch
                if (_binding == null) return@launch
                val estado = getString(if (arrendado) R.string.cuenta_apto_arrendado else R.string.cuenta_apto_propietario)
                binding.encabezado.ponerInfo(listOf(rol, identidad, estado).filter { it.isNotBlank() }.joinToString(" · "))
            }
        }

        val nav = findNavController()
        binding.filaPerfil.setOnClickListener { nav.navigate(R.id.perfilFragment) }
        binding.filaAjustes.setOnClickListener { nav.navigate(R.id.ajustesFragment) }
        binding.filaPermisos.setOnClickListener { nav.navigate(R.id.permisosFragment) }
        binding.filaLegal.setOnClickListener { nav.navigate(R.id.legalFragment) }
        binding.filaAyuda.setOnClickListener { nav.navigate(R.id.ayudaFragment) }
        val version = getString(R.string.version, BuildConfig.VERSION_NAME)
        binding.filaAcercaDe.subtitulo = version
        binding.filaAcercaDe.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setIcon(R.mipmap.ic_launcher_round)
                .setTitle(R.string.app_name)
                .setMessage(getString(R.string.acerca_de_mensaje, getString(R.string.app_lema), version))
                .setPositiveButton(R.string.btn_cerrar, null)
                .show()
        }
        binding.btnCerrarSesion.setOnClickListener { confirmarCerrarSesion() }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
