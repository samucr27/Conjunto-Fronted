package com.example.conjuntoresidencial.ui.admin

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.api.instance
import com.example.conjuntoresidencial.databinding.FragmentCarteraResumenBinding
import com.example.conjuntoresidencial.databinding.ItemPersonalFilaBinding
import com.example.conjuntoresidencial.model.EstadisticasCartera
import com.example.conjuntoresidencial.model.RevisionSolicitud
import com.example.conjuntoresidencial.model.SolicitudContable
import com.example.conjuntoresidencial.ui.cartera.Cuotas
import com.example.conjuntoresidencial.ui.comun.EstadoVisual
import com.example.conjuntoresidencial.ui.comun.TonoEstado
import com.example.conjuntoresidencial.ui.comun.accionesDeLista
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.confirmar
import com.example.conjuntoresidencial.ui.comun.ejecutarAccion
import com.example.conjuntoresidencial.ui.comun.elegirOpcion
import com.example.conjuntoresidencial.ui.comun.mostrarEstado
import com.example.conjuntoresidencial.ui.comun.pedirTexto
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.util.Formatos
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.YearMonth

/**
 * Cartera de la administración (P5): estadísticas de un mes y lo que el contador mandó a aprobar
 * (la facturación del mes y los pagos en efectivo). El detalle por apartamento es del contador.
 */
class CarteraResumenFragment : Fragment() {
    private var _binding: FragmentCarteraResumenBinding? = null
    private val binding get() = _binding!!
    private var cargado = false

    // El mes elegido sobrevive a girar la pantalla
    private var mes: YearMonth
        get() = arguments?.getString(ARG_MES)?.let(YearMonth::parse) ?: YearMonth.now()
        set(valor) { arguments = (arguments ?: Bundle()).apply { putString(ARG_MES, valor.toString()) } }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCarteraResumenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(
            binding.encabezado, R.string.cartera_resumen_titulo, R.string.cartera_resumen_subtitulo, menu = R.menu.menu_lista,
            alElegir = accionesDeLista(binding.swipe) { cargar() }
        )
        binding.estado.vincular(binding.contenido)
        binding.swipe.setOnRefreshListener { cargar() }
        binding.btnMesAnterior.setOnClickListener { mes = mes.minusMonths(1); cargar() }
        binding.btnMesSiguiente.setOnClickListener { mes = mes.plusMonths(1); cargar() }
        cargar()
    }

    /** Estadísticas del mes y solicitudes pendientes en paralelo. */
    private fun cargar() {
        binding.tvMes.text = Cuotas.nombreMes(mes.toString())
        if (!cargado) binding.estado.mostrarCargando()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val (e, s) = coroutineScope {
                    val a = async { RetrofitClient.instance.getEstadisticasCartera(mes.toString()) }
                    val b = async { RetrofitClient.instance.getSolicitudesContables("PENDIENTE") }
                    a.await() to b.await()
                }
                binding.swipe.isRefreshing = false
                val estadisticas = e.body()
                if (!e.isSuccessful || estadisticas == null) {
                    binding.estado.mostrarError(RetrofitClient.mensajeDeError(e, getString(R.string.cartera_error))) { cargar() }
                    return@launch
                }
                cargado = true
                mostrarEstadisticas(estadisticas)
                mostrarSolicitudes(if (s.isSuccessful) s.body().orEmpty() else emptyList())
                binding.estado.mostrarContenido()
            } catch (ex: Exception) {
                binding.swipe.isRefreshing = false
                binding.estado.mostrarError(RetrofitClient.mensajeDeExcepcion(ex)) { cargar() }
            }
        }
    }

    private fun mostrarEstadisticas(e: EstadisticasCartera) {
        if (e.cuotas == 0) {
            binding.tvRecaudo.setText(R.string.cartera_sin_cuotas)
            binding.barraRecaudo.isVisible = false
            binding.tvPorCobrar.isVisible = false
            binding.tvEstadosCuotas.isVisible = false
        } else {
            binding.tvRecaudo.text = getString(R.string.cartera_recaudo_valor,
                Formatos.moneda(e.recaudado), Formatos.moneda(e.facturado), e.porcentajeRecaudo)
            binding.barraRecaudo.isVisible = true
            binding.barraRecaudo.setProgressCompat(e.porcentajeRecaudo, true)
            binding.tvPorCobrar.isVisible = true
            binding.tvPorCobrar.text = getString(R.string.cartera_por_cobrar, Formatos.moneda(e.porCobrar))
            binding.tvEstadosCuotas.isVisible = true
            binding.tvEstadosCuotas.text = getString(R.string.cartera_cuotas_estados, e.pagadas, e.enRevision, e.pendientes, e.vencidas)
        }
        binding.tvApartamentos.text = getString(R.string.cartera_apartamentos_valor,
            e.apartamentosAlDia, e.apartamentosEnMora, e.apartamentos)
        binding.tvCarteraTotal.text = getString(R.string.cartera_total, Formatos.moneda(e.carteraTotal))
    }

    /** Una fila por solicitud; al tocarla, aprobar o rechazar (con motivo). */
    private fun mostrarSolicitudes(lista: List<SolicitudContable>) {
        binding.contenedorSolicitudes.removeAllViews()
        val fondo = android.util.TypedValue().also {
            requireContext().theme.resolveAttribute(android.R.attr.selectableItemBackground, it, true)
        }.resourceId
        lista.forEach { s ->
            val fila = ItemPersonalFilaBinding.inflate(layoutInflater, binding.contenedorSolicitudes, true)
            fila.tvNombreFila.text = titulo(s)
            fila.tvCedulaFila.text = listOf(detalle(s), getString(R.string.cartera_solicitud_por, s.solicitadaPor)).joinToString("\n")
            fila.tvArlFila.mostrarEstado(EstadoVisual(R.string.estado_pendiente_aprobacion, TonoEstado.ADVERTENCIA))
            fila.root.setBackgroundResource(fondo)
            fila.root.isClickable = true
            fila.root.isFocusable = true
            fila.root.setOnClickListener { revisar(s) }
        }
        binding.tvSinSolicitudes.isVisible = lista.isEmpty()
        binding.encabezado.ponerInfo(
            if (lista.isEmpty()) null else resources.getQuantityString(R.plurals.cartera_por_aprobar, lista.size, lista.size)
        )
    }

    private fun titulo(s: SolicitudContable): String =
        if (s.tipo == FACTURACION) getString(R.string.cartera_solicitud_facturacion, Cuotas.nombreMes(s.periodo.orEmpty()))
        else getString(R.string.cartera_solicitud_pago, s.torre.orEmpty(), s.apto.orEmpty())

    private fun detalle(s: SolicitudContable): String {
        val valor = Formatos.moneda(s.valor ?: 0.0)
        return if (s.tipo == FACTURACION) getString(R.string.cartera_solicitud_facturacion_detalle, valor, Formatos.fecha(s.fechaLimite))
        else getString(R.string.cartera_solicitud_pago_detalle, valor, Cuotas.nombreMes(s.periodo.orEmpty()))
    }

    private fun revisar(s: SolicitudContable) {
        elegirOpcion(titulo(s), listOf(
            R.string.cartera_aprobar to {
                confirmar(titulo(s), detalle(s), R.string.cartera_aprobar) {
                    ejecutarAccion(getString(R.string.cartera_aprobada),
                        { RetrofitClient.instance.revisarSolicitudContable(s.id, RevisionSolicitud(true)) }) { cargar() }
                }
            },
            R.string.cartera_rechazar to {
                pedirTexto(getString(R.string.cartera_rechazar), R.string.obra_motivo_hint, null,
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES,
                    validar = { if (it.isBlank()) getString(R.string.obra_motivo_obligatorio) else null }) { motivo ->
                    ejecutarAccion(getString(R.string.cartera_rechazada),
                        { RetrofitClient.instance.revisarSolicitudContable(s.id, RevisionSolicitud(false, motivo)) }) { cargar() }
                }
            }
        ))
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    companion object {
        private const val ARG_MES = "mes"
        private const val FACTURACION = "FACTURACION"
    }
}
