package com.example.conjuntoresidencial.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.util.SessionManager
import com.example.conjuntoresidencial.ui.residentes.ResidentesFragment
import com.example.conjuntoresidencial.databinding.FragmentAdminDashboardBinding
import com.example.conjuntoresidencial.ui.comun.NotificacionesViewModel
import com.example.conjuntoresidencial.ui.comun.configurar
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.util.confirmarCerrarSesion

class AdminDashboardFragment : Fragment() {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!
    // Compartido con MainActivity, que lo actualiza al mostrar la barra inferior
    private val notificaciones: NotificacionesViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val contador = SessionManager(requireContext()).esContador()
        configurarEncabezado(
            binding.encabezado, R.string.panel_admin_titulo,
            if (contador) R.string.panel_contador_subtitulo else R.string.panel_admin_subtitulo,
            atras = false, menu = R.menu.menu_panel
        ) { item ->
            (item == R.id.accion_cerrar_sesion).also { if (it) confirmarCerrarSesion() }
        }
        // Las notificaciones están en la barra inferior; aquí, el resumen de las no leídas
        notificaciones.noLeidas.observe(viewLifecycleOwner) { noLeidas ->
            binding.encabezado.ponerInfo(
                if (noLeidas > 0) resources.getQuantityString(R.plurals.resumen_notificaciones, noLeidas, noLeidas)
                else getString(R.string.resumen_sin_notificaciones)
            )
        }

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
        binding.cardVehiculosAdmin.configurar(R.drawable.ic_car, R.string.menu_vehiculos_admin, R.string.menu_vehiculos_admin_sub) {
            nav.navigate(R.id.vehiculosAdminFragment)
        }
        binding.cardFacturacion.configurar(R.drawable.ic_receipt, R.string.menu_facturacion, R.string.menu_facturacion_sub) {
            nav.navigate(R.id.cuotasFragment)
        }
        binding.cardEquipo.configurar(R.drawable.ic_people, R.string.menu_equipo, R.string.menu_equipo_sub) {
            nav.navigate(R.id.equipoFragment)
        }
        // El contador solo ve la cartera y la facturación (el backend tampoco le deja lo demás)
        if (contador) {
            listOf(binding.cardDirectorioAptos, binding.cardControlObras, binding.cardHistorialReservas, binding.cardZonasAdmin,
                binding.cardPersonalObra, binding.cardVehiculosAdmin, binding.cardResidentesPendientes, binding.cardEquipo)
                .forEach { it.root.visibility = View.GONE }
        }
        binding.cardResidentesPendientes.configurar(R.drawable.ic_people, R.string.menu_residentes_pendientes,
            R.string.menu_residentes_pendientes_sub) {
            nav.navigate(R.id.residentesFragment, ResidentesFragment.pendientes())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
