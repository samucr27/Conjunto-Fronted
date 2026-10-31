package com.example.conjuntoresidencial.ui.propietario

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FragmentZonasComunesPropietarioBinding
import com.example.conjuntoresidencial.ui.comun.ZonaAdapter
import com.example.conjuntoresidencial.ui.comun.mostrarRecurso
import com.example.conjuntoresidencial.ui.comun.accionesDeLista
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.ui.comun.resumen
import com.example.conjuntoresidencial.ui.reservas.ReservarZonaFragment

/** Zonas comunes del residente: "Reservar" abre el calendario de la zona (ReservarZonaFragment). */
class ZonasComunesPropietarioFragment : Fragment() {
    private var _binding: FragmentZonasComunesPropietarioBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ZonasPropietarioViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentZonasComunesPropietarioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(
            binding.encabezado, R.string.menu_zonas, R.string.zonas_res_subtitulo, menu = R.menu.menu_lista,
            alElegir = accionesDeLista(binding.swipe) { viewModel.fetchZonas() }
        )

        // Con callback: las zonas reservables muestran el botón "Reservar"
        val adaptador = ZonaAdapter(onReservar = { zona ->
            findNavController().navigate(R.id.reservarZonaFragment, ReservarZonaFragment.argumentos(zona.id!!, zona.nombre))
        })
        binding.rvZonasPropietario.layoutManager = LinearLayoutManager(requireContext())
        binding.rvZonasPropietario.adapter = adaptador
        binding.estado.vincular(binding.rvZonasPropietario)
        binding.swipe.setOnRefreshListener { viewModel.fetchZonas() }

        viewModel.zonas.observe(viewLifecycleOwner) { resource ->
            binding.estado.mostrarRecurso(
                resource, binding.swipe, R.drawable.ic_place, getString(R.string.zonas_res_vacio),
                reintentar = { viewModel.fetchZonas() }
            ) { lista ->
                adaptador.submitList(lista)
                binding.encabezado.ponerInfo(resumen(
                    R.plurals.resumen_zonas, lista.size,
                    R.plurals.resumen_reservables to lista.count { it.esReservable == true }
                ))
            }
        }

        if (viewModel.zonas.value == null) viewModel.fetchZonas()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
