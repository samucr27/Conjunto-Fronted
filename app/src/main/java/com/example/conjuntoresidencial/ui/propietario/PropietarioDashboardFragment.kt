package com.example.conjuntoresidencial.ui.propietario

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.FragmentPropietarioDashboardBinding
import com.example.conjuntoresidencial.util.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PropietarioDashboardFragment : Fragment() {

    private var _binding: FragmentPropietarioDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPropietarioDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val aptoId = SessionManager(requireContext()).getApartamentoId()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getApartamentoById(aptoId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val apto = response.body()!!
                        binding.tvBienvenidaPropietario.text = "Torre ${apto.torre} - Apto ${apto.apto}"
                    } else {
                        binding.tvBienvenidaPropietario.text = "Apto #$aptoId"
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    binding.tvBienvenidaPropietario.text = "Apto #$aptoId"
                }
            }
        }

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

        binding.cardMisVehiculos.setOnClickListener {
            findNavController().navigate(R.id.action_propietarioDashboardFragment_to_misVehiculosFragment)
        }
        binding.cardZonasPropietario.setOnClickListener {
            findNavController().navigate(R.id.action_propietarioDashboardFragment_to_zonasComunesPropietarioFragment)
        }
        binding.cardMisReservas.setOnClickListener {
            findNavController().navigate(R.id.action_propietarioDashboardFragment_to_misReservasFragment)
        }
        binding.cardMiCartera.setOnClickListener {
            findNavController().navigate(R.id.action_propietarioDashboardFragment_to_miCarteraFragment)
        }
        binding.cardRegistrarObra.setOnClickListener {
            findNavController().navigate(R.id.action_propietarioDashboardFragment_to_registrarObraFragment)
        }
        binding.cardMisObras.setOnClickListener {
            findNavController().navigate(R.id.action_propietarioDashboardFragment_to_misObrasFragment)
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