package com.example.conjuntoresidencial.ui.admin

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
import com.example.conjuntoresidencial.ui.comun.configurar
import com.example.conjuntoresidencial.util.Formatos
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.util.confirmarCerrarSesion
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class AdminDashboardFragment : Fragment() {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!
    private var insignia: BadgeDrawable? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarEncabezado(
            binding.encabezado, R.string.panel_admin_titulo, R.string.panel_admin_subtitulo,
            atras = false, menu = R.menu.menu_panel
        ) { item ->
            (item == R.id.accion_cerrar_sesion).also { if (it) confirmarCerrarSesion() }
        }
        // Campana: botón redondo junto a "⋮", con la insignia de no leídas
        binding.encabezado.btnAccion.apply {
            visibility = View.VISIBLE
            setIconResource(R.drawable.ic_notifications)
            contentDescription = getString(R.string.notificaciones)
            setOnClickListener { mostrarDialogNotificaciones() }
        }
        crearInsignia()

        // "Atrás" en el panel pregunta si se quiere cerrar sesión
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = confirmarCerrarSesion()
            })

        val nav = findNavController()
        binding.cardDirectorioAptos.configurar(R.drawable.ic_apartment, R.string.menu_directorio, R.string.menu_directorio_sub) {
            nav.navigate(R.id.action_adminDashboardFragment_to_directorioApartamentosFragment)
        }
        binding.cardControlObras.configurar(R.drawable.ic_build, R.string.menu_obras, R.string.menu_obras_sub) {
            nav.navigate(R.id.action_adminDashboardFragment_to_controlObrasFragment)
        }
        binding.cardHistorialReservas.configurar(R.drawable.ic_event, R.string.menu_reservas, R.string.menu_reservas_sub) {
            nav.navigate(R.id.action_adminDashboardFragment_to_reservasAdminFragment)
        }
        binding.cardZonasAdmin.configurar(R.drawable.ic_place, R.string.menu_zonas, R.string.menu_zonas_sub) {
            nav.navigate(R.id.action_adminDashboardFragment_to_zonasComunesAdminFragment)
        }
        binding.cardPersonalObra.configurar(R.drawable.ic_people, R.string.menu_personal, R.string.menu_personal_sub) {
            nav.navigate(R.id.action_adminDashboardFragment_to_personalObraFragment)
        }
        binding.cardCarteraAdmin.configurar(R.drawable.ic_wallet, R.string.menu_cartera, R.string.menu_cartera_sub) {
            nav.navigate(R.id.action_adminDashboardFragment_to_carteraAdminFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        actualizarInsignia()
    }

    /**
     * Insignia de Material sobre la campana del encabezado. Pegada a una vista (no a un menú de
     * Toolbar), Material no toca la descripción del botón: TalkBack lee la que pone mostrarInsignia.
     */
    @androidx.annotation.OptIn(ExperimentalBadgeUtils::class)
    private fun crearInsignia() {
        val nueva = BadgeDrawable.create(requireContext()).apply {
            isVisible = false
            maxCharacterCount = 2 // "9+"
        }
        // Hay que esperar a que el botón tenga tamaño para ubicar la insignia en su esquina
        binding.encabezado.btnAccion.post {
            _binding?.let { BadgeUtils.attachBadgeDrawable(nueva, it.encabezado.btnAccion) }
        }
        insignia = nueva
    }

    private fun actualizarInsignia() {
        viewLifecycleOwner.lifecycleScope.launch {
            val noLeidas = try {
                // Solo el número: no hace falta descargar la lista cada vez que se vuelve al panel
                val response = RetrofitClient.instance.getNotificacionesNoLeidas()
                if (response.isSuccessful) response.body()?.total?.toInt() ?: 0 else 0
            } catch (_: Exception) {
                0 // sin conexión: no se muestra el contador
            }
            mostrarInsignia(noLeidas)
        }
    }

    private fun mostrarInsignia(noLeidas: Int) {
        val badge = insignia ?: return
        badge.number = noLeidas
        badge.isVisible = noLeidas > 0
        // TalkBack lee "Notificaciones, 2 sin leer"
        binding.encabezado.btnAccion.contentDescription =
            if (noLeidas > 0) getString(R.string.notificaciones_no_leidas, noLeidas)
            else getString(R.string.notificaciones)
        // Resumen del encabezado: "2 notificaciones sin leer" o "Sin notificaciones nuevas"
        binding.encabezado.ponerInfo(
            if (noLeidas > 0) resources.getQuantityString(R.plurals.resumen_notificaciones, noLeidas, noLeidas)
            else getString(R.string.resumen_sin_notificaciones)
        )
    }

    private fun mostrarDialogNotificaciones() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getNotificaciones()
                if (response.isSuccessful) {
                    mostrarDialogNotificaciones(response.body().orEmpty())
                } else {
                    Snackbar.make(binding.root,
                        RetrofitClient.mensajeDeError(response, getString(R.string.actividad_error_cargar)),
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
                .setTitle(R.string.actividad_reciente)
                .setMessage(R.string.actividad_vacia)
                .setPositiveButton(R.string.cerrar, null)
                .show()
            return
        }

        val mensaje = buildString {
            notificaciones.forEach { n ->
                val icono = if (n.tipo == "VEHICULO") "🚗" else "📅"
                appendLine("$icono ${n.mensaje}")
                // "2026-10-21T00:37:12" -> "21/10/2026 · 12:37 a. m."
                appendLine("   ${Formatos.fechaHora(n.fecha.take(10), n.fecha.drop(11))}")
                appendLine()
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.actividad_reciente_n, notificaciones.size))
            .setMessage(mensaje.trim())
            .setPositiveButton(R.string.cerrar, null)
            .setNeutralButton(R.string.actividad_limpiar) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val ok = try { RetrofitClient.instance.eliminarNotificaciones().isSuccessful } catch (_: Exception) { false }
                    if (!ok) Snackbar.make(binding.root, R.string.actividad_error_limpiar, Snackbar.LENGTH_LONG).show()
                    actualizarInsignia()
                }
            }
            .show()

        // Abrir la lista cuenta como leerla
        if (notificaciones.any { !it.leida }) {
            viewLifecycleOwner.lifecycleScope.launch {
                // Si falla no se avisa: el contador se recarga abajo y sigue mostrando las no leídas
                try { RetrofitClient.instance.marcarNotificacionesLeidas() } catch (_: Exception) {}
                actualizarInsignia()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        insignia = null
        _binding = null
    }
}
