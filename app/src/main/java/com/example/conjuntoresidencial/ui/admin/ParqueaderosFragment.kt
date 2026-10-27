package com.example.conjuntoresidencial.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.ui.comun.debajoDeLosBotones
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.FragmentParqueaderosBinding
import com.example.conjuntoresidencial.databinding.ItemCuotaBinding
import com.example.conjuntoresidencial.model.AsignacionParqueadero
import com.example.conjuntoresidencial.model.GenerarParqueaderos
import com.example.conjuntoresidencial.model.ParqueaderoDTO
import com.example.conjuntoresidencial.ui.comun.*
import com.example.conjuntoresidencial.util.Opciones
import com.example.conjuntoresidencial.util.Resource
import com.example.conjuntoresidencial.util.comoLista
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Parqueaderos del conjunto (administración, P5): métricas (libres, asignados, invitados),
 * filtros, crear en lote y, al tocar uno, asignarlo o reasignarlo, liberarlo o eliminarlo.
 */
class ParqueaderosFragment : Fragment() {
    private var _binding: FragmentParqueaderosBinding? = null
    private val binding get() = _binding!!
    private val adaptador = ParqueaderoAdapter { opciones(it) }
    private var filtro: String? = null // null = todos; LIBRE, ASIGNADO o un tipo
    private var todos: List<ParqueaderoDTO> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentParqueaderosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(binding.encabezado, R.string.parqueaderos_titulo, R.string.parqueaderos_descripcion,
            menu = R.menu.menu_lista, alElegir = accionesDeLista(binding.swipe) { cargar() })
        binding.rvParqueaderos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvParqueaderos.adapter = adaptador
        binding.estado.vincular(binding.rvParqueaderos)
        binding.swipe.setOnRefreshListener { cargar() }
        // La lista no es hija directa del swipe: solo se actualiza al jalar desde arriba de la lista
        binding.swipe.setOnChildScrollUpCallback { _, _ -> binding.rvParqueaderos.canScrollVertically(-1) }
        debajoDeLosBotones(binding.contenidoParqueaderos)
        controlesFlotantes?.mostrarBotonFlotante(R.string.parqueaderos_crear, R.drawable.ic_add) { crear() }
        cargar()
    }

    private fun cargar() {
        if (!binding.swipe.isRefreshing) binding.estado.mostrarCargando()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val (lista, resumen) = coroutineScope {
                    val l = async { RetrofitClient.instance.getParqueaderos(null, null) }
                    val r = async { RetrofitClient.instance.getResumenParqueaderos() }
                    l.await() to r.await()
                }
                if (_binding == null) return@launch
                binding.swipe.isRefreshing = false
                if (!lista.isSuccessful) {
                    binding.estado.mostrarError(RetrofitClient.mensajeDeError(lista, getString(R.string.parqueaderos_error))) { cargar() }
                    return@launch
                }
                todos = lista.body().orEmpty()
                resumen.body()?.let { r ->
                    binding.tvLibres.text = r.libres.toString()
                    binding.tvAsignados.text = r.asignados.toString()
                    binding.tvInvitados.text = r.invitados.toString()
                    binding.encabezado.ponerInfo(resources.getQuantityString(R.plurals.resumen_parqueaderos, r.total + r.invitados, r.total + r.invitados))
                }
                ponerFiltros()
                mostrar()
            } catch (e: Exception) {
                if (_binding == null) return@launch
                binding.swipe.isRefreshing = false
                binding.estado.mostrarError(RetrofitClient.mensajeDeExcepcion(e)) { cargar() }
            }
        }
    }

    private fun ponerFiltros() {
        val opciones = listOf<Pair<String, String?>>(getString(R.string.parqueaderos_todos) to null,
            getString(R.string.parqueaderos_libres) to "LIBRE", getString(R.string.parqueaderos_asignados) to "ASIGNADO") +
            todos.map { it.tipo }.distinct().map { it to it }
        binding.chipsFiltro.removeAllViews()
        opciones.forEach { (texto, valor) ->
            binding.chipsFiltro.addView(Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle).apply {
                text = texto
                isCheckable = true
                isChecked = valor == filtro
                setOnClickListener { filtro = valor; mostrar(); ponerFiltros() }
            })
        }
    }

    private fun mostrar() {
        val lista = todos.filter { p -> filtro == null || p.estado == filtro || p.tipo == filtro }
        binding.estado.mostrarRecurso(Resource.Success(lista), binding.swipe, R.drawable.ic_car,
            getString(if (todos.isEmpty()) R.string.parqueaderos_vacio else R.string.parqueaderos_vacio_filtro),
            if (todos.isEmpty()) getString(R.string.parqueaderos_vacio_mensaje) else null, reintentar = { cargar() }) {
            adaptador.submitList(it)
        }
    }

    private fun opciones(p: ParqueaderoDTO) {
        val opciones = mutableListOf<Pair<Int, () -> Unit>>()
        if (p.tipo != "Invitados") {
            opciones += (if (p.estado == "LIBRE") R.string.parqueadero_asignar else R.string.parqueadero_reasignar) to { reasignar(p) }
        }
        if (p.estado == "ASIGNADO") opciones += R.string.parqueadero_liberar to {
            confirmar(getString(R.string.parqueadero_liberar_titulo, p.numero), null, R.string.parqueadero_liberar) {
                ejecutarAccion(getString(R.string.parqueadero_liberado),
                    { RetrofitClient.instance.asignarParqueadero(p.id, AsignacionParqueadero(null)) }) { cargar() }
            }
        } else opciones += R.string.residente_eliminar_si to {
            confirmar(getString(R.string.parqueadero_eliminar_titulo, p.numero), null, R.string.residente_eliminar_si) {
                ejecutarAccion(getString(R.string.parqueadero_eliminado), { RetrofitClient.instance.eliminarParqueadero(p.id) }) { cargar() }
            }
        }
        elegirOpcion("${p.numero} · ${p.tipo}", opciones)
    }

    /** A otro apartamento: los vehículos aprobados de esa categoría que aún no tienen parqueadero. */
    private fun reasignar(p: ParqueaderoDTO) {
        viewLifecycleOwner.lifecycleScope.launch {
            val candidatos = try {
                RetrofitClient.instance.getVehiculosPorEstado("APROBADO").body().orEmpty()
                    .filter { it.tipo == p.tipo && it.parqueaderoId == null }
            } catch (_: Exception) {
                emptyList()
            }
            if (view == null) return@launch
            if (candidatos.isEmpty()) {
                aviso(getString(R.string.parqueadero_sin_candidatos, p.tipo.lowercase())).show()
                return@launch
            }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.parqueadero_asignar_titulo, p.numero))
                .setItems(candidatos.map { v ->
                    getString(R.string.apto_item_titulo, v.torre, v.apto) + " · " + (v.placa ?: v.marca ?: v.tipo)
                }.toTypedArray()) { _, i ->
                    val elegido = candidatos[i]
                    ejecutarAccion(getString(R.string.parqueadero_asignado, p.numero), {
                        // Si estaba ocupado, primero se libera
                        if (p.estado == "ASIGNADO") RetrofitClient.instance.asignarParqueadero(p.id, AsignacionParqueadero(null))
                        RetrofitClient.instance.asignarParqueadero(p.id, AsignacionParqueadero(elegido.id))
                    }) { cargar() }
                }
                .setNegativeButton(R.string.btn_cancelar, null)
                .show()
        }
    }

    private fun crear() {
        val margen = (24 * resources.displayMetrics.density).toInt()
        val tipos = Opciones.TIPOS_VEHICULO + "Invitados"
        val tipo = AutoCompleteTextView(requireContext()).apply { inputType = 0 }
        val tilTipo = TextInputLayout(requireContext(), null, com.google.android.material.R.attr.textInputOutlinedExposedDropdownMenuStyle).apply {
            hint = getString(R.string.parqueaderos_tipo)
            addView(tipo)
        }
        tipo.comoLista(tipos)
        val cantidad = TextInputEditText(requireContext()).apply { inputType = android.text.InputType.TYPE_CLASS_NUMBER }
        val tilCantidad = TextInputLayout(requireContext()).apply { hint = getString(R.string.parqueaderos_cantidad); addView(cantidad) }
        val prefijo = TextInputEditText(requireContext()).apply { inputType = android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS }
        val tilPrefijo = TextInputLayout(requireContext()).apply {
            hint = getString(R.string.parqueaderos_prefijo)
            helperText = getString(R.string.parqueaderos_prefijo_ayuda)
            addView(prefijo)
        }
        val caja = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(margen, margen / 2, margen, 0)
            listOf(tilTipo, tilCantidad, tilPrefijo).forEach { addView(it) }
        }
        val dialogo = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.parqueaderos_crear)
            .setView(caja)
            .setPositiveButton(R.string.parqueaderos_crear_si, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val n = cantidad.text?.toString()?.toIntOrNull()
            tilCantidad.error = if (n == null || n !in 1..200) getString(R.string.parqueaderos_cantidad_rango) else null
            if (tilCantidad.error != null) return@setOnClickListener
            dialogo.dismiss()
            val pedido = GenerarParqueaderos(tipo.text.toString(), n!!, prefijo.text?.toString()?.trim()?.ifBlank { null })
            ejecutarAccion(resources.getQuantityString(R.plurals.parqueaderos_creados, n, n),
                { RetrofitClient.instance.generarParqueaderos(pedido) }) { cargar() }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    /** Fila de un parqueadero (mismo diseño que las cuotas): número y tipo, a quién está asignado y su estado. */
    private class ParqueaderoAdapter(private val onTocar: (ParqueaderoDTO) -> Unit) :
        ListAdapter<ParqueaderoDTO, ParqueaderoAdapter.VH>(DiffPorId { it.id }) {
        class VH(val binding: ItemCuotaBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemCuotaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(h: VH, position: Int) {
            val p = getItem(position)
            val ctx = h.binding.root.context
            h.binding.tvTituloCuota.text = ctx.getString(R.string.parqueadero_titulo, p.numero, p.tipo)
            h.binding.tvDetalleCuota.text = if (p.estado == "ASIGNADO") listOfNotNull(
                ctx.getString(R.string.apto_item_titulo, p.torre, p.apto), p.placa, p.vehiculo
            ).joinToString(" · ") else ctx.getString(if (p.tipo == "Invitados") R.string.parqueadero_de_invitados else R.string.parqueadero_sin_asignar)
            h.binding.tvEstadoCuota.mostrarEstado(
                if (p.estado == "ASIGNADO") EstadoVisual(R.string.parqueadero_estado_asignado, TonoEstado.NEUTRO)
                else EstadoVisual(R.string.parqueadero_estado_libre, TonoEstado.EXITO))
            h.binding.root.setOnClickListener { onTocar(p) }
        }
    }
}
