package com.example.conjuntoresidencial.ui.plataforma

import com.example.conjuntoresidencial.ui.comun.ponerCodigo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.api.instance
import com.example.conjuntoresidencial.databinding.FragmentConjuntoDetalleBinding
import com.example.conjuntoresidencial.databinding.ItemCifraPlataformaBinding
import com.example.conjuntoresidencial.databinding.ItemTextoConAccionBinding
import com.example.conjuntoresidencial.model.DetalleConjunto
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.ui.comun.ponerTitulo
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.launch

/**
 * P8 bloque 6: un conjunto visto desde la plataforma. Si está activo, cuántos apartamentos,
 * residentes (propietarios, arrendatarios y por aprobar), vehículos, guardias, celulares con
 * sesión, visitas del mes y parqueaderos tiene; sus datos y su límite de celulares; sus cuentas
 * de administración; editar, restablecer el acceso del principal y suspender o reactivar; y lo
 * último que la plataforma hizo con él. No muestra datos de los residentes.
 */
class ConjuntoDetalleFragment : Fragment() {

    private var _binding: FragmentConjuntoDetalleBinding? = null
    private val binding get() = _binding!!
    private val conjuntoId get() = requireArguments().getLong(ARG_CONJUNTO)
    private var detalle: DetalleConjunto? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentConjuntoDetalleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(binding.encabezado, getString(R.string.plataforma_conjuntos_titulo))
        binding.swipe.setOnRefreshListener { cargar() }
        binding.btnEditar.setOnClickListener { detalle?.let { formularioConjunto(it.conjunto) { cargar() } } }
        binding.btnRestablecer.setOnClickListener { detalle?.let { restablecerPrincipal(it.conjunto) { cargar() } } }
        binding.btnEstado.setOnClickListener { detalle?.let { cambiarEstadoConjunto(it.conjunto) { cargar() } } }
        binding.btnVerAuditoria.setOnClickListener {
            val nombre = detalle?.conjunto?.nombre.orEmpty()
            findNavController().navigate(R.id.auditoriaPlataformaFragment,
                bundleOf(AuditoriaPlataformaFragment.ARG_CONJUNTO to conjuntoId, AuditoriaPlataformaFragment.ARG_TITULO to nombre))
        }
        binding.swipe.isRefreshing = true
        cargar()
    }

    private fun cargar() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.detalle(conjuntoId)
                val d = r.body()
                if (_binding == null) return@launch
                if (r.isSuccessful && d != null) pintar(d)
                else aviso(RetrofitClient.mensajeDeError(r, getString(R.string.plataforma_error_cargar))).show()
            } catch (e: Exception) {
                if (_binding != null) aviso(RetrofitClient.mensajeDeExcepcion(e)).show()
            }
            _binding?.swipe?.isRefreshing = false
        }
    }

    private fun pintar(d: DetalleConjunto) {
        detalle = d
        val c = d.conjunto
        binding.contenido.isVisible = true
        binding.encabezado.ponerTitulo(c.nombre)
        val estado = getString(if (c.activo) R.string.detalle_activo else R.string.detalle_suspendido)
        binding.encabezado.ponerInfo(getString(R.string.detalle_info, c.municipio, c.departamento, estado))

        // La etiqueta del estado: verde activo, rojo suspendido
        val fondo = if (c.activo) com.google.android.material.R.attr.colorSecondaryContainer else com.google.android.material.R.attr.colorErrorContainer
        val texto = if (c.activo) com.google.android.material.R.attr.colorOnSecondaryContainer else com.google.android.material.R.attr.colorOnErrorContainer
        binding.tvEstado.text = estado
        binding.tvEstado.background?.mutate()?.setTint(MaterialColors.getColor(binding.tvEstado, fondo))
        binding.tvEstado.setTextColor(MaterialColors.getColor(binding.tvEstado, texto))

        // P9 bloque 2: el código de hoy, por si un administrador lo pide
        binding.codigoConjunto.root.isVisible = d.codigoDelDia != null
        d.codigoDelDia?.let {
            binding.codigoConjunto.ponerCodigo(it)
        }

        val f = d.cifras
        filaCifras(binding.filaCifras1, listOf(
            Triple(R.drawable.ic_home, R.string.detalle_apartamentos, f.apartamentos),
            Triple(R.drawable.ic_key, R.string.detalle_propietarios, f.propietarios),
            Triple(R.drawable.ic_person, R.string.detalle_arrendatarios, f.arrendatarios)))
        filaCifras(binding.filaCifras2, listOf(
            Triple(R.drawable.ic_people, R.string.detalle_pendientes, f.residentesPendientes),
            Triple(R.drawable.ic_car, R.string.detalle_vehiculos, f.vehiculos),
            Triple(R.drawable.ic_place, R.string.detalle_parqueaderos, f.parqueaderos)))
        filaCifras(binding.filaCifras3, listOf(
            Triple(R.drawable.ic_security, R.string.detalle_guardias, f.guardias),
            Triple(R.drawable.ic_smartphone, R.string.detalle_celulares, f.celularesConSesion),
            Triple(R.drawable.ic_event, R.string.detalle_visitas, f.visitasMes)))

        binding.tvDatos.text = listOfNotNull(
            c.direccion?.takeIf { it.isNotBlank() }?.let { getString(R.string.detalle_direccion, it) },
            c.creadoEn?.let { getString(R.string.detalle_creado, fechaEvento(it).ifBlank { it.take(10) }) },
            c.maxDispositivos?.let { getString(R.string.detalle_limite, it) },
            d.ultimaSesion?.let { getString(R.string.detalle_ultima_sesion, fechaEvento(it)) } ?: getString(R.string.detalle_sin_sesiones)
        ).joinToString("\n")

        binding.btnEstado.setText(if (c.activo) R.string.plataforma_suspender else R.string.plataforma_reactivar)
        binding.btnRestablecer.isEnabled = d.cuentas.any { it.activo && it.rol == "ADMIN" }

        binding.tvSinCuentas.isVisible = d.cuentas.isEmpty()
        binding.listaCuentas.removeAllViews()
        d.cuentas.forEach { cuenta ->
            val fila = ItemTextoConAccionBinding.inflate(layoutInflater, binding.listaCuentas, false)
            fila.tvEtiqueta.isVisible = true
            fila.tvEtiqueta.text = getString(when {
                cuenta.rol == "CONTADOR" -> R.string.rol_contador
                cuenta.rol == "PORTERIA" -> R.string.rol_porteria
                cuenta.principal -> R.string.equipo_principal
                else -> R.string.rol_asistente
            })
            fila.tvTitulo.text = cuenta.nombreCompleto ?: cuenta.usuario
            fila.tvDetalle.text = cuenta.usuario + if (cuenta.activo) "" else " · " + getString(R.string.plataforma_de_baja)
            fila.root.alpha = if (cuenta.activo) 1f else 0.6f
            binding.listaCuentas.addView(fila.root)
        }

        binding.listaEventos.removeAllViews()
        d.eventos.forEach { e -> binding.listaEventos.addView(filaEvento(binding.listaEventos, e, conConjunto = false)) }
    }

    private fun filaCifras(fila: LinearLayout, cifras: List<Triple<Int, Int, Long>>) {
        fila.removeAllViews()
        val separacion = (4 * resources.displayMetrics.density).toInt()
        cifras.forEachIndexed { i, (icono, nombre, valor) ->
            val c = ItemCifraPlataformaBinding.inflate(layoutInflater, fila, false)
            c.poner(icono, nombre)
            c.valor(valor)
            c.root.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                if (i > 0) marginStart = separacion
                if (i < cifras.size - 1) marginEnd = separacion
            }
            fila.addView(c.root)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_CONJUNTO = "conjuntoId"
    }
}
