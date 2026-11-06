package com.example.conjuntoresidencial.ui.plataforma

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.api.instance
import androidx.core.view.isVisible
import com.example.conjuntoresidencial.databinding.FragmentConjuntosPlataformaBinding
import com.example.conjuntoresidencial.databinding.ItemTextoConAccionBinding
import com.example.conjuntoresidencial.model.ConjuntoPlataforma
import com.example.conjuntoresidencial.ui.comun.DiffPorId
import com.example.conjuntoresidencial.ui.comun.accionesDeLista
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.controlesFlotantes
import com.example.conjuntoresidencial.ui.comun.mostrarRecurso
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.util.Resource
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.util.Locale

/**
 * P7: los conjuntos de la plataforma. P8 bloque 6: se buscan por nombre, municipio, departamento
 * o administrador principal y se filtran por estado; tocar uno abre su detalle (cifras, cuentas,
 * acciones y auditoría). "+" crea uno nuevo con su administrador principal.
 */
class ConjuntosPlataformaFragment : Fragment() {

    private var _binding: FragmentConjuntosPlataformaBinding? = null
    private val binding get() = _binding!!
    private val adaptador = ConjuntoAdapter { abrir(it) }
    private var todos: List<ConjuntoPlataforma> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentConjuntosPlataformaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(binding.encabezado, R.string.plataforma_conjuntos_titulo, R.string.plataforma_conjuntos_descripcion,
            menu = R.menu.menu_lista, alElegir = accionesDeLista(binding.swipe) { cargar() })
        binding.lista.layoutManager = LinearLayoutManager(requireContext())
        binding.lista.adapter = adaptador
        binding.estado.vincular(binding.lista)
        binding.swipe.setOnRefreshListener { cargar() }
        binding.etBuscar.doAfterTextChanged { filtrar() }
        binding.chipsEstado.setOnCheckedStateChangeListener { _, _ -> filtrar() }
        // P10: desde las cifras del panel llega con el filtro puesto
        if (savedInstanceState == null) {
            when (arguments?.getString(ARG_FILTRO)) {
                FILTRO_ACTIVOS -> binding.chipsEstado.check(R.id.chipActivos)
                FILTRO_SUSPENDIDOS -> binding.chipsEstado.check(R.id.chipSuspendidos)
            }
            arguments?.getString(ARG_BUSQUEDA)?.let { binding.etBuscar.setText(it) }
        }
        controlesFlotantes?.mostrarBotonFlotante(R.string.plataforma_nuevo, R.drawable.ic_add) { formularioConjunto(null) { cargar() } }
        cargar()
    }

    private fun cargar() {
        viewLifecycleOwner.lifecycleScope.launch {
            val resource = try {
                val r = RetrofitClient.instance.conjuntos()
                if (r.isSuccessful) Resource.Success(r.body().orEmpty())
                else Resource.Error(RetrofitClient.mensajeDeError(r, getString(R.string.plataforma_error_cargar)))
            } catch (e: Exception) {
                Resource.Error(RetrofitClient.mensajeDeExcepcion(e))
            }
            if (_binding == null) return@launch
            binding.estado.mostrarRecurso(resource, binding.swipe, R.drawable.ic_apartment,
                getString(R.string.plataforma_vacio), reintentar = { cargar() }) { lista ->
                todos = lista
                filtrar()
                val suspendidos = lista.count { !it.activo }
                binding.encabezado.ponerInfo((listOf(resources.getQuantityString(R.plurals.plataforma_resumen_conjuntos, lista.size, lista.size)) +
                    if (suspendidos > 0) listOf(resources.getQuantityString(R.plurals.plataforma_resumen_suspendidos, suspendidos, suspendidos))
                    else emptyList()).joinToString(" · "))
            }
            if (resource is Resource.Error) binding.encabezado.ponerInfo(null)
            // mostrarRecurso deja la lista a la vista: si el filtro no deja nada, se avisa
            if (resource is Resource.Success) filtrar()
        }
    }

    private fun filtrar() {
        val q = normal(binding.etBuscar.text?.toString().orEmpty().trim())
        val estado = binding.chipsEstado.checkedChipId
        val lista = todos.filter { c ->
            (estado != R.id.chipActivos || c.activo) && (estado != R.id.chipSuspendidos || !c.activo) &&
                (q.isEmpty() || listOfNotNull(c.nombre, c.municipio, c.departamento, c.principal, c.direccion).any { normal(it).contains(q) })
        }
        adaptador.submitList(lista)
        // Sin ningún conjunto lo muestra mostrarRecurso; aquí, cuando el filtro no deja nada
        if (todos.isEmpty()) return
        if (lista.isEmpty()) binding.estado.mostrarVacio(R.drawable.ic_search, getString(R.string.conjuntos_sin_resultados))
        else binding.estado.mostrarContenido()
    }

    private fun abrir(c: ConjuntoPlataforma) {
        findNavController().navigate(R.id.conjuntoDetalleFragment, bundleOf(ConjuntoDetalleFragment.ARG_CONJUNTO to c.id))
    }

    private fun normal(s: String) = Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase(Locale.ROOT)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_FILTRO = "filtro"
        const val ARG_BUSQUEDA = "busqueda"
        const val FILTRO_ACTIVOS = "activos"
        const val FILTRO_SUSPENDIDOS = "suspendidos"
    }

    /** P10: cada conjunto en una cápsula redondeada, separada de las demás. */
    private class ConjuntoAdapter(private val onTocar: (ConjuntoPlataforma) -> Unit) :
        ListAdapter<ConjuntoPlataforma, ConjuntoAdapter.VH>(DiffPorId { it.id }) {
        class VH(val b: ItemTextoConAccionBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemTextoConAccionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                .conIcono(R.drawable.ic_apartment, tocable = true))

        override fun onBindViewHolder(h: VH, position: Int) {
            val c = getItem(position)
            val contexto = h.itemView.context
            h.b.tvTitulo.text = c.nombre
            h.b.tvEtiqueta.isVisible = !c.activo
            h.b.tvEtiqueta.setText(R.string.plataforma_estado_suspendido)
            val apartamentos = contexto.resources.getQuantityString(R.plurals.plataforma_apartamentos, c.apartamentos.toInt(), c.apartamentos.toInt())
            h.b.tvDetalle.text = contexto.getString(R.string.plataforma_fila_detalle, c.municipio, c.departamento, apartamentos,
                c.principal ?: contexto.getString(R.string.plataforma_sin_principal))
            h.itemView.alpha = if (c.activo) 1f else 0.7f
            h.itemView.setOnClickListener { onTocar(c) }
        }
    }
}
