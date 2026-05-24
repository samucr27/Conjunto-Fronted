package com.example.conjuntoresidencial.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FragmentLoginSelectorBinding

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

        binding.btnSoyAdmin.setOnClickListener {
            findNavController().navigate(R.id.action_loginSelectorFragment_to_loginAdminFragment)
        }

        binding.btnSoyResidente.setOnClickListener {
            findNavController().navigate(R.id.action_loginSelectorFragment_to_loginApartamentoFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}