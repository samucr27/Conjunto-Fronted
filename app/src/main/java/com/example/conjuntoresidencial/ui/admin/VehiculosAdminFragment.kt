package com.example.conjuntoresidencial.ui.admin

import com.example.conjuntoresidencial.ui.comun.BarraBusqueda
import com.example.conjuntoresidencial.ui.comun.repetir
import com.example.conjuntoresidencial.util.map
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FragmentVehiculosAdminBinding
import com.example.conjuntoresidencial.databinding.ItemVehiculoPendienteBinding
import com.example.conjuntoresidencial.model.CupoVehiculoDTO
import com.example.conjuntoresidencial.model.VehiculoDTO
import com.example.conjuntoresidencial.ui.comun.DiffPorId
import com.example.conjuntoresidencial.ui.comun.Estados
import com.example.conjuntoresidencial.ui.comun.accionesDeLista
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.mostrarEstado
import com.example.conjuntoresidencial.ui.comun.mostrarRecurso
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.ui.comun.resumen
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * Vehículos del administrador: los que registraron los residentes y esperan su aprobación
 * (aprobar o rechazar con motivo) y, desde el menú "⋮", cuántos de cada tipo puede tener cada
 * apartamento. Desde una notificación llega con ese vehículo resaltado.
 */
class VehiculosAdminFragment : Fragment() {
    private var _binding: FragmentVehiculosAdminBinding? = null
    private val binding get() = _binding!!
    private val viewModel: VehiculosAdminViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentVehiculosAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val actualizar = accionesDeLista(binding.swipe) { cargar() }
        configurarEncabezado(
            binding.encabezado, R.string.vehiculos_admin_titulo, R.string.vehiculos_admin_subtitulo,
            menu = R.menu.menu_vehiculos_admin
        ) { item ->
            if (item == R.id.accion_cupos) { viewModel.cargarCupos(getString(R.string.cupos_error)); true }
            else actualizar(item)
        }

        val resaltado = arguments?.takeIf { it.containsKey(ARG_RESALTAR) }?.getLong(ARG_RESALTAR)
        val adaptador = VehiculoPendienteAdapter(
            resaltado = resaltado,
            // P5: se aprueba asignándole un parqueadero libre de su categoría (o sin parqueadero)
            onAprobar = { v ->
                elegirParqueadero(v.tipo, getString(R.string.parqueadero_al_aprobar, v.placa ?: v.tipo)) { parqueaderoId ->
                    viewModel.revisar(v, true, null, getString(R.string.vehiculo_aprobado_hecho),
                        getString(R.string.vehiculo_error_revisar), parqueaderoId)
                }
            },
            onRechazar = ::pedirMotivo
        )
        binding.rvVehiculosPendientes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvVehiculosPendientes.adapter = adaptador
        binding.estado.vincular(binding.rvVehiculosPendientes)
        binding.swipe.setOnRefreshListener { cargar() }

        var desplazado = false
        val barra = BarraBusqueda(this, binding.busqueda, R.string.busqueda_vehiculos) { viewModel.pendientes.repetir() }
        barra.vincular(binding.swipe, binding.rvVehiculosPendientes)
        viewModel.pendientes.observe(viewLifecycleOwner) { todo ->
            val resource = todo.map { lista ->
                lista.filter { v -> barra.coincide(v.placa, v.marca, v.color, v.tipo, v.torre, v.apto, "torre ${v.torre} apto ${v.apto}") }
            }
            binding.estado.mostrarRecurso(
                resource, binding.swipe, R.drawable.ic_car,
                if (barra.activa) getString(R.string.busqueda_sin_resultados) else getString(R.string.vehiculos_admin_vacio),
                if (barra.activa) null else getString(R.string.vehiculos_admin_vacio_mensaje), reintentar = { cargar() }
            ) { lista ->
                adaptador.submitList(lista) {
                    val posicion = lista.indexOfFirst { it.id == resaltado }
                    if (!desplazado && posicion >= 0) {
                        desplazado = true
                        binding.rvVehiculosPendientes.scrollToPosition(posicion)
                    }
                }
                binding.encabezado.ponerInfo(resumen(R.plurals.resumen_por_aprobar, lista.size))
            }
        }
        viewModel.mensaje.observe(viewLifecycleOwner) { evento ->
            evento.obtenerSiNoManejado()?.let { aviso(it).show() }
        }
        // Al cargar los cupos (menú "⋮ → Cupos por apartamento") se abre su editor
        viewModel.cupos.observe(viewLifecycleOwner) { evento ->
            evento.obtenerSiNoManejado()?.takeIf { it.isNotEmpty() }?.let(::editarCupos)
        }

        if (viewModel.pendientes.value == null) cargar()
    }

    private fun cargar() = viewModel.cargar(getString(R.string.vehiculos_admin_error))

    /** Rechazar pide un motivo (opcional): el residente lo ve en Mis vehículos. */
    private fun pedirMotivo(vehiculo: VehiculoDTO) {
        val campo = TextInputLayout(requireContext(), null, com.google.android.material.R.attr.textInputOutlinedStyle).apply {
            hint = getString(R.string.vehiculo_motivo)
            helperText = getString(R.string.vehiculo_motivo_ayuda)
            addView(TextInputEditText(context).apply {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                maxLines = 3
            })
        }
        val contenedor = LinearLayout(requireContext()).apply {
            val relleno = (24 * resources.displayMetrics.density).toInt()
            setPadding(relleno, (8 * resources.displayMetrics.density).toInt(), relleno, 0)
            addView(campo, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.vehiculo_rechazar_titulo, descripcion(vehiculo)))
            .setView(contenedor)
            .setPositiveButton(R.string.vehiculo_rechazar) { _, _ ->
                viewModel.revisar(
                    vehiculo, false, campo.editText?.text?.toString()?.trim()?.ifBlank { null },
                    getString(R.string.vehiculo_rechazado_hecho), getString(R.string.vehiculo_error_revisar)
                )
            }
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
    }

    /** Un campo numérico por tipo; al guardar se envían todos juntos. */
    private fun editarCupos(cupos: List<CupoVehiculoDTO>) {
        val dp = resources.displayMetrics.density
        val columna = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24 * dp).toInt(), (8 * dp).toInt(), (24 * dp).toInt(), 0)
        }
        val campos = cupos.map { cupo ->
            TextInputLayout(requireContext(), null, com.google.android.material.R.attr.textInputOutlinedStyle).apply {
                hint = cupo.tipo
                addView(TextInputEditText(context).apply {
                    inputType = InputType.TYPE_CLASS_NUMBER
                    setText(cupo.maximo.toString())
                })
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    .apply { topMargin = (8 * dp).toInt() }
                columna.addView(this)
            }
        }
        val dialogo = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.cupos_titulo)
            .setMessage(R.string.cupos_mensaje)
            .setView(android.widget.ScrollView(requireContext()).apply { addView(columna) })
            .setPositiveButton(R.string.btn_guardar, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val nuevos = cupos.zip(campos).map { (cupo, campo) ->
                val valor = campo.editText?.text?.toString()?.toIntOrNull()
                campo.error = if (valor == null || valor !in 0..MAXIMO_CUPO) getString(R.string.cupos_rango, MAXIMO_CUPO) else null
                cupo.copy(maximo = valor ?: -1)
            }
            if (nuevos.any { it.maximo !in 0..MAXIMO_CUPO }) return@setOnClickListener
            viewModel.guardarCupos(nuevos, getString(R.string.cupos_guardados), getString(R.string.cupos_error))
            dialogo.dismiss()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    companion object {
        /** Id del vehículo que se abrió desde una notificación. */
        const val ARG_RESALTAR = "vehiculoResaltado"
        private const val MAXIMO_CUPO = 20

        /** "ABC123 · Carro Mazda Rojo" o, sin placa, "Bicicleta GW Negra". */
        fun descripcion(v: VehiculoDTO): String {
            val detalle = listOf(v.tipo, v.marca, v.color).filterNot { it.isNullOrBlank() }.joinToString(" ")
            return if (v.placa.isNullOrBlank()) detalle else "${v.placa} · $detalle"
        }
    }
}

class VehiculoPendienteAdapter(
    private val resaltado: Long?,
    private val onAprobar: (VehiculoDTO) -> Unit,
    private val onRechazar: (VehiculoDTO) -> Unit
) : ListAdapter<VehiculoDTO, VehiculoPendienteAdapter.VH>(DiffPorId { it.id }) {

    class VH(val binding: ItemVehiculoPendienteBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemVehiculoPendienteBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: VH, pos: Int) {
        val v = getItem(pos)
        val ctx = h.binding.root.context
        val descripcion = VehiculosAdminFragment.descripcion(v)
        h.binding.tvVehiculoPendiente.text = descripcion
        h.binding.tvAptoVehiculoPendiente.text = ctx.getString(R.string.apto_item_titulo, v.torre ?: "", v.apto ?: "")
        h.binding.tvEstadoVehiculoPendiente.mostrarEstado(Estados.vehiculo(v.estado))
        h.binding.btnAprobarVehiculo.contentDescription = ctx.getString(R.string.desc_aprobar_vehiculo, descripcion)
        h.binding.btnRechazarVehiculo.contentDescription = ctx.getString(R.string.desc_rechazar_vehiculo, descripcion)
        h.binding.btnAprobarVehiculo.setOnClickListener { onAprobar(v) }
        h.binding.btnRechazarVehiculo.setOnClickListener { onRechazar(v) }
        // El que se abrió desde una notificación lleva un borde
        val esResaltado = resaltado != null && v.id == resaltado
        h.binding.root.strokeWidth = if (esResaltado) (2 * ctx.resources.displayMetrics.density).toInt() else 0
        h.binding.root.strokeColor = MaterialColors.getColor(h.binding.root, androidx.appcompat.R.attr.colorPrimary)
    }
}
