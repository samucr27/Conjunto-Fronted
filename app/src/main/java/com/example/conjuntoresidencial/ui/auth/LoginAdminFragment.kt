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

        binding.btnIngresarAdmin.setOnClickListener {
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
                    sessionManager.saveRol("ADMIN")
                    findNavController().navigate(R.id.action_loginAdminFragment_to_adminDashboardFragment)
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