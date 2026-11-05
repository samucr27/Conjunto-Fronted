package com.example.conjuntoresidencial.ui.plataforma

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.api.instance
import com.example.conjuntoresidencial.databinding.FragmentLoginPlataformaBinding
import com.example.conjuntoresidencial.model.LoginPlataforma
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.util.SessionManager
import com.example.conjuntoresidencial.util.exigirTexto
import com.example.conjuntoresidencial.util.irAPasoPendiente
import com.example.conjuntoresidencial.util.limpiarErrorAlEscribir
import kotlinx.coroutines.launch

/** P7: login de la plataforma. Sin conjunto (no es de ninguno) ni huella (es una cuenta con más poder). */
class LoginPlataformaFragment : Fragment() {

    private var _binding: FragmentLoginPlataformaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginPlataformaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // P8 bloque 2: la primera vez, antes del login va la bienvenida
        if (irAPasoPendiente()) return
        configurarEncabezado(binding.encabezado, R.string.plataforma_login_titulo, atras = false)
        binding.tilUsuario.limpiarErrorAlEscribir()
        binding.tilContrasena.limpiarErrorAlEscribir()
        binding.etContrasena.setOnEditorActionListener { _, accion, _ ->
            (accion == EditorInfo.IME_ACTION_DONE).also { if (it) binding.btnIngresar.performClick() }
        }
        binding.btnIngresar.setOnClickListener { ingresar() }
    }

    private fun ingresar() {
        val completos = listOf(
            binding.tilUsuario.exigirTexto(getString(R.string.login_falta_usuario)),
            binding.tilContrasena.exigirTexto(getString(R.string.login_falta_contrasena))
        ).all { it }
        if (!completos) return
        val datos = LoginPlataforma(binding.etUsuario.text.toString().trim(), binding.etContrasena.text.toString())
        cargando(true)
        viewLifecycleOwner.lifecycleScope.launch {
            val error = try {
                val r = RetrofitClient.instance.login(datos)
                val respuesta = r.body()
                if (r.isSuccessful && respuesta != null) {
                    SessionManager(requireContext()).guardar(respuesta)
                    findNavController().navigate(R.id.panelPlataformaFragment, null,
                        NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build())
                    return@launch
                }
                RetrofitClient.mensajeDeError(r, getString(R.string.plataforma_login_error))
            } catch (e: Exception) {
                RetrofitClient.mensajeDeExcepcion(e)
            }
            if (_binding == null) return@launch
            cargando(false)
            aviso(error).show()
        }
    }

    private fun cargando(si: Boolean) {
        binding.progreso.visibility = if (si) View.VISIBLE else View.GONE
        binding.btnIngresar.isEnabled = !si
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
