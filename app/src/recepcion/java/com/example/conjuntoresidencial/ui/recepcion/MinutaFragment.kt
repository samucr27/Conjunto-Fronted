package com.example.conjuntoresidencial.ui.recepcion

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.api.instance
import com.example.conjuntoresidencial.databinding.FragmentMinutaBinding
import com.example.conjuntoresidencial.databinding.ItemTextoConAccionBinding
import com.example.conjuntoresidencial.model.AnotacionMinuta
import com.example.conjuntoresidencial.model.NuevaAnotacion
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

/**
 * P8 bloque 5: la minuta de la portería. El guardia elige la categoría (novedad, ronda, paquete,
 * incidente o turno), escribe qué pasó y queda con su nombre y la hora. No se edita ni se borra.
 */
class MinutaFragment : Fragment() {

    private var _binding: FragmentMinutaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMinutaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(binding.encabezado, R.string.minuta_titulo, R.string.minuta_descripcion)
        CATEGORIAS_MINUTA.forEachIndexed { i, (codigo, nombre) ->
            binding.chipsCategoria.addView(Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle).apply {
                id = View.generateViewId()
                tag = codigo
                setText(nombre)
                isCheckable = true
                isChecked = i == 0
            })
        }
        binding.btnAnotar.setOnClickListener { anotar() }
        cargar()
    }

    private fun categoria(): String = binding.chipsCategoria.findViewById<Chip>(binding.chipsCategoria.checkedChipId)?.tag as? String
        ?: CATEGORIAS_MINUTA.first().first

    private fun anotar() {
        val texto = binding.etTexto.text?.toString()?.trim().orEmpty()
        binding.tilTexto.error = if (texto.isEmpty()) getString(R.string.minuta_falta_texto) else null
        if (texto.isEmpty()) return
        binding.btnAnotar.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.anotar(NuevaAnotacion(categoria(), texto))
                if (_binding == null) return@launch
                if (r.isSuccessful) {
                    binding.etTexto.setText("")
                    aviso(R.string.minuta_anotada).show()
                    cargar()
                } else {
                    aviso(RetrofitClient.mensajeDeError(r, getString(R.string.porteria_error))).show()
                }
            } catch (e: Exception) {
                if (_binding != null) aviso(RetrofitClient.mensajeDeExcepcion(e)).show()
            }
            _binding?.btnAnotar?.isEnabled = true
        }
    }

    private fun cargar() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.minuta(7)
                val lista = r.body()
                if (_binding != null && r.isSuccessful && lista != null) pintar(lista)
            } catch (e: Exception) {
                if (_binding != null) aviso(RetrofitClient.mensajeDeExcepcion(e)).show()
            }
        }
    }

    private fun pintar(lista: List<AnotacionMinuta>) {
        binding.tvVacio.isVisible = lista.isEmpty()
        binding.lista.removeAllViews()
        lista.forEach { m ->
            val fila = ItemTextoConAccionBinding.inflate(layoutInflater, binding.lista, false)
            fila.tvEtiqueta.isVisible = true
            fila.tvEtiqueta.setText(nombreCategoria(m.categoria))
            fila.tvTitulo.text = m.texto
            fila.tvDetalle.text = m.guardia?.let { getString(R.string.minuta_pie, fechaCorta(m.creadaEn), it) } ?: fechaCorta(m.creadaEn)
            binding.lista.addView(fila.root)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
