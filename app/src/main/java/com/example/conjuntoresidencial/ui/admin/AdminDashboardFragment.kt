package com.example.conjuntoresidencial.ui.admin

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.FragmentAdminDashboardBinding
import com.example.conjuntoresidencial.model.NotificacionDTO
import com.example.conjuntoresidencial.util.SessionManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

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

        // Enlaces de Navegación del Dashboard
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

        // CORRECCIÓN: Evento de la nueva pantalla de Cartera General
        binding.cardCarteraAdmin.setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboardFragment_to_carteraAdminFragment)
        }

        binding.btnCerrarSesion.setOnClickListener {
            mostrarDialogSalir()
        }

        binding.btnNotificaciones.setOnClickListener {
            mostrarDialogNotificaciones()
        }
    }

    override fun onResume() {
        super.onResume()
        actualizarBadge()
    }

    private fun actualizarBadge() {
        viewLifecycleOwner.lifecycleScope.launch {
            val noLeidas = try {
                // Solo el número: no hace falta descargar la lista cada vez que se vuelve al panel
                val response = RetrofitClient.instance.getNotificacionesNoLeidas()
                if (response.isSuccessful) response.body()?.total?.toInt() ?: 0 else 0
            } catch (_: Exception) {
                0 // sin conexión: no se muestra el contador
            }
            mostrarBadge(noLeidas)
        }
    }

    private fun mostrarBadge(noLeidas: Int) {
        if (noLeidas > 0) {
            binding.tvBadgeNotificaciones.visibility = View.VISIBLE
            binding.tvBadgeNotificaciones.text = if (noLeidas > 9) "9+" else noLeidas.toString()
        } else {
            binding.tvBadgeNotificaciones.visibility = View.GONE
        }
    }

    private fun mostrarDialogNotificaciones() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getNotificaciones()
                if (response.isSuccessful) {
                    mostrarDialogNotificaciones(response.body().orEmpty())
                } else {
                    Snackbar.make(binding.root,
                        RetrofitClient.mensajeDeError(response, "No se pudieron cargar las notificaciones"),
                        Snackbar.LENGTH_LONG).show()
                }
            } catch (_: Exception) {
                Snackbar.make(binding.root, R.string.error_red, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun mostrarDialogNotificaciones(notificaciones: List<NotificacionDTO>) {
        if (notificaciones.isEmpty()) {
            MaterialAlertDialogBuilder(requireContext())
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
                appendLine("   ${formatearFecha(n.fecha)}")
                appendLine()
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Actividad reciente (${notificaciones.size})")
            .setMessage(mensaje.trim())
            .setPositiveButton("Marcar todas como leídas", null) // ya se marcan al abrir
            .setNeutralButton("Limpiar todo") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val ok = try { RetrofitClient.instance.eliminarNotificaciones().isSuccessful } catch (_: Exception) { false }
                    if (!ok) Snackbar.make(binding.root, "No se pudieron limpiar las notificaciones", Snackbar.LENGTH_LONG).show()
                    actualizarBadge()
                }
            }
            .setNegativeButton("Cerrar", null)
            .show()

        // Abrir la lista cuenta como leerla
        if (notificaciones.any { !it.leida }) {
            viewLifecycleOwner.lifecycleScope.launch {
                // Si falla no se avisa: el contador se recarga abajo y sigue mostrando las no leídas
                try { RetrofitClient.instance.marcarNotificacionesLeidas() } catch (_: Exception) {}
                actualizarBadge()
            }
        }
    }

    // "2026-10-20T20:15:00" -> "20/10/2026 20:15" (sin java.time: minSdk 24)
    private fun formatearFecha(iso: String): String = try {
        val (fecha, hora) = iso.split("T")
        val (anio, mes, dia) = fecha.split("-")
        "$dia/$mes/$anio ${hora.take(5)}"
    } catch (_: Exception) {
        iso
    }

    private fun mostrarDialogSalir() {
        MaterialAlertDialogBuilder(requireContext())
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