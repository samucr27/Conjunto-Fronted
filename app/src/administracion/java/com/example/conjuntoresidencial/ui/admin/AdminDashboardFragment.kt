package com.example.conjuntoresidencial.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        val sesion = SessionManager(requireContext())
        val contador = sesion.esContador()
        configurarEncabezado(
            binding.encabezado, R.string.pestana_herramientas,
            if (contador) R.string.panel_contador_subtitulo else R.string.herramientas_admin_subtitulo,
            atras = false
        )
        // Las notificaciones están en la barra inferior; aquí, el resumen de las no leídas
        notificaciones.noLeidas.observe(viewLifecycleOwner) { noLeidas ->
            binding.encabezado.ponerInfo(
                if (noLeidas > 0) resources.getQuantityString(R.plurals.resumen_notificaciones, noLeidas, noLeidas)
                else getString(R.string.resumen_sin_notificaciones)
            )
        }


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
        // P5: la administración ve en la cartera estadísticas y lo que mandó el contador; el contador, cada apartamento
        binding.cardCarteraAdmin.configurar(R.drawable.ic_wallet, R.string.menu_cartera,
            if (contador) R.string.menu_cartera_sub else R.string.cartera_resumen_tarjeta_sub) {
            if (contador) nav.navigate(R.id.action_adminDashboardFragment_to_carteraAdminFragment)
            else nav.navigate(R.id.carteraResumenFragment)
        }
        binding.cardParqueaderos.configurar(R.drawable.ic_car, R.string.parqueaderos_titulo, R.string.parqueaderos_tarjeta_sub) {
            nav.navigate(R.id.parqueaderosFragment)
        }
        binding.cardVehiculosAdmin.configurar(R.drawable.ic_car, R.string.menu_vehiculos_admin, R.string.menu_vehiculos_admin_sub) {
            nav.navigate(R.id.vehiculosAdminFragment)
        }
        binding.cardFacturacion.configurar(R.drawable.ic_receipt, R.string.menu_facturacion, R.string.menu_facturacion_sub) {
            nav.navigate(R.id.cuotasFragment)
        }
        binding.cardMensajes.configurar(R.drawable.ic_chat, R.string.chat_mensajes, R.string.chat_mensajes_sub) {
            nav.navigate(R.id.conversacionesFragment)
        }
        binding.cardEquipo.configurar(R.drawable.ic_people, R.string.menu_equipo, R.string.menu_equipo_sub) {
            nav.navigate(R.id.equipoFragment)
        }
        binding.cardPersonalConjunto.configurar(R.drawable.ic_people, R.string.menu_personal_conjunto, R.string.menu_personal_conjunto_sub) {
            nav.navigate(R.id.personalConjuntoFragment)
        }
        // El contador solo ve la cartera y la facturación (el backend tampoco le deja lo demás)
        if (contador) {
            quitar(binding.cardPersonalConjunto.root)
            listOf(binding.cardDirectorioAptos, binding.cardControlObras, binding.cardHistorialReservas, binding.cardZonasAdmin,
                binding.cardPersonalObra, binding.cardVehiculosAdmin, binding.cardParqueaderos, binding.cardResidentesPendientes, binding.cardEquipo, binding.cardMensajes)
                .forEach { quitar(it.root) }
        } else {
            // La facturación es del contador; el equipo, solo del administrador principal
            quitar(binding.cardFacturacion.root)
            if (!sesion.esPrincipal()) quitar(binding.cardEquipo.root)
        }
        binding.cardResidentesPendientes.configurar(R.drawable.ic_people, R.string.menu_residentes_pendientes,
            R.string.menu_residentes_pendientes_sub) {
            nav.navigate(R.id.residentesFragment, ResidentesFragment.pendientes())
        }
    }

    /** GridLayout deja vacía la celda de una tarjeta GONE: las que no aplican se quitan del todo. */
    private fun quitar(tarjeta: View) {
        (tarjeta.parent as? ViewGroup)?.removeView(tarjeta)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
