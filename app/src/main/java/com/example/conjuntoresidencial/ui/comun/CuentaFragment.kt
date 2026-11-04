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
import com.example.conjuntoresidencial.util.cerrarSesion
import com.example.conjuntoresidencial.util.ConjuntoElegido
import com.example.conjuntoresidencial.ui.auth.cambiarConjunto
import com.example.conjuntoresidencial.ui.auth.elegirConjunto
import androidx.core.view.isVisible
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
            sesion.esPorteria() -> R.string.rol_porteria
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
                    RetrofitClient.comun.getApartamentoById(sesion.getApartamentoId()).body()?.arrendado
                }.getOrNull() ?: return@launch
                if (_binding == null) return@launch
                val estado = getString(if (arrendado) R.string.cuenta_apto_arrendado else R.string.cuenta_apto_propietario)
                binding.encabezado.ponerInfo(listOf(rol, identidad, estado).filter { it.isNotBlank() }.joinToString(" · "))
            }
        }

        val nav = findNavController()
        binding.filaPerfil.setOnClickListener { nav.navigate(R.id.perfilFragment) }
        binding.filaAjustes.setOnClickListener { nav.navigate(R.id.ajustesFragment) }
        binding.filaDispositivos.setOnClickListener { nav.navigate(R.id.dispositivosFragment) }
        binding.filaPermisos.setOnClickListener { nav.navigate(R.id.permisosFragment) }
        binding.filaLegal.setOnClickListener { nav.navigate(R.id.legalFragment) }
        binding.filaAyuda.setOnClickListener { nav.navigate(R.id.ayudaFragment) }
        configurarConjunto()
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

    /**
     * P7: el conjunto de la sesión. Elegir el mismo solo actualiza su ubicación; elegir otro cierra
     * la sesión (la cuenta es de un solo conjunto). Sin conjunto conocido (servidor anterior) no se muestra.
     */
    private fun configurarConjunto() {
        val contexto = requireContext()
        val actual = ConjuntoElegido.actual(contexto)
        binding.filaConjunto.isVisible = actual != null
        binding.divisorConjunto.isVisible = actual != null
        if (actual == null) return
        binding.filaConjunto.subtitulo = actual.nombre
        binding.filaConjunto.setOnClickListener {
            elegirConjunto(null) { nuevo ->
                if (nuevo.id == actual.id) {
                    ConjuntoElegido.guardar(contexto, nuevo)
                    return@elegirConjunto
                }
                MaterialAlertDialogBuilder(contexto)
                    .setTitle(R.string.conjunto_cambiar_titulo)
                    .setMessage(getString(R.string.conjunto_cambiar_mensaje, nuevo.nombre))
                    .setPositiveButton(R.string.conjunto_cambiar_confirmar) { _, _ ->
                        // Ya sin sesión: la huella guardada se quita solo en este celular
                        cerrarSesion { cambiarConjunto(contexto.applicationContext, nuevo) }
                    }
                    .setNegativeButton(R.string.btn_cancelar, null)
                    .show()
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
