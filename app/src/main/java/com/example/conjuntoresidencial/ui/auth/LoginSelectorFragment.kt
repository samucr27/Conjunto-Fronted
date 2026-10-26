package com.example.conjuntoresidencial.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.BuildConfig
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.FragmentLoginSelectorBinding
import com.example.conjuntoresidencial.seguridad.AccesoBiometrico
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.controlesFlotantes
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import kotlinx.coroutines.launch

/**
 * Pantalla de entrada: administrador o residente. Si la huella está activada, ofrece entrar
 * con ella (y la pide sola una vez al abrir la app, al terminar el logo animado).
 */
class LoginSelectorFragment : Fragment() {

    private var _binding: FragmentLoginSelectorBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginSelectorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Encabezado One UI con el logo: nombre de la app y lema; sin botones (es la primera pantalla)
        configurarEncabezado(binding.encabezado, R.string.app_name, atras = false)
        binding.encabezado.imgLogo.visibility = View.VISIBLE
        binding.encabezado.ponerInfo(getString(R.string.app_lema))
        binding.tvVersion.text = getString(R.string.version, BuildConfig.VERSION_NAME)

        binding.btnSoyAdmin.setOnClickListener {
            findNavController().navigate(R.id.action_loginSelectorFragment_to_loginAdminFragment)
        }

        binding.btnSoyResidente.setOnClickListener {
            findNavController().navigate(R.id.action_loginSelectorFragment_to_loginApartamentoFragment)
        }

        // Cada app tiene su entrada: la de administración o la de residentes (P5)
        val esAppAdmin = resources.getBoolean(R.bool.app_admin)
        binding.btnSoyAdmin.visibility = if (esAppAdmin) View.VISIBLE else View.GONE
        binding.btnSoyResidente.visibility = if (esAppAdmin) View.GONE else View.VISIBLE

        binding.btnEntrarHuella.setOnClickListener { entrarConHuella() }
        mostrarHuella()
        if (AccesoBiometrico.activado(requireContext()) && !huellaOfrecida) {
            huellaOfrecida = true
            controlesFlotantes?.alTerminarCarga { if (_binding != null) entrarConHuella() }
        }
    }

    private fun mostrarHuella() {
        val cuenta = AccesoBiometrico.cuentaGuardada(requireContext())
        binding.grupoHuella.visibility = if (cuenta != null) View.VISIBLE else View.GONE
        binding.tvCuentaHuella.text = cuenta
    }

    private fun entrarConHuella() {
        AccesoBiometrico.entrar(this, alObtener = ::iniciarCon, alFallar = { mensaje ->
            if (_binding == null) return@entrar
            mensaje?.let { aviso(it).show() }
            mostrarHuella()
        })
    }

    /** La huella entregó las credenciales: se inicia sesión con ellas como si se escribieran. */
    private fun iniciarCon(credenciales: AccesoBiometrico.Credenciales) {
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
                        val cuenta = credenciales.cuenta(requireContext())
                        AccesoBiometrico.desactivar(requireContext())
                        aviso(getString(R.string.biometria_contrasena_cambio, cuenta)).show()
                        mostrarHuella()
                    }
                    else -> aviso(RetrofitClient.mensajeDeError(respuesta, getString(R.string.biometria_error_datos))).show()
                }
            } catch (e: Exception) {
                aviso(RetrofitClient.mensajeDeExcepcion(e)).show()
            }
            cargando(false)
        }
    }

    private fun cargando(si: Boolean) {
        binding.progresoHuella.visibility = if (si) View.VISIBLE else View.GONE
        listOf(binding.btnEntrarHuella, binding.btnSoyAdmin, binding.btnSoyResidente).forEach { it.isEnabled = !si }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        // La huella se pide sola una vez por apertura de la app; después, con el botón
        var huellaOfrecida = false
    }
}