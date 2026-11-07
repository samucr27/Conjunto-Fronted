package com.example.conjuntoresidencial.ui.contabilidad

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.api.instance
import com.example.conjuntoresidencial.databinding.FragmentPantallaContenidoBinding
import com.example.conjuntoresidencial.model.DetalleSolicitudDTO
import com.example.conjuntoresidencial.model.RevisionSolicitud
import com.example.conjuntoresidencial.ui.comun.Bloques
import com.example.conjuntoresidencial.ui.comun.Bloques.textoLimpio
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.confirmar
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.util.Formatos
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * P11 bloque 7: todo lo de una solicitud del contador: lo que pide (y, si es una edición, lo que
 * estaba aprobado), la vista previa de la facturación, su nota y el historial con cada versión.
 * El administrador la aprueba o la devuelve con el motivo; el contador la corrige o la edita.
 */
class SolicitudDetalleFragment : Fragment() {

    private var _binding: FragmentPantallaContenidoBinding? = null
    private val binding get() = _binding!!
    private val id: Long get() = requireArguments().getLong(FormulariosCartera.ARG_SOLICITUD)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPantallaContenidoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(binding.encabezado, R.string.cont_detalle_titulo, R.string.cont_detalle_sub)
        binding.swipe.setOnRefreshListener { cargar() }
        binding.estado.vincular(binding.desplazar)
        cargar()
    }

    private fun cargar() {
        if (!binding.swipe.isRefreshing) binding.estado.mostrarCargando()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.detalleSolicitud(id)
                if (_binding == null) return@launch
                binding.swipe.isRefreshing = false
                val d = r.body()
                if (!r.isSuccessful || d == null) {
                    binding.estado.mostrarError(RetrofitClient.mensajeDeError(r, getString(R.string.cont_error_cargar))) { cargar() }
                    return@launch
                }
                pintar(d)
                binding.estado.mostrarContenido()
            } catch (e: Exception) {
                if (_binding == null) return@launch
                binding.swipe.isRefreshing = false
                binding.estado.mostrarError(RetrofitClient.mensajeDeExcepcion(e)) { cargar() }
            }
        }
    }

    private fun pintar(d: DetalleSolicitudDTO) {
        val s = d.solicitud
        val c = binding.contenido
        c.removeAllViews()
        binding.encabezado.ponerInfo(TextosContables.tipo(this, s.tipo) + " · " + getString(R.string.cont_version, s.version))

        // Encabezado: qué es, cómo va y quién la mandó
        val cab = Bloques.tarjeta(c, s.resumen ?: TextosContables.tipo(this, s.tipo), detalle = TextosContables.quien(this, s),
            etiqueta = TextosContables.estado(this, s))
        if (s.edicion && s.estado == "PENDIENTE") Bloques.parrafo(cab, getString(R.string.cont_es_edicion))
        s.revisadaEn?.let { Bloques.parrafo(cab, getString(R.string.cont_revisada_por, s.revisadaPor.orEmpty(), Formatos.fecha(it))) }
        if (s.estado == "RECHAZADA" && s.motivoRechazo != null) {
            Bloques.parrafo(cab, getString(R.string.cont_motivo_linea, s.motivoRechazo), apagado = false)
        }

        // Lo que pide (y lo aprobado antes, para comparar una edición)
        Bloques.seccion(c, getString(if (s.edicion) R.string.cont_lo_que_pide_ahora else R.string.cont_lo_que_pide))
        val pide = Bloques.tarjeta(c, TextosContables.tipo(this, s.tipo))
        d.lineas.forEach { Bloques.dato(pide, it.etiqueta, it.valor) }
        s.nota?.let { Bloques.dato(pide, getString(R.string.cont_nota_del_contador), it) }
        if (d.lineasAprobadas.isNotEmpty()) {
            Bloques.seccion(c, getString(R.string.cont_lo_aprobado_antes))
            val antes = Bloques.tarjeta(c, getString(R.string.cont_version_aprobada))
            d.lineasAprobadas.forEach { Bloques.dato(antes, it.etiqueta, it.valor) }
        }

        // Vista previa de la facturación
        d.vistaPrevia?.let { v ->
            Bloques.seccion(c, getString(R.string.cont_vista_previa))
            Bloques.cifras(c, v.apartamentos.toString() to getString(R.string.cont_aptos),
                Formatos.moneda(v.total) to getString(R.string.cont_total))
            if (v.minimo != null && v.maximo != null && v.minimo != v.maximo) {
                Bloques.parrafo(c, getString(R.string.cont_rango, Formatos.moneda(v.minimo), Formatos.moneda(v.maximo)))
            }
            v.avisos.forEach { Bloques.parrafo(c, "• $it") }
            if (v.valores.isNotEmpty()) {
                val lista = Bloques.tarjeta(c, getString(R.string.cont_por_apartamento))
                v.valores.take(MAXIMO_VALORES).forEach { Bloques.dato(lista, getString(R.string.cont_torre_apto, it.torre, it.apto), Formatos.moneda(it.valor)) }
                if (v.valores.size > MAXIMO_VALORES) Bloques.parrafo(lista, getString(R.string.cont_y_mas, v.valores.size - MAXIMO_VALORES))
            }
        }

        // Acciones
        if (d.puedeRevisar) {
            Bloques.seccion(c, getString(R.string.cont_tu_decision))
            Bloques.boton(c, getString(R.string.cont_aprobar), icono = R.drawable.ic_check_circle) {
                confirmar(getString(R.string.cont_aprobar_titulo), getString(R.string.cont_aprobar_mensaje), R.string.cont_aprobar) { revisar(true, null) }
            }
            Bloques.boton(c, getString(R.string.cont_devolver), relleno = false, icono = R.drawable.ic_close) { devolver() }
        }
        if (d.puedeEditar && d.detalle != null) {
            Bloques.seccion(c, getString(R.string.cont_que_hacer))
            Bloques.boton(c, getString(when (s.estado) {
                "APROBADA" -> R.string.cont_editar_aprobada
                "RECHAZADA" -> R.string.cont_corregir_reenviar
                else -> R.string.cont_corregir
            }), icono = R.drawable.ic_edit) { FormulariosCartera.editar(this, s, d.detalle) { cargar() } }
            if (s.estado == "APROBADA") Bloques.parrafo(c, getString(R.string.cont_editar_aprobada_ayuda))
        }

        // Historial
        Bloques.seccion(c, getString(R.string.cont_historial))
        d.historial.asReversed().forEach { p ->
            val paso = Bloques.tarjeta(c, TextosContables.accion(this, p.accion) + " · " + getString(R.string.cont_version, p.version),
                detalle = getString(R.string.cont_por, p.autor.orEmpty(), Formatos.fecha(p.cuando)))
            p.motivo?.let { Bloques.parrafo(paso, getString(R.string.cont_motivo_linea, it), apagado = false) }
            if (p.lineas.isNotEmpty() && p.accion != "ENVIADA") {
                p.lineas.forEach { Bloques.dato(paso, it.etiqueta, it.valor) }
            }
        }
    }

    private fun devolver() {
        val caja = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            val dp = resources.displayMetrics.density
            setPadding((24 * dp).toInt(), (4 * dp).toInt(), (24 * dp).toInt(), 0)
        }
        val motivo = Bloques.campo(caja, getString(R.string.cont_motivo_devolver),
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_MULTI_LINE,
            getString(R.string.cont_motivo_devolver_ayuda), maximo = 1000)
        val dialogo = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.cont_devolver_titulo)
            .setView(caja)
            .setPositiveButton(R.string.cont_devolver, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
        dialogo.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (motivo.textoLimpio.isEmpty()) { motivo.error = getString(R.string.cont_falta_motivo); return@setOnClickListener }
            dialogo.dismiss()
            revisar(false, motivo.textoLimpio)
        }
    }

    private fun revisar(aprobada: Boolean, motivo: String?) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.revisarSolicitud(id, RevisionSolicitud(aprobada, motivo))
                if (_binding == null) return@launch
                if (r.isSuccessful) {
                    aviso(if (aprobada) R.string.cont_aprobada_ok else R.string.cont_devuelta_ok).show()
                    cargar()
                } else aviso(RetrofitClient.mensajeDeError(r, getString(R.string.cont_error_enviar))).show()
            } catch (e: Exception) {
                if (_binding != null) aviso(RetrofitClient.mensajeDeExcepcion(e)).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val MAXIMO_VALORES = 40
    }
}
