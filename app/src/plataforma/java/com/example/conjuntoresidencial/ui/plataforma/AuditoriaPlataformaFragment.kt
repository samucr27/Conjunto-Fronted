package com.example.conjuntoresidencial.ui.plataforma

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.api.instance
import com.example.conjuntoresidencial.databinding.FragmentAuditoriaPlataformaBinding
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.google.android.material.chip.Chip
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * P7: lo que hizo cada operador de la plataforma, de todos o de un conjunto. P8 bloque 6: se
 * busca por texto (operador, conjunto o detalle), se filtra por acción y por rango de fechas, y
 * se pagina de a 50. Tocar un evento de un conjunto abre su detalle.
 */
class AuditoriaPlataformaFragment : Fragment() {

    private var _binding: FragmentAuditoriaPlataformaBinding? = null
    private val binding get() = _binding!!
    private val conjunto get() = arguments?.getLong(ARG_CONJUNTO)?.takeIf { it > 0 }
    private var busqueda: Job? = null
    private var pagina = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAuditoriaPlataformaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val titulo = arguments?.getString(ARG_TITULO).orEmpty().ifBlank { getString(R.string.plataforma_auditoria) }
        configurarEncabezado(binding.encabezado, titulo, R.string.plataforma_auditoria_descripcion)

        // "Todas" y una por acción
        val acciones = listOf<Pair<String?, Int>>(null to R.string.auditoria_todas) + ACCIONES_AUDITORIA
        acciones.forEachIndexed { i, (codigo, nombre) ->
            binding.chipsAccion.addView(Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle).apply {
                id = View.generateViewId()
                tag = codigo
                setText(nombre)
                isCheckable = true
                isChecked = i == 0
            })
        }
        binding.chipsAccion.setOnCheckedStateChangeListener { _, _ -> buscar(esperar = false) }
        binding.chipsRango.setOnCheckedStateChangeListener { _, _ -> buscar(esperar = false) }
        binding.etBuscar.doAfterTextChanged { buscar(esperar = true) }
        binding.btnCargarMas.setOnClickListener { cargar(pagina + 1) }
        buscar(esperar = false)
    }

    private fun accion(): String? = binding.chipsAccion.findViewById<Chip>(binding.chipsAccion.checkedChipId)?.tag as? String

    /** El primer día del rango ("2026-11-05"), o null para todo. */
    private fun desde(): String? {
        val dias = when (binding.chipsRango.checkedChipId) {
            R.id.chipRangoHoy -> 0
            R.id.chipRango7 -> 6
            R.id.chipRango30 -> 29
            else -> return null
        }
        val dia = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -dias) }
        return SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(dia.time)
    }

    private fun buscar(esperar: Boolean) {
        busqueda?.cancel()
        busqueda = viewLifecycleOwner.lifecycleScope.launch {
            if (esperar) delay(350)
            cargarAhora(0)
        }
    }

    private fun cargar(p: Int) {
        busqueda?.cancel()
        busqueda = viewLifecycleOwner.lifecycleScope.launch { cargarAhora(p) }
    }

    private suspend fun cargarAhora(p: Int) {
        val q = binding.etBuscar.text?.toString()?.trim().orEmpty().ifEmpty { null }
        binding.progreso.isVisible = true
        binding.btnCargarMas.isEnabled = false
        try {
            val r = RetrofitClient.instance.buscarAuditoria(conjunto, accion(), desde(), null, q, p)
            val resultado = r.body()
            if (_binding == null) return
            if (r.isSuccessful && resultado != null) {
                pagina = p
                if (p == 0) binding.lista.removeAllViews()
                resultado.eventos.forEach { e ->
                    val fila = filaEvento(binding.lista, e, conConjunto = conjunto == null)
                    if (conjunto == null) e.conjuntoId?.let { id ->
                        fila.setOnClickListener {
                            findNavController().navigate(R.id.conjuntoDetalleFragment, bundleOf(ConjuntoDetalleFragment.ARG_CONJUNTO to id))
                        }
                    }
                    binding.lista.addView(fila)
                }
                binding.tvVacio.isVisible = binding.lista.childCount == 0
                binding.btnCargarMas.isVisible = resultado.hayMas
            } else {
                aviso(RetrofitClient.mensajeDeError(r, getString(R.string.plataforma_error_cargar))).show()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            if (_binding != null) aviso(RetrofitClient.mensajeDeExcepcion(e)).show()
        } finally {
            _binding?.let {
                it.progreso.isVisible = false
                it.btnCargarMas.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_CONJUNTO = "conjuntoId"
        const val ARG_TITULO = "titulo"
    }
}
