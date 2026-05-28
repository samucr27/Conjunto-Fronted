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
import com.example.conjuntoresidencial.util.AdminNotificacionManager
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
                    mostrarDialogSalir()
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
            mostrarDialogSalir()
        }

        // Badge de notificaciones — click abre el dialog de alertas
        binding.btnNotificaciones.setOnClickListener {
            mostrarDialogNotificaciones()
        }
    }

    override fun onResume() {
        super.onResume()
        // Actualizar badge cada vez que el admin regresa al dashboard
        actualizarBadge()
    }

    private fun actualizarBadge() {
        val noLeidas = AdminNotificacionManager.contarNoLeidas(requireContext())
        if (noLeidas > 0) {
            binding.tvBadgeNotificaciones.visibility = View.VISIBLE
            binding.tvBadgeNotificaciones.text = if (noLeidas > 9) "9+" else noLeidas.toString()
        } else {
            binding.tvBadgeNotificaciones.visibility = View.GONE
        }
    }

    private fun mostrarDialogNotificaciones() {
        val notificaciones = AdminNotificacionManager.getNotificaciones(requireContext())

        if (notificaciones.isEmpty()) {
            AlertDialog.Builder(requireContext())
                .setTitle("Actividad reciente")
                .setMessage("No hay notificaciones nuevas.")
                .setPositiveButton("Cerrar", null)
                .show()
            return
        }

        val mensaje = buildString {
            notificaciones.forEach { n ->
                val icono = if (n.tipo == "VEHICULO") "🚗" else "📅"
                appendLine("$icono ${n.mensaje}")
                appendLine("   ${n.fecha}")
                appendLine()
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Actividad reciente (${notificaciones.size})")
            .setMessage(mensaje.trim())
            .setPositiveButton("Marcar todas como leídas") { _, _ ->
                AdminNotificacionManager.marcarTodasLeidas(requireContext())
                actualizarBadge()
            }
            .setNeutralButton("Limpiar todo") { _, _ ->
                AdminNotificacionManager.limpiarTodas(requireContext())
                actualizarBadge()
            }
            .setNegativeButton("Cerrar", null)
            .show()

        // Marcar como leídas al abrir
        AdminNotificacionManager.marcarTodasLeidas(requireContext())
        actualizarBadge()
    }

    private fun mostrarDialogSalir() {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}