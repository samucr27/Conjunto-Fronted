package com.example.conjuntoresidencial.ui.contabilidad

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.api.instance
import com.example.conjuntoresidencial.databinding.FragmentPantallaContenidoBinding
import com.example.conjuntoresidencial.model.SolicitudContableDTO
import com.example.conjuntoresidencial.ui.comun.Bloques
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.util.SessionManager
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

/**
 * P11 bloque 7: lo que el contador manda al administrador. Para el administrador es el historial de
 * aprobaciones y rechazos; para el contador, "Mis solicitudes" (las devueltas, para corregirlas).
 * Tocar una abre su detalle.
 */
class SolicitudesContablesFragment : Fragment() {

    private var _binding: FragmentPantallaContenidoBinding? = null
    private val binding get() = _binding!!
    private var estado: String? = null
    private var todas: List<SolicitudContableDTO> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPantallaContenidoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val contador = SessionManager(requireContext()).esContador()
        configurarEncabezado(binding.encabezado,
            if (contador) R.string.cont_mis_solicitudes else R.string.cont_aprobaciones,
            if (contador) R.string.cont_mis_solicitudes_sub else R.string.cont_aprobaciones_sub)
        estado = savedInstanceState?.getString(FormulariosCartera.ARG_ESTADO) ?: arguments?.getString(FormulariosCartera.ARG_ESTADO)
            ?: if (contador) null else "PENDIENTE"
        binding.swipe.setOnRefreshListener { cargar() }
        binding.estado.vincular(binding.desplazar)
        cargar()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(FormulariosCartera.ARG_ESTADO, estado)
    }

    override fun onResume() {
        super.onResume()
        // Al volver del detalle (aprobada, devuelta o corregida) la lista cambia
        if (todas.isNotEmpty()) cargar()
    }

    private fun cargar() {
        if (!binding.swipe.isRefreshing && todas.isEmpty()) binding.estado.mostrarCargando()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.solicitudesContables(null)
                if (_binding == null) return@launch
                binding.swipe.isRefreshing = false
                if (!r.isSuccessful) {
                    binding.estado.mostrarError(RetrofitClient.mensajeDeError(r, getString(R.string.cont_error_cargar))) { cargar() }
                    return@launch
                }
                todas = r.body().orEmpty()
                pintar()
            } catch (e: Exception) {
                if (_binding == null) return@launch
                binding.swipe.isRefreshing = false
                binding.estado.mostrarError(RetrofitClient.mensajeDeExcepcion(e)) { cargar() }
            }
        }
    }

    private fun pintar() {
        val c = binding.contenido
        c.removeAllViews()
        val pendientes = todas.count { it.estado == "PENDIENTE" }
        val devueltas = todas.count { it.estado == "RECHAZADA" }
        binding.encabezado.ponerInfo(resources.getQuantityString(R.plurals.cont_esperando, pendientes, pendientes))
        // Filtros
        val chips = ChipGroup(requireContext()).apply { isSingleSelection = true; isSelectionRequired = true }
        listOf(getString(R.string.cont_filtro_pendientes, pendientes) to "PENDIENTE",
            getString(R.string.cont_filtro_devueltas, devueltas) to "RECHAZADA",
            getString(R.string.cont_filtro_aprobadas) to "APROBADA",
            getString(R.string.cont_filtro_todas) to null).forEach { (texto, valor) ->
            chips.addView(Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle).apply {
                text = texto
                isCheckable = true
                isChecked = valor == estado
                setOnClickListener { estado = valor; pintar() }
            })
        }
        c.addView(android.widget.HorizontalScrollView(requireContext()).apply {
            isHorizontalScrollBarEnabled = false
            addView(chips)
        })
        val lista = todas.filter { estado == null || it.estado == estado }
        if (lista.isEmpty()) {
            binding.estado.mostrarContenido()
            Bloques.parrafo(c, getString(R.string.cont_sin_solicitudes))
            return
        }
        lista.forEach { s ->
            Bloques.tarjeta(c, s.resumen ?: TextosContables.tipo(this, s.tipo),
                detalle = TextosContables.tipo(this, s.tipo) + " · " + getString(R.string.cont_version, s.version) +
                    (s.motivoRechazo?.let { "\n" + getString(R.string.cont_motivo_linea, it) } ?: ""),
                extra = TextosContables.quien(this, s),
                etiqueta = TextosContables.estado(this, s)) {
                findNavController().navigate(R.id.solicitudDetalleFragment, bundleOf(FormulariosCartera.ARG_SOLICITUD to s.id))
            }
        }
        binding.estado.mostrarContenido()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
