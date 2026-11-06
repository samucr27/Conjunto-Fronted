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
import com.example.conjuntoresidencial.databinding.FragmentDentroBinding
import com.example.conjuntoresidencial.databinding.ItemTextoConAccionBinding
import com.example.conjuntoresidencial.model.VisitaDTO
import com.example.conjuntoresidencial.ui.comun.FiltroTorreApto
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.confirmar
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import kotlinx.coroutines.launch

/**
 * P10: quién está dentro del conjunto ahora (visitas autorizadas sin salida), desde Visitas. Se
 * filtra primero por torre y después por apartamento (o nombre o placa) para encontrar a quien
 * sale y registrar su salida, que libera el parqueadero de invitados si tenía uno.
 */
class DentroFragment : Fragment() {

    private var _binding: FragmentDentroBinding? = null
    private val binding get() = _binding!!
    private var todos: List<VisitaDTO> = emptyList()
    private lateinit var filtro: FiltroTorreApto

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDentroBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(binding.encabezado, R.string.visitas_dentro, R.string.dentro_descripcion)
        filtro = FiltroTorreApto(binding.filtro) { _, _ -> pintar() }
        filtro.ponerPista(R.string.dentro_buscar)
        binding.swipe.setOnRefreshListener { cargar() }
        cargar()
    }

    private fun cargar() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.visitasDentro()
                if (_binding == null) return@launch
                if (r.isSuccessful) {
                    todos = r.body().orEmpty()
                    filtro.ponerTorres(todos.map { it.torre })
                    binding.encabezado.ponerInfo(resources.getQuantityString(R.plurals.dentro_resumen, todos.size, todos.size))
                    pintar()
                } else {
                    aviso(RetrofitClient.mensajeDeError(r, getString(R.string.porteria_error))).show()
                }
            } catch (e: Exception) {
                if (_binding != null) aviso(RetrofitClient.mensajeDeExcepcion(e)).show()
            }
            _binding?.swipe?.isRefreshing = false
        }
    }

    private fun pintar() {
        val q = filtro.texto
        val torre = filtro.torre
        val lista = todos.filter { v ->
            (torre == null || v.torre == torre) && (q.isEmpty() || FiltroTorreApto.coincideApto(v.apto, q) ||
                FiltroTorreApto.normalizar(v.visitante).contains(q) ||
                FiltroTorreApto.normalizar(v.placa.orEmpty()).replace(" ", "").contains(q.replace(" ", "")))
        }
        binding.tvVacio.isVisible = lista.isEmpty()
        binding.tvVacio.setText(if (todos.isEmpty()) R.string.dentro_vacio else R.string.dentro_sin_resultados)
        binding.lista.removeAllViews()
        lista.forEach { v ->
            val fila = ItemTextoConAccionBinding.inflate(layoutInflater, binding.lista, false)
            fila.tvEtiqueta.isVisible = true
            fila.tvEtiqueta.text = ingreso(v)
            fila.tvTitulo.text = v.visitante
            fila.tvDetalle.text = getString(R.string.recepcion_dentro_detalle, v.torre, v.apto, fechaCorta(v.creadaEn))
            fila.btnAccion.isVisible = true
            fila.btnAccion.setText(R.string.recepcion_salida)
            fila.btnAccion.setOnClickListener {
                confirmar(getString(R.string.recepcion_salida_confirmar, v.visitante), null, R.string.recepcion_salida) { salida(v) }
            }
            binding.lista.addView(fila.root)
        }
    }

    private fun salida(v: VisitaDTO) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.registrarSalidaVisita(v.id)
                if (_binding == null) return@launch
                aviso(if (r.isSuccessful) {
                    v.parqueadero?.let { getString(R.string.ingreso_salida_ok, it) } ?: getString(R.string.recepcion_salida_ok)
                } else RetrofitClient.mensajeDeError(r, getString(R.string.porteria_error))).show()
                cargar()
            } catch (e: Exception) {
                if (_binding != null) aviso(RetrofitClient.mensajeDeExcepcion(e)).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
