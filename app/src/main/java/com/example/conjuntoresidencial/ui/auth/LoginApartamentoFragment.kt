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
import com.example.conjuntoresidencial.databinding.FragmentLoginApartamentoBinding
import com.example.conjuntoresidencial.util.Resource
import com.example.conjuntoresidencial.util.SessionManager
import com.example.conjuntoresidencial.util.configurarBarra
import com.example.conjuntoresidencial.util.exigirTexto
import com.example.conjuntoresidencial.util.limpiarErrorAlEscribir
import android.view.inputmethod.EditorInfo

class LoginApartamentoFragment : Fragment() {

    private var _binding: FragmentLoginApartamentoBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginApartamentoViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginApartamentoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sessionManager = SessionManager(requireContext())
        configurarBarra(binding.barra.toolbar, R.string.login_residente_titulo)

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
            viewModel.loginApartamento(torre, apto, pass)
        }

        viewModel.loginState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressApto.visibility = View.VISIBLE
                    binding.btnLoginApto.isEnabled = false
                }
                is Resource.Success -> {
                    binding.progressApto.visibility = View.GONE
                    resource.data?.let {
                        sessionManager.saveToken(it.token)
                        sessionManager.saveApartamentoId(it.id)
                        sessionManager.saveTorre(it.torre ?: "")
                        sessionManager.saveApto(it.apto ?: "")
                        sessionManager.saveRol(it.rol)
                        findNavController().navigate(R.id.action_loginApartamentoFragment_to_propietarioDashboardFragment)
                    }
                }
                is Resource.Error -> {
                    binding.progressApto.visibility = View.GONE
                    binding.btnLoginApto.isEnabled = true
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