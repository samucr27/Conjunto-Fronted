package com.example.conjuntoresidencial.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FragmentLoginAdminBinding
import com.example.conjuntoresidencial.util.Resource
import com.example.conjuntoresidencial.util.SessionManager
import com.example.conjuntoresidencial.util.configurarBarra
import com.example.conjuntoresidencial.util.exigirTexto
import com.example.conjuntoresidencial.util.limpiarErrorAlEscribir
import android.view.inputmethod.EditorInfo

class LoginAdminFragment : Fragment() {

    private var _binding: FragmentLoginAdminBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginAdminViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sessionManager = SessionManager(requireContext())
        configurarBarra(binding.barra.toolbar, R.string.login_admin_titulo)

        // "Listo" en el teclado del campo de contraseña también inicia sesión
        binding.etPassAdmin.setOnEditorActionListener { _, accion, _ ->
            (accion == EditorInfo.IME_ACTION_DONE).also { if (it) binding.btnIngresarAdmin.performClick() }
        }

        binding.tilUsuarioAdmin.limpiarErrorAlEscribir()
        binding.tilPassAdmin.limpiarErrorAlEscribir()
        binding.btnIngresarAdmin.setOnClickListener {
            val completos = listOf(
                binding.tilUsuarioAdmin.exigirTexto(getString(R.string.login_falta_usuario)),
                binding.tilPassAdmin.exigirTexto(getString(R.string.login_falta_contrasena))
            ).all { it }
            if (!completos) return@setOnClickListener
            val user = binding.etUsuarioAdmin.text.toString().trim()
            val pass = binding.etPassAdmin.text.toString().trim()
            viewModel.loginAdmin(user, pass)
        }

        viewModel.loginState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressAdmin.visibility = View.VISIBLE
                    binding.btnIngresarAdmin.isEnabled = false
                }
                is Resource.Success -> {
                    binding.progressAdmin.visibility = View.GONE
                    resource.data?.let {
                        sessionManager.saveToken(it.token)
                        sessionManager.saveRol(it.rol)
                        findNavController().navigate(R.id.action_loginAdminFragment_to_adminDashboardFragment)
                    }
                }
                is Resource.Error -> {
                    binding.progressAdmin.visibility = View.GONE
                    binding.btnIngresarAdmin.isEnabled = true
                    Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}