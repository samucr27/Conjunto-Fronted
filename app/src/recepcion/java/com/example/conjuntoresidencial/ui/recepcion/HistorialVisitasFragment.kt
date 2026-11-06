package com.example.conjuntoresidencial.ui.recepcion

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.api.instance
import com.example.conjuntoresidencial.databinding.FragmentHistorialVisitasBinding
import com.example.conjuntoresidencial.databinding.ItemVisitaBinding
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * P8 bloque 5: las visitas de hoy, 7, 30 o 90 días, de la más reciente a la más vieja. Se busca
 * mientras se escribe (nombre, documento, placa o "5-502"); "Cargar más" trae las siguientes 30.
 */
class HistorialVisitasFragment : Fragment() {

    private var _binding: FragmentHistorialVisitasBinding? = null
    private val binding get() = _binding!!
    private var busqueda: Job? = null
    private var pagina = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHistorialVisitasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(binding.encabezado, R.string.historial_titulo, R.string.historial_descripcion)
        binding.etBuscar.doAfterTextChanged { buscar(esperar = true) }
        binding.chipsRango.setOnCheckedStateChangeListener { _, _ -> buscar(esperar = false) }
        binding.btnCargarMas.setOnClickListener { cargar(pagina + 1) }
        buscar(esperar = false)
    }

    private fun dias() = when (binding.chipsRango.checkedChipId) {
        R.id.chipHoy -> 1
        R.id.chip30 -> 30
        R.id.chip90 -> 90
        else -> 7
    }

    /** Desde la primera página; al escribir espera un momento para no consultar con cada letra. */
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
            val r = RetrofitClient.instance.historial(q, dias(), p)
            val resultado = r.body()
            if (_binding == null) return
            if (r.isSuccessful && resultado != null) {
                pagina = p
                if (p == 0) binding.lista.removeAllViews()
                resultado.visitas.forEach { v ->
                    val fila = ItemVisitaBinding.inflate(layoutInflater, binding.lista, false)
                    pintarVisita(fila, v)
                    binding.lista.addView(fila.root)
                }
                binding.tvVacio.isVisible = binding.lista.childCount == 0
                binding.btnCargarMas.isVisible = resultado.hayMas
            } else {
                aviso(RetrofitClient.mensajeDeError(r, getString(R.string.porteria_error))).show()
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
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
}
