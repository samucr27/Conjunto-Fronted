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
import com.example.conjuntoresidencial.databinding.FragmentPantallaContenidoBinding
import com.example.conjuntoresidencial.model.PanelContadorDTO
import com.example.conjuntoresidencial.ui.cartera.Cuotas
import com.example.conjuntoresidencial.ui.comun.Bloques
import com.example.conjuntoresidencial.ui.comun.TonoEstado
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.util.Formatos
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.launch

/**
 * P11 bloque 6: el resumen del contador: cómo va el recaudo del mes, la cartera, sus solicitudes
 * (las devueltas primero), el recaudo de los últimos seis meses y los apartamentos que más deben.
 */
class PanelContadorFragment : Fragment() {

    private var _binding: FragmentPantallaContenidoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPantallaContenidoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(binding.encabezado, R.string.cont_panel_titulo, R.string.cont_panel_sub)
        binding.swipe.setOnRefreshListener { cargar() }
        binding.estado.vincular(binding.desplazar)
        cargar()
    }

    private fun cargar() {
        if (!binding.swipe.isRefreshing) binding.estado.mostrarCargando()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.panelContador()
                if (_binding == null) return@launch
                binding.swipe.isRefreshing = false
                val p = r.body()
                if (!r.isSuccessful || p == null) {
                    binding.estado.mostrarError(RetrofitClient.mensajeDeError(r, getString(R.string.cont_error_cargar))) { cargar() }
                    return@launch
                }
                pintar(p)
                binding.estado.mostrarContenido()
            } catch (e: Exception) {
                if (_binding == null) return@launch
                binding.swipe.isRefreshing = false
                binding.estado.mostrarError(RetrofitClient.mensajeDeExcepcion(e)) { cargar() }
            }
        }
    }

    private fun pintar(p: PanelContadorDTO) {
        val c = binding.contenido
        c.removeAllViews()
        val m = p.mes
        binding.encabezado.ponerInfo(getString(R.string.cont_recaudo_mes, Cuotas.nombreMes(m.periodo), m.porcentajeRecaudo))

        // Lo que pide atención: devueltas para corregir
        if (p.misDevueltas > 0) {
            Bloques.tarjeta(c, resources.getQuantityString(R.plurals.cont_devueltas, p.misDevueltas.toInt(), p.misDevueltas.toInt()),
                detalle = getString(R.string.cont_devueltas_ayuda), etiqueta = getString(R.string.cont_corregir) to TonoEstado.ERROR) {
                findNavController().navigate(R.id.solicitudesContablesFragment, bundleOf(FormulariosCartera.ARG_ESTADO to "RECHAZADA"))
            }
        }
        Bloques.tarjeta(c, getString(R.string.cont_mis_solicitudes),
            detalle = resources.getQuantityString(R.plurals.cont_esperando, p.misPendientes.toInt(), p.misPendientes.toInt())) {
            findNavController().navigate(R.id.solicitudesContablesFragment)
        }

        Bloques.seccion(c, getString(R.string.cont_este_mes, Cuotas.nombreMes(m.periodo)))
        Bloques.cifras(c, Formatos.moneda(m.facturado) to getString(R.string.cont_facturado),
            Formatos.moneda(m.recaudado) to getString(R.string.cont_recaudado))
        Bloques.cifras(c, Formatos.moneda(m.porCobrar) to getString(R.string.cont_por_cobrar),
            "${m.porcentajeRecaudo} %" to getString(R.string.cont_recaudo))
        Bloques.cifras(c, m.apartamentosAlDia.toString() to getString(R.string.cont_al_dia),
            m.apartamentosEnMora.toString() to getString(R.string.cont_en_mora),
            Formatos.moneda(m.carteraTotal) to getString(R.string.cont_cartera_total))

        Bloques.seccion(c, getString(R.string.cont_seis_meses))
        val meses = Bloques.tarjeta(c, getString(R.string.cont_recaudado_vs_facturado))
        p.recaudo.forEach { r ->
            val pct = if (r.facturado > 0) (r.recaudado * 100 / r.facturado).toInt() else 0
            Bloques.dato(meses, Cuotas.nombreMes(r.periodo),
                if (r.facturado > 0) getString(R.string.cont_recaudo_linea, Formatos.moneda(r.recaudado), Formatos.moneda(r.facturado), pct)
                else getString(R.string.cont_sin_facturar))
            if (r.facturado > 0) meses.addView(LinearProgressIndicator(requireContext()).apply {
                max = 100; progress = pct; isIndeterminate = false
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                trackCornerRadius = (4 * resources.displayMetrics.density).toInt()
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (8 * resources.displayMetrics.density).toInt()
            })
        }

        Bloques.seccion(c, getString(R.string.cont_acciones))
        Bloques.boton(c, getString(R.string.cont_facturar_titulo), icono = R.drawable.ic_receipt) {
            findNavController().navigate(R.id.facturacionContadorFragment)
        }
        Bloques.boton(c, getString(R.string.cont_cartera_titulo), relleno = false, icono = R.drawable.ic_wallet) {
            findNavController().navigate(R.id.carteraContadorFragment)
        }

        if (p.morosos.isNotEmpty()) {
            Bloques.seccion(c, getString(R.string.cont_mas_deben))
            p.morosos.forEach { a ->
                Bloques.tarjeta(c, getString(R.string.cont_torre_apto, a.torre, a.apto),
                    detalle = getString(R.string.cont_saldo_linea, Formatos.moneda(a.saldo), a.pendientes, a.vencidas),
                    etiqueta = getString(R.string.cont_en_mora) to TonoEstado.ERROR) {
                    findNavController().navigate(R.id.carteraApartamentoFragment, bundleOf(FormulariosCartera.ARG_APARTAMENTO to a.apartamentoId))
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
