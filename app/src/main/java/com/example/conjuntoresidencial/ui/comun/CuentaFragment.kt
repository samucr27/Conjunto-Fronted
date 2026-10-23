package com.example.conjuntoresidencial.ui.comun

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import com.example.conjuntoresidencial.BuildConfig
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FragmentCuentaBinding
import com.example.conjuntoresidencial.util.SessionManager
import com.example.conjuntoresidencial.util.Tema
import com.example.conjuntoresidencial.util.confirmarCerrarSesion

/**
 * Pestaña Cuenta (perfil y ajustes juntos), para los dos roles. Los datos salen de la
 * sesión guardada al iniciar sesión: no hace peticiones al servidor.
 */
class CuentaFragment : Fragment() {
    private var _binding: FragmentCuentaBinding? = null
    private val binding get() = _binding!!

    // Botón del grupo de tema para cada modo
    private val botonesTema = mapOf(
        Tema.Modo.SISTEMA to R.id.btnTemaSistema,
        Tema.Modo.CLARO to R.id.btnTemaClaro,
        Tema.Modo.OSCURO to R.id.btnTemaOscuro
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCuentaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sesion = SessionManager(requireContext())
        val identidad: String
        if (sesion.esAdmin()) {
            identidad = sesion.getUsuario()
            binding.tvRol.setText(R.string.rol_admin)
            binding.tvEtiquetaIdentidad.setText(R.string.cuenta_usuario)
        } else {
            identidad = getString(R.string.panel_residente_titulo, sesion.getTorre(), sesion.getApto())
            binding.tvRol.setText(R.string.rol_residente)
            binding.tvEtiquetaIdentidad.setText(R.string.cuenta_apartamento)
        }
        binding.tvIdentidad.text = identidad

        // Pestaña de la barra inferior: sin botón para volver
        configurarEncabezado(binding.encabezado, R.string.pestana_cuenta, R.string.cuenta_descripcion, atras = false)
        binding.encabezado.ponerInfo(listOf(binding.tvRol.text, identidad).filter { it.isNotBlank() }.joinToString(" · "))

        // Con letra grande los tres botones no caben en una fila ("Sist…", "Osc…"): van en columna
        if (resources.configuration.fontScale >= 1.3f) {
            binding.grupoTema.orientation = LinearLayout.VERTICAL
            botonesTema.values.forEach { id ->
                binding.grupoTema.findViewById<View>(id).updateLayoutParams<LinearLayout.LayoutParams> {
                    width = LinearLayout.LayoutParams.MATCH_PARENT
                    weight = 0f
                }
            }
        }
        binding.grupoTema.check(botonesTema.getValue(Tema.actual(requireContext())))
        binding.grupoTema.addOnButtonCheckedListener { _, id, marcado ->
            if (!marcado) return@addOnButtonCheckedListener
            val modo = botonesTema.entries.first { it.value == id }.key
            // Cambiar el tema recrea la actividad; la sesión y la pantalla se conservan
            if (modo != Tema.actual(requireContext())) Tema.cambiar(requireContext(), modo)
        }

        binding.tvVersion.text = getString(R.string.version, BuildConfig.VERSION_NAME)
        binding.btnCerrarSesion.setOnClickListener { confirmarCerrarSesion() }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
