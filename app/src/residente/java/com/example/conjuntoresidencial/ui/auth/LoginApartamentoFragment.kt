package com.example.conjuntoresidencial.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FragmentLoginApartamentoBinding
import com.example.conjuntoresidencial.seguridad.AccesoBiometrico
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.util.Resource
import com.example.conjuntoresidencial.util.exigirTexto
import com.example.conjuntoresidencial.util.limpiarErrorAlEscribir
import com.google.android.material.snackbar.Snackbar

/**
 * Acceso del residente (P5): arriba se elige propietario o arrendatario; se entra con la torre,
 * el apartamento y la contraseña personal. Abajo, el registro para quien aún no tiene cuenta.
 */
class LoginApartamentoFragment : Fragment() {

    private var _binding: FragmentLoginApartamentoBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginApartamentoViewModel by viewModels()

    // Lo que se escribió en el último intento: se cifra si el usuario activa la huella
    private var credenciales: AccesoBiometrico.Credenciales? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginApartamentoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Sin flecha cuando es la primera pantalla (P5: sin el selector de entrada)
        configurarEncabezado(binding.encabezado, R.string.login_residente_titulo,
            atras = androidx.navigation.fragment.NavHostFragment.findNavController(this).previousBackStackEntry != null)

        // "Listo" en el teclado del campo de contraseña también inicia sesión
        binding.etPassApto.setOnEditorActionListener { _, accion, _ ->
            (accion == EditorInfo.IME_ACTION_DONE).also { if (it) binding.btnLoginApto.performClick() }
        }

        listOf(binding.tilTorreApto, binding.tilNumApto, binding.tilPassApto).forEach { it.limpiarErrorAlEscribir() }
        binding.btnLoginApto.setOnClickListener {
            val completos = listOf(
                binding.tilTorreApto.exigirTexto(getString(R.string.login_falta_torre)),
                binding.tilNumApto.exigirTexto(getString(R.string.login_falta_apartamento)),
                binding.tilPassApto.exigirTexto(getString(R.string.login_falta_contrasena))
            ).all { it }
            if (!completos) return@setOnClickListener
            val torre = binding.etTorreApto.text.toString().trim()
            val apto = binding.etNumApto.text.toString().trim()
            val pass = binding.etPassApto.text.toString().trim()
            val rol = if (binding.grupoModo.checkedButtonId == R.id.btnArrendatario) "ARRENDATARIO" else "PROPIETARIO"
            credenciales = AccesoBiometrico.Credenciales(rol, torre = torre, apto = apto, contrasena = pass)
            viewModel.loginTitular(torre, apto, rol, pass)
        }
        binding.btnRegistrarse.setOnClickListener { findNavController().navigate(R.id.registroFragment) }
        ofrecerHuella(binding.btnHuella) { si ->
            binding.progressApto.visibility = if (si) View.VISIBLE else View.GONE
            listOf(binding.btnLoginApto, binding.btnHuella, binding.btnRegistrarse).forEach { it.isEnabled = !si }
        }

        viewModel.loginState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressApto.visibility = View.VISIBLE
                    binding.btnLoginApto.isEnabled = false
                }
                is Resource.Success -> {
                    binding.progressApto.visibility = View.GONE
                    // Guarda la sesión, ofrece entrar con huella la próxima vez y abre Inicio
                    resource.data?.let { completarInicioSesion(it, credenciales) }
                }
                is Resource.Error -> {
                    binding.progressApto.visibility = View.GONE
                    binding.btnLoginApto.isEnabled = true
                    aviso(resource.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
