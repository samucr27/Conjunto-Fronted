package com.example.conjuntoresidencial.ui.plataforma

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.api.instance
import com.example.conjuntoresidencial.databinding.FragmentListaPlataformaBinding
import com.example.conjuntoresidencial.model.EventoPlataforma
import com.example.conjuntoresidencial.ui.comun.DiffPorId
import com.example.conjuntoresidencial.ui.comun.accionesDeLista
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.mostrarRecurso
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.util.Formatos
import com.example.conjuntoresidencial.util.Resource
import kotlinx.coroutines.launch

/** P7: lo que hizo cada operador de la plataforma (los 200 más recientes), de todos o de un conjunto. */
class AuditoriaPlataformaFragment : Fragment() {

    private var _binding: FragmentListaPlataformaBinding? = null
    private val binding get() = _binding!!
    private val adaptador = EventoAdapter()
    private val conjunto get() = arguments?.getLong(ARG_CONJUNTO)?.takeIf { it > 0 }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentListaPlataformaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val titulo = arguments?.getString(ARG_TITULO).orEmpty().ifBlank { getString(R.string.plataforma_auditoria) }
        configurarEncabezado(binding.encabezado, titulo, R.string.plataforma_auditoria_descripcion,
            menu = R.menu.menu_lista, alElegir = accionesDeLista(binding.swipe) { cargar() })
        binding.lista.layoutManager = LinearLayoutManager(requireContext())
        binding.lista.adapter = adaptador
        binding.estado.vincular(binding.lista)
        binding.swipe.setOnRefreshListener { cargar() }
        cargar()
    }

    private fun cargar() {
        viewLifecycleOwner.lifecycleScope.launch {
            val resource = try {
                val r = RetrofitClient.instance.auditoria(conjunto)
                if (r.isSuccessful) Resource.Success(r.body().orEmpty())
                else Resource.Error(RetrofitClient.mensajeDeError(r, getString(R.string.plataforma_error_cargar)))
            } catch (e: Exception) {
                Resource.Error(RetrofitClient.mensajeDeExcepcion(e))
            }
            if (_binding == null) return@launch
            binding.estado.mostrarRecurso(resource, binding.swipe, R.drawable.ic_description,
                getString(R.string.plataforma_auditoria_vacia), reintentar = { cargar() }) { lista ->
                adaptador.submitList(lista)
                binding.encabezado.ponerInfo(resources.getQuantityString(R.plurals.plataforma_resumen_eventos, lista.size, lista.size))
            }
            if (resource is Resource.Error) binding.encabezado.ponerInfo(null)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class EventoAdapter : ListAdapter<EventoPlataforma, EventoAdapter.VH>(DiffPorId { it.id }) {
        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val accion: TextView = v.findViewById(android.R.id.text1)
            val detalle: TextView = v.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false))

        override fun onBindViewHolder(h: VH, position: Int) {
            val e = getItem(position)
            val contexto = h.itemView.context
            h.accion.text = contexto.getString(when (e.accion) {
                "INICIO_SESION" -> R.string.plataforma_evento_inicio
                "CREAR_CONJUNTO" -> R.string.plataforma_evento_crear
                "EDITAR_CONJUNTO" -> R.string.plataforma_evento_editar
                "SUSPENDER_CONJUNTO" -> R.string.plataforma_evento_suspender
                "REACTIVAR_CONJUNTO" -> R.string.plataforma_evento_reactivar
                "RESTABLECER_PRINCIPAL" -> R.string.plataforma_evento_restablecer
                else -> R.string.plataforma_evento_otro
            })
            // El servidor manda la hora de Colombia sin zona: "2026-11-03T08:39:12"
            val fecha = Formatos.fechaHora(e.fecha.take(10), e.fecha.substringAfter('T', ""))
            h.detalle.text = listOfNotNull(fecha, e.operador, e.detalle?.takeIf { it.isNotBlank() }).joinToString(" · ")
        }
    }

    companion object {
        const val ARG_CONJUNTO = "conjuntoId"
        const val ARG_TITULO = "titulo"
    }
}
