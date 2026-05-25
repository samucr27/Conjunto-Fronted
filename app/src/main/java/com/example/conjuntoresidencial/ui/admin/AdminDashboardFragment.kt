package com.example.conjuntoresidencial.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FragmentAdminDashboardBinding
import com.example.conjuntoresidencial.util.SessionManager

class AdminDashboardFragment : Fragment() {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Cerrar sesión")
                        .setMessage("¿Deseas salir?")
                        .setPositiveButton("Sí") { _, _ ->
                            SessionManager(requireContext()).clearSession()
                            findNavController().navigate(R.id.loginSelectorFragment)
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            }
        )

        binding.cardDirectorioAptos.setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboardFragment_to_directorioApartamentosFragment)
        }
        binding.cardControlObras.setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboardFragment_to_controlObrasFragment)
        }
        binding.cardHistorialReservas.setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboardFragment_to_reservasAdminFragment)
        }
        binding.cardZonasAdmin.setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboardFragment_to_zonasComunesAdminFragment)
        }
        binding.cardPersonalObra.setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboardFragment_to_personalObraFragment)
        }
        binding.btnCerrarSesion.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Cerrar sesión")
                .setMessage("¿Deseas salir?")
                .setPositiveButton("Sí") { _, _ ->
                    SessionManager(requireContext()).clearSession()
                    findNavController().navigate(R.id.loginSelectorFragment)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}