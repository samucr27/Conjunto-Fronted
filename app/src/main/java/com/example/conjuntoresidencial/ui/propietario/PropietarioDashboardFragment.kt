package com.example.conjuntoresidencial.ui.propietario

import androidx.lifecycle.lifecycleScope
import com.example.conjuntoresidencial.api.RetrofitClient
import kotlinx.coroutines.launch
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FragmentPropietarioDashboardBinding
import com.example.conjuntoresidencial.ui.comun.configurar
import com.example.conjuntoresidencial.util.SessionManager
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.ponerInfo

class PropietarioDashboardFragment : Fragment() {

    private var _binding: FragmentPropietarioDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPropietarioDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Torre y apartamento ya están en la sesión desde el login: no hace falta pedirlos al servidor
        val sesion = SessionManager(requireContext())
        configurarEncabezado(
            binding.encabezado, R.string.pestana_herramientas,
            if (sesion.getRol() == "ARRENDATARIO") R.string.panel_arrendatario_subtitulo else R.string.panel_residente_subtitulo,
            atras = false
        )
        binding.encabezado.ponerInfo(getString(R.string.panel_residente_titulo, sesion.getTorre(), sesion.getApto()))

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
        binding.cardMiHogar.configurar(R.drawable.ic_people, R.string.menu_mi_hogar, R.string.menu_mi_hogar_sub) {
            nav.navigate(R.id.miHogarFragment)
        }
        // P5: el chat lo tienen el propietario y el arrendatario (cada uno el suyo)
        binding.cardChat.configurar(R.drawable.ic_chat, R.string.chat_tarjeta, R.string.chat_tarjeta_sub) {
            findNavController().navigate(R.id.chatFragment)
        }
        binding.cardArrendatario.configurar(R.drawable.ic_key, R.string.arrendatario_titulo, R.string.arrendatario_tarjeta_sub) {
            nav.navigate(R.id.arrendatarioFragment)
        }
        mostrarSegunRol(sesion.esPropietario(), arrendado = false)
        // Si está arrendado, el propietario solo conserva lo suyo (lo dice el backend)
        if (sesion.esPropietario()) {
            viewLifecycleOwner.lifecycleScope.launch {
                val arrendado = runCatching {
                    RetrofitClient.instance.getApartamentoById(sesion.getApartamentoId()).body()?.arrendado
                }.getOrNull() == true
                if (_binding != null) mostrarSegunRol(true, arrendado)
            }
        }
    }

    /**
     * El arrendatario no ve lo que solo maneja el propietario (cartera, obras, su arrendatario);
     * el propietario de un apartamento arrendado no ve lo de quien vive ahí.
     */
    private fun mostrarSegunRol(propietario: Boolean, arrendado: Boolean) {
        val soloQuienVive = listOf(binding.cardMisVehiculos, binding.cardZonasPropietario, binding.cardMisReservas, binding.cardMiHogar)
        val soloPropietario = listOf(binding.cardMiCartera, binding.cardRegistrarObra, binding.cardMisObras, binding.cardArrendatario)
        soloQuienVive.forEach { it.root.visibility = if (propietario && arrendado) View.GONE else View.VISIBLE }
        soloPropietario.forEach { it.root.visibility = if (propietario) View.VISIBLE else View.GONE }
        binding.tvAvisoArrendado.visibility = if (propietario && arrendado) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
