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
import com.example.conjuntoresidencial.api.instance
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
    private lateinit var buscador: com.example.conjuntoresidencial.ui.comun.FiltroTorreApto

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentParqueaderosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(binding.encabezado, R.string.parqueaderos_titulo, R.string.parqueaderos_descripcion,
            menu = R.menu.menu_lista, alElegir = accionesDeLista(binding.swipe) { cargar() })
        binding.rvParqueaderos.layoutManager = LinearLayoutManager(requireContext())
        // P11: buscador por torre y después por apartamento, número o placa
        buscador = com.example.conjuntoresidencial.ui.comun.FiltroTorreApto(binding.filtro) { _, _ -> mostrar() }
        buscador.ponerPista(R.string.parqueaderos_buscar)
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
                buscador.ponerTorres(todos.mapNotNull { it.torre })
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
        val q = buscador.texto
        val torre = buscador.torre
        val lista = todos.filter { p -> filtro == null || p.estado == filtro || p.tipo == filtro }
            .filter { p -> torre == null || p.torre == torre }
            .filter { p ->
                q.isEmpty() || com.example.conjuntoresidencial.ui.comun.FiltroTorreApto.normalizar(p.numero).contains(q) ||
                    (p.apto != null && com.example.conjuntoresidencial.ui.comun.FiltroTorreApto.coincideApto(p.apto, q)) ||
                    com.example.conjuntoresidencial.ui.comun.FiltroTorreApto.normalizar("${p.torre}-${p.apto}").contains(q) ||
                    com.example.conjuntoresidencial.ui.comun.FiltroTorreApto.normalizar(p.placa.orEmpty()).contains(q)
            }
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

    /**
     * P11: a un apartamento (elegido con buscador). Si ese apartamento tiene vehículos aprobados de
     * esa categoría sin parqueadero, se puede vincular uno; el vehículo es opcional.
     */
    private fun reasignar(p: ParqueaderoDTO) {
        viewLifecycleOwner.lifecycleScope.launch {
            val aptos = try {
                RetrofitClient.instance.getApartamentos().takeIf { it.isSuccessful }?.body()
            } catch (_: Exception) {
                null
            }
            if (view == null) return@launch
            if (aptos == null) { aviso(R.string.parqueaderos_error).show(); return@launch }
            elegirApartamento(p, aptos.sortedWith(compareBy({ it.torre.padStart(4, '0') }, { it.apto.padStart(6, '0') })))
        }
    }

    private fun elegirApartamento(p: ParqueaderoDTO, aptos: List<com.example.conjuntoresidencial.model.Apartamento>) {
        val ctx = requireContext()
        val d = resources.displayMetrics.density
        val buscar = com.google.android.material.textfield.TextInputEditText(ctx).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            maxLines = 1
        }
        val campo = com.google.android.material.textfield.TextInputLayout(ctx, null,
            com.google.android.material.R.attr.textInputOutlinedStyle).apply {
            hint = getString(R.string.parqueadero_buscar_apto)
            setStartIconDrawable(R.drawable.ic_search)
            addView(buscar)
        }
        var visibles = aptos
        val adaptadorAptos = android.widget.ArrayAdapter<String>(ctx, android.R.layout.simple_list_item_1, mutableListOf())
        fun filtrar() {
            val q = com.example.conjuntoresidencial.ui.comun.FiltroTorreApto.normalizar(buscar.text?.toString().orEmpty().trim())
            visibles = aptos.filter { a ->
                q.isEmpty() || com.example.conjuntoresidencial.ui.comun.FiltroTorreApto.normalizar("${a.torre}-${a.apto}").contains(q) ||
                    com.example.conjuntoresidencial.ui.comun.FiltroTorreApto.normalizar("${a.torre} ${a.apto}").contains(q) ||
                    a.apto.startsWith(q)
            }
            adaptadorAptos.clear()
            adaptadorAptos.addAll(visibles.map { getString(R.string.apto_item_titulo, it.torre, it.apto) })
        }
        val lista = android.widget.ListView(ctx).apply { adapter = adaptadorAptos }
        val caja = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding((24 * d).toInt(), (8 * d).toInt(), (24 * d).toInt(), 0)
            addView(campo)
            addView(lista, android.widget.LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, (320 * d).toInt()))
        }
        filtrar()
        buscar.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) = filtrar()
        })
        val dialogo = MaterialAlertDialogBuilder(ctx)
            .setTitle(getString(R.string.parqueadero_asignar_titulo, p.numero))
            .setView(caja)
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
        lista.setOnItemClickListener { _, _, i, _ ->
            dialogo.dismiss()
            elegirVehiculo(p, visibles[i])
        }
    }

    /** Con vehículos de esa categoría sin parqueadero, se elige uno o ninguno; si no hay, va directo al apartamento. */
    private fun elegirVehiculo(p: ParqueaderoDTO, a: com.example.conjuntoresidencial.model.Apartamento) {
        viewLifecycleOwner.lifecycleScope.launch {
            val candidatos = try {
                RetrofitClient.comun.getVehiculosPorApartamento(a.id).body().orEmpty()
                    .filter { it.estado == "APROBADO" && it.tipo == p.tipo && (it.parqueaderoId == null || it.parqueaderoId == p.id) }
            } catch (_: Exception) {
                emptyList()
            }
            if (view == null) return@launch
            val destino = getString(R.string.apto_item_titulo, a.torre, a.apto)
            fun asignar(vehiculoId: Long?) = ejecutarAccion(getString(R.string.parqueadero_asignado_a, p.numero, destino),
                { RetrofitClient.instance.asignarParqueadero(p.id, AsignacionParqueadero(vehiculoId, a.id)) }) { cargar() }
            if (candidatos.isEmpty()) { asignar(null); return@launch }
            val opciones = candidatos.map { v -> listOfNotNull(v.placa, v.marca, v.color).joinToString(" · ").ifEmpty { v.tipo } } +
                getString(R.string.parqueadero_sin_vehiculo)
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.parqueadero_vincular_vehiculo, destino))
                .setItems(opciones.toTypedArray()) { _, i -> asignar(candidatos.getOrNull(i)?.id) }
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
