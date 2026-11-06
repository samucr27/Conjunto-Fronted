package com.example.conjuntoresidencial.ui.plataforma

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.api.instance
import com.example.conjuntoresidencial.databinding.FragmentPanelPlataformaBinding
import com.example.conjuntoresidencial.databinding.ItemTextoConAccionBinding
import com.example.conjuntoresidencial.model.PanelPlataforma
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.controlesFlotantes
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import kotlinx.coroutines.launch

/**
 * P8 bloque 6: lo primero que ve el operador. Cuántos conjuntos hay (activos, suspendidos, nuevos
 * del mes), cuántos apartamentos, residentes, celulares con sesión y visitas tienen entre todos,
 * los conjuntos que hay que revisar (sin administrador activo), cuántos por departamento y lo
 * último de la auditoría. Desde aquí se abren los conjuntos y la auditoría.
 */
class PanelPlataformaFragment : Fragment() {

    private var _binding: FragmentPanelPlataformaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPanelPlataformaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(binding.encabezado, R.string.panel_titulo, R.string.panel_descripcion,
            atras = false, menu = R.menu.menu_panel_plataforma) { id ->
            when (id) {
                R.id.accion_actualizar -> { binding.swipe.isRefreshing = true; cargar(); true }
                R.id.accion_contrasena -> { cambiarContrasenaPlataforma(); true }
                R.id.accion_cerrar_sesion -> { confirmarSalirPlataforma(); true }
                else -> false
            }
        }
        binding.cifraConjuntos.poner(R.drawable.ic_apartment, R.string.panel_conjuntos_activos)
        binding.cifraSuspendidos.poner(R.drawable.ic_lock, R.string.panel_conjuntos_suspendidos)
        binding.cifraApartamentos.poner(R.drawable.ic_home, R.string.panel_apartamentos)
        binding.cifraResidentes.poner(R.drawable.ic_people, R.string.panel_residentes)
        binding.cifraCelulares.poner(R.drawable.ic_smartphone, R.string.panel_celulares)
        binding.cifraVisitas.poner(R.drawable.ic_security, R.string.panel_visitas)
        // P10: cada cifra lleva a su pantalla (los conjuntos con el filtro que corresponde)
        binding.cifraConjuntos.alTocar { verConjuntos(ConjuntosPlataformaFragment.FILTRO_ACTIVOS) }
        binding.cifraSuspendidos.alTocar { verConjuntos(ConjuntosPlataformaFragment.FILTRO_SUSPENDIDOS) }
        binding.cifraApartamentos.alTocar { verConjuntos(null) }
        binding.cifraResidentes.alTocar { verConjuntos(null) }
        binding.cifraCelulares.alTocar { verConjuntos(null) }
        binding.cifraVisitas.alTocar { verConjuntos(null) }
        binding.swipe.setOnRefreshListener { cargar() }
        controlesFlotantes?.mostrarBotonFlotante(R.string.plataforma_nuevo, R.drawable.ic_add) { formularioConjunto(null) { cargar() } }
        // Es la primera pantalla: "atrás" pregunta si cerrar la sesión
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { confirmarSalirPlataforma() }
        cargar()
    }

    private fun cargar() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.panel()
                val panel = r.body()
                if (_binding == null) return@launch
                if (r.isSuccessful && panel != null) pintar(panel)
                else aviso(RetrofitClient.mensajeDeError(r, getString(R.string.plataforma_error_cargar))).show()
            } catch (e: Exception) {
                if (_binding != null) aviso(RetrofitClient.mensajeDeExcepcion(e)).show()
            }
            _binding?.swipe?.isRefreshing = false
        }
    }

    private fun pintar(p: PanelPlataforma) {
        val total = p.conjuntosActivos + p.conjuntosSuspendidos
        binding.encabezado.ponerInfo(resources.getQuantityString(R.plurals.plataforma_resumen_conjuntos, total.toInt(), total.toInt()))
        binding.cifraConjuntos.valor(p.conjuntosActivos,
            getString(R.string.panel_nuevos_mes, p.conjuntosNuevosMes).takeIf { p.conjuntosNuevosMes > 0 })
        binding.cifraSuspendidos.valor(p.conjuntosSuspendidos, getString(R.string.panel_suspendidos_detalle))
        binding.cifraApartamentos.valor(p.apartamentos, getString(R.string.panel_apartamentos_detalle))
        binding.cifraResidentes.valor(p.residentes, getString(R.string.panel_residentes_detalle))
        binding.cifraCelulares.valor(p.celularesConSesion, getString(R.string.panel_cuentas, p.cuentasAdministracion))
        binding.cifraVisitas.valor(p.visitasSemana, getString(R.string.panel_visitas_detalle))

        binding.seccionPendientes.isVisible = p.pendientes.isNotEmpty()
        binding.listaPendientes.removeAllViews()
        p.pendientes.forEach { x ->
            val fila = ItemTextoConAccionBinding.inflate(layoutInflater, binding.listaPendientes, false)
                .conIcono(R.drawable.ic_error_outline)
            fila.root.setOnClickListener { abrirConjunto(x.conjuntoId) }
            fila.tvTitulo.text = x.nombre
            fila.tvDetalle.text = "${x.municipio} · ${x.motivo}"
            fila.btnAccion.isVisible = true
            fila.btnAccion.setText(R.string.panel_abrir)
            fila.btnAccion.setOnClickListener { abrirConjunto(x.conjuntoId) }
            binding.listaPendientes.addView(fila.root)
        }

        binding.listaDepartamentos.removeAllViews()
        p.porDepartamento.forEach { d ->
            val fila = ItemTextoConAccionBinding.inflate(layoutInflater, binding.listaDepartamentos, false)
                .conIcono(R.drawable.ic_place, tocable = true)
            fila.root.setOnClickListener { verConjuntos(null, d.departamento) }
            fila.tvTitulo.text = d.departamento
            fila.tvDetalle.text = getString(R.string.panel_departamento_detalle, d.activos, d.conjuntos)
            binding.listaDepartamentos.addView(fila.root)
        }

        binding.tvSinActividad.isVisible = p.ultimosEventos.isEmpty()
        binding.listaActividad.removeAllViews()
        p.ultimosEventos.forEach { e ->
            binding.listaActividad.addView(filaEvento(binding.listaActividad, e, conConjunto = true))
        }
    }

    /** P10: la pestaña de conjuntos con un filtro de estado o una búsqueda ya puestos. */
    private fun verConjuntos(filtro: String?, busqueda: String? = null) {
        findNavController().navigate(R.id.conjuntosPlataformaFragment, bundleOf(
            ConjuntosPlataformaFragment.ARG_FILTRO to filtro, ConjuntosPlataformaFragment.ARG_BUSQUEDA to busqueda))
    }

    private fun abrirConjunto(id: Long) {
        findNavController().navigate(R.id.conjuntoDetalleFragment, bundleOf(ConjuntoDetalleFragment.ARG_CONJUNTO to id))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
