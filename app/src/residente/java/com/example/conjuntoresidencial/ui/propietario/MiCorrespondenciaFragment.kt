package com.example.conjuntoresidencial.ui.propietario

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.FragmentMiCorrespondenciaBinding
import com.example.conjuntoresidencial.databinding.ItemCorrespondenciaBinding
import com.example.conjuntoresidencial.model.CorrespondenciaDTO
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.pintarCorrespondencia
import kotlinx.coroutines.launch

/**
 * P9 bloque 3: Mi correspondencia. Lo que llegó a la portería para el apartamento (paquetes,
 * cartas, documentos y recibos): primero lo que falta recoger y luego lo entregado, con a quién.
 */
class MiCorrespondenciaFragment : Fragment() {

    private var _binding: FragmentMiCorrespondenciaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMiCorrespondenciaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(binding.encabezado, R.string.corr_mia_titulo, R.string.corr_mia_descripcion)
        binding.swipe.setOnRefreshListener { cargar() }
        cargar()
    }

    private fun cargar() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val r = RetrofitClient.comun.miCorrespondencia()
                if (_binding == null) return@launch
                if (r.isSuccessful) pintar(r.body().orEmpty())
                else aviso(RetrofitClient.mensajeDeError(r, getString(R.string.corr_error))).show()
            } catch (e: Exception) {
                if (_binding != null) aviso(RetrofitClient.mensajeDeExcepcion(e)).show()
            }
            _binding?.swipe?.isRefreshing = false
        }
    }

    private fun pintar(lista: List<CorrespondenciaDTO>) {
        val (pendientes, entregadas) = lista.partition { it.estado != "ENTREGADA" }
        binding.tvVacio.isVisible = lista.isEmpty()
        binding.tvResumen.isVisible = pendientes.isNotEmpty()
        binding.tvResumen.text = getString(R.string.corr_mia_pendientes, pendientes.size)
        binding.lista.removeAllViews()
        (pendientes + entregadas).forEach { c ->
            val fila = ItemCorrespondenciaBinding.inflate(layoutInflater, binding.lista, false)
            pintarCorrespondencia(fila, c, conApartamento = false, alEntregar = null)
            binding.lista.addView(fila.root)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
