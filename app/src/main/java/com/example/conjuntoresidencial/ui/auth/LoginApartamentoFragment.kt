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

        binding.btnLoginApto.setOnClickListener {
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
                        sessionManager.saveApartamentoId(it.id)
                        sessionManager.saveRol("PROPIETARIO")
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