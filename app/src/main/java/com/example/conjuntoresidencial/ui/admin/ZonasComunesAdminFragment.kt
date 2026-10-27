package com.example.conjuntoresidencial.ui.admin

import androidx.navigation.fragment.findNavController
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FragmentZonasComunesAdminBinding
import com.example.conjuntoresidencial.ui.comun.mostrarRecurso
import com.example.conjuntoresidencial.ui.comun.accionesDeLista
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.controlesFlotantes
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.ui.comun.resumen
import com.example.conjuntoresidencial.ui.comun.aviso

/**
 * Zonas comunes del administrador: registrar una (botón flotante) o tocar una para editarla.
 * Cada zona reservable puede tener aforo (personas por reserva) y horario para reservar.
 */
class ZonasComunesAdminFragment : Fragment() {
    private var _binding: FragmentZonasComunesAdminBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ZonasAdminViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentZonasComunesAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(
            binding.encabezado, R.string.zonas_titulo, R.string.zonas_subtitulo, menu = R.menu.menu_lista,
            alElegir = accionesDeLista(binding.swipe) { viewModel.fetchZonas() }
        )

        // P5: crear o editar en una pantalla completa (horario por día y fechas bloqueadas)
        val adaptador = ZonaAdapter(onTocar = { z -> findNavController().navigate(R.id.editarZonaFragment, EditarZonaFragment.argumentos(z.id)) })
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Boolean>(EditarZonaFragment.GUARDADA)
            ?.observe(viewLifecycleOwner) { si ->
                if (si == true) {
                    findNavController().currentBackStackEntry?.savedStateHandle?.set(EditarZonaFragment.GUARDADA, false)
                    viewModel.fetchZonas()
                }
            }
        binding.rvZonasAdmin.layoutManager = LinearLayoutManager(requireContext())
        binding.rvZonasAdmin.adapter = adaptador
        binding.estado.vincular(binding.rvZonasAdmin)
        binding.swipe.setOnRefreshListener { viewModel.fetchZonas() }

        viewModel.zonas.observe(viewLifecycleOwner) { resource ->
            binding.estado.mostrarRecurso(
                resource, binding.swipe, R.drawable.ic_place, getString(R.string.zonas_vacio),
                reintentar = { viewModel.fetchZonas() }
            ) { lista ->
                adaptador.submitList(lista)
                binding.encabezado.ponerInfo(resumen(
                    R.plurals.resumen_zonas, lista.size,
                    R.plurals.resumen_reservables to lista.count { it.esReservable == true }
                ))
            }
        }

        viewModel.registro.observe(viewLifecycleOwner) { evento ->
            evento.obtenerSiNoManejado()?.let { aviso(it).show() }
        }

        // Botón flotante (encima del difuminado); se encoge a solo ícono al desplazar hacia abajo
        controlesFlotantes?.mostrarBotonFlotante(R.string.zonas_registrar, R.drawable.ic_add) {
            findNavController().navigate(R.id.editarZonaFragment, EditarZonaFragment.argumentos(null))
        }?.let { boton ->
            binding.rvZonasAdmin.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                    if (dy > 0) boton.shrink() else if (dy < 0) boton.extend()
                }
            })
        }

        if (viewModel.zonas.value == null) viewModel.fetchZonas()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
