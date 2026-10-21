package com.example.conjuntoresidencial.ui.propietario

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FragmentPropietarioDashboardBinding
import com.example.conjuntoresidencial.ui.comun.configurar
import com.example.conjuntoresidencial.util.SessionManager
import com.example.conjuntoresidencial.util.configurarBarra
import com.example.conjuntoresidencial.util.confirmarCerrarSesion

class PropietarioDashboardFragment : Fragment() {

    private var _binding: FragmentPropietarioDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPropietarioDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarBarra(
            binding.barra.toolbar, R.string.panel_residente_subtitulo,
            atras = false, menu = R.menu.menu_panel_propietario
        ) { item ->
            if (item == R.id.accion_cerrar_sesion) { confirmarCerrarSesion(); true } else false
        }
        // Torre y apartamento ya están en la sesión desde el login: no hace falta pedirlos al servidor
        val sesion = SessionManager(requireContext())
        binding.barra.toolbar.title = getString(R.string.panel_residente_titulo, sesion.getTorre(), sesion.getApto())
        binding.barra.toolbar.subtitle = getString(R.string.panel_residente_subtitulo)

        // "Atrás" en el panel pregunta si se quiere cerrar sesión
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = confirmarCerrarSesion()
            })

        val nav = findNavController()
        binding.cardMisVehiculos.configurar(R.drawable.ic_car, R.string.menu_mis_vehiculos, R.string.menu_mis_vehiculos_sub) {
            nav.navigate(R.id.action_propietarioDashboardFragment_to_misVehiculosFragment)
        }
        binding.cardZonasPropietario.configurar(R.drawable.ic_place, R.string.menu_zonas, R.string.menu_zonas_res_sub) {
            nav.navigate(R.id.action_propietarioDashboardFragment_to_zonasComunesPropietarioFragment)
        }
        binding.cardMisReservas.configurar(R.drawable.ic_event, R.string.menu_mis_reservas, R.string.menu_mis_reservas_sub) {
            nav.navigate(R.id.action_propietarioDashboardFragment_to_misReservasFragment)
        }
        binding.cardMiCartera.configurar(R.drawable.ic_wallet, R.string.menu_mi_cartera, R.string.menu_mi_cartera_sub) {
            nav.navigate(R.id.action_propietarioDashboardFragment_to_miCarteraFragment)
        }
        binding.cardRegistrarObra.configurar(R.drawable.ic_add, R.string.menu_registrar_obra, R.string.menu_registrar_obra_sub) {
            nav.navigate(R.id.action_propietarioDashboardFragment_to_registrarObraFragment)
        }
        binding.cardMisObras.configurar(R.drawable.ic_build, R.string.menu_mis_obras, R.string.menu_mis_obras_sub) {
            nav.navigate(R.id.action_propietarioDashboardFragment_to_misObrasFragment)
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
