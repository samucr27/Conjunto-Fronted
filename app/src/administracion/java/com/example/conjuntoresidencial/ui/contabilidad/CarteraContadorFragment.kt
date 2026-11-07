package com.example.conjuntoresidencial.ui.contabilidad

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.api.instance
import com.example.conjuntoresidencial.databinding.FiltroTorreAptoBinding
import com.example.conjuntoresidencial.databinding.FragmentPantallaContenidoBinding
import com.example.conjuntoresidencial.model.AptoCarteraDTO
import com.example.conjuntoresidencial.ui.comun.Bloques
import com.example.conjuntoresidencial.ui.comun.FiltroTorreApto
import com.example.conjuntoresidencial.ui.comun.TonoEstado
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.util.Formatos
import kotlinx.coroutines.launch

/**
 * P11 bloque 6: la cartera del contador. Primero se elige la torre y después el apartamento (o se
 * escribe); cada uno muestra su saldo, si está en mora, su coeficiente y si tiene algo esperando
 * al administrador. Tocar uno abre su cartera.
 */
class CarteraContadorFragment : Fragment() {

    private var _binding: FragmentPantallaContenidoBinding? = null
    private val binding get() = _binding!!
    private lateinit var filtro: FiltroTorreApto
    private lateinit var lista: LinearLayout
    private var todos: List<AptoCarteraDTO> = emptyList()
    private var soloMora = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPantallaContenidoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(binding.encabezado, R.string.cont_cartera_titulo, R.string.cont_cartera_sub)
        val f = FiltroTorreAptoBinding.inflate(layoutInflater, binding.contenido, false)
        binding.contenido.addView(f.root)
        filtro = FiltroTorreApto(f) { _, _ -> pintar() }
        filtro.ponerPista(R.string.cont_buscar_apto)
        val chipMora = com.google.android.material.chip.Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle).apply {
            setText(R.string.cont_solo_mora)
            isCheckable = true
            setOnCheckedChangeListener { _, marcado -> soloMora = marcado; pintar() }
        }
        binding.contenido.addView(chipMora)
        lista = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        binding.contenido.addView(lista)
        binding.swipe.setOnRefreshListener { cargar() }
        binding.estado.vincular(binding.desplazar)
        cargar()
    }

    override fun onResume() {
        super.onResume()
        if (todos.isNotEmpty()) cargar()
    }

    private fun cargar() {
        if (!binding.swipe.isRefreshing && todos.isEmpty()) binding.estado.mostrarCargando()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.carteraPorApartamento(null)
                if (_binding == null) return@launch
                binding.swipe.isRefreshing = false
                if (!r.isSuccessful) {
                    binding.estado.mostrarError(RetrofitClient.mensajeDeError(r, getString(R.string.cont_error_cargar))) { cargar() }
                    return@launch
                }
                todos = r.body().orEmpty()
                filtro.ponerTorres(todos.map { it.torre })
                val mora = todos.count { it.vencidas > 0 }
                binding.encabezado.ponerInfo(getString(R.string.cont_cartera_info, todos.size, mora,
                    Formatos.moneda(todos.sumOf { it.saldo })))
                pintar()
                binding.estado.mostrarContenido()
            } catch (e: Exception) {
                if (_binding == null) return@launch
                binding.swipe.isRefreshing = false
                binding.estado.mostrarError(RetrofitClient.mensajeDeExcepcion(e)) { cargar() }
            }
        }
    }

    private fun pintar() {
        lista.removeAllViews()
        val q = filtro.texto
        val torre = filtro.torre
        val visibles = todos.filter { a ->
            (torre == null || a.torre == torre) && (!soloMora || a.vencidas > 0) &&
                (q.isEmpty() || FiltroTorreApto.coincideApto(a.apto, q) || FiltroTorreApto.normalizar("${a.torre}-${a.apto}").contains(q))
        }
        if (visibles.isEmpty()) { Bloques.parrafo(lista, getString(R.string.cont_sin_aptos)); return }
        visibles.forEach { a ->
            val etiqueta = when {
                a.vencidas > 0 -> getString(R.string.cont_en_mora) to TonoEstado.ERROR
                a.pendientes > 0 -> getString(R.string.cont_por_pagar) to TonoEstado.ADVERTENCIA
                else -> getString(R.string.cont_al_dia) to TonoEstado.EXITO
            }
            val extra = listOfNotNull(
                a.coeficiente?.let { getString(R.string.cont_coeficiente_linea, it.toString()) },
                if (a.solicitudesAbiertas > 0) resources.getQuantityString(R.plurals.cont_esperando, a.solicitudesAbiertas, a.solicitudesAbiertas) else null
            ).joinToString(" · ").ifEmpty { null }
            Bloques.tarjeta(lista, getString(R.string.cont_torre_apto, a.torre, a.apto),
                detalle = getString(R.string.cont_saldo_linea, Formatos.moneda(a.saldo), a.pendientes, a.vencidas),
                extra = extra, etiqueta = etiqueta) {
                findNavController().navigate(R.id.carteraApartamentoFragment, bundleOf(FormulariosCartera.ARG_APARTAMENTO to a.apartamentoId))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
