package com.example.conjuntoresidencial.ui.comun

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityViewCommand
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FragmentNotificacionesBinding
import com.example.conjuntoresidencial.databinding.ItemNotificacionBinding
import com.example.conjuntoresidencial.databinding.ItemNotificacionDiaBinding
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.example.conjuntoresidencial.model.NotificacionDTO
import com.example.conjuntoresidencial.ui.comun.DestinoNotificacion
import com.example.conjuntoresidencial.ui.comun.DiffPorId
import com.example.conjuntoresidencial.ui.comun.NotificacionesViewModel
import com.example.conjuntoresidencial.ui.comun.accionesDeLista
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.mostrarRecurso
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.ui.comun.resumen
import com.example.conjuntoresidencial.util.SessionManager
import com.example.conjuntoresidencial.ui.comun.aviso
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

/**
 * Pestaña Notificaciones: del administrador (reservas y vehículos nuevos) o del residente
 * (respuestas a sus vehículos y reservas); cada uno ve solo las suyas (lo filtra el backend).
 * P9 bloque 1: cada aviso con el ícono, el color y la categoría de su tipo, un título corto y el
 * detalle, agrupados por día (Hoy, Ayer...).
 * Las nuevas se ven resaltadas; al tocar una se marca leída y lleva a su pantalla
 * (DestinoNotificacion). Se borran deslizándolas hacia un lado, con opción de deshacer.
 */
class NotificacionesFragment : Fragment() {
    private var _binding: FragmentNotificacionesBinding? = null
    private val binding get() = _binding!!
    // Compartido con MainActivity (insignia de la barra) y el panel (resumen)
    private val viewModel: NotificacionesViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNotificacionesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val actualizar = accionesDeLista(binding.swipe) { cargar() }
        // Pestaña de la barra inferior: sin botón para volver
        // La misma pantalla para los dos roles: cambian la descripción y el mensaje sin avisos
        val esAdmin = SessionManager(requireContext()).esAdmin()
        configurarEncabezado(
            binding.encabezado, R.string.notificaciones,
            if (esAdmin) R.string.notificaciones_descripcion else R.string.notificaciones_descripcion_residente,
            atras = false, menu = R.menu.menu_notificaciones
        ) { item ->
            when (item) {
                R.id.accion_marcar_leidas -> {
                    viewModel.marcarTodasLeidas(getString(R.string.notificaciones_error_marcar)); true
                }
                R.id.accion_limpiar -> { confirmarLimpiar(); true }
                else -> actualizar(item)
            }
        }

        val adaptador = NotificacionAdapter(onAbrir = ::abrir, onEliminar = ::eliminar)
        binding.rvNotificaciones.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNotificaciones.adapter = adaptador
        binding.estado.vincular(binding.rvNotificaciones)
        binding.swipe.setOnRefreshListener { cargar() }
        deslizarParaEliminar(adaptador)

        viewModel.lista.observe(viewLifecycleOwner) { resource ->
            binding.estado.mostrarRecurso(
                resource, binding.swipe, R.drawable.ic_notifications, getString(R.string.actividad_vacia),
                getString(if (esAdmin) R.string.notificaciones_vacio_mensaje else R.string.notificaciones_vacio_mensaje_residente),
                reintentar = { cargar() }
            ) { lista ->
                adaptador.submitList(FilaAviso.agrupar(requireContext(), lista))
                binding.encabezado.ponerInfo(resumen(
                    R.plurals.resumen_avisos, lista.size,
                    R.plurals.resumen_nuevas to lista.count { !it.leida }
                ))
            }
        }

        viewModel.mensaje.observe(viewLifecycleOwner) { evento ->
            evento.obtenerSiNoManejado()?.let { aviso(it, Snackbar.LENGTH_LONG).show() }
        }

        // Siempre se recarga al entrar: es lo que el administrador viene a ver
        cargar()
    }

    private fun cargar() = viewModel.cargar(getString(R.string.actividad_error_cargar))

    private fun abrir(n: NotificacionDTO) {
        viewModel.marcarLeida(n.id)
        DestinoNotificacion.de(n).abrir(findNavController())
    }

    private fun eliminar(n: NotificacionDTO) {
        val error = getString(R.string.notificaciones_error_eliminar)
        viewModel.eliminar(n, error)
        // Se borra de verdad cuando el "Deshacer" desaparece sin usarse (su duración varía:
        // con TalkBack dura más), o si otro borrado lo reemplaza
        aviso(R.string.notificacion_eliminada, Snackbar.LENGTH_LONG)
            .setAction(R.string.deshacer) { viewModel.deshacerEliminar(n) }
            .addCallback(object : Snackbar.Callback() {
                override fun onDismissed(barra: Snackbar?, evento: Int) {
                    if (evento != DISMISS_EVENT_ACTION) viewModel.confirmarEliminar(n, error)
                }
            })
            .show()
    }

    /** Deslizar una tarjeta hacia cualquier lado la elimina (TalkBack: acción "Eliminar"). */
    private fun deslizarParaEliminar(adaptador: NotificacionAdapter) {
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START or ItemTouchHelper.END) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, destino: RecyclerView.ViewHolder) = false
            // Los encabezados de día no se deslizan
            override fun getSwipeDirs(rv: RecyclerView, vh: RecyclerView.ViewHolder) =
                if (vh is NotificacionAdapter.VH) super.getSwipeDirs(rv, vh) else 0
            override fun onSwiped(vh: RecyclerView.ViewHolder, direccion: Int) {
                @Suppress("DEPRECATION") // la versión de RecyclerView del proyecto no trae bindingAdapterPosition
                val posicion = vh.adapterPosition
                if (posicion == RecyclerView.NO_POSITION) return
                (adaptador.currentList[posicion] as? FilaAviso.Aviso)?.let { eliminar(it.n) }
            }
        }).attachToRecyclerView(binding.rvNotificaciones)
    }

    private fun confirmarLimpiar() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.actividad_limpiar)
            .setMessage(R.string.notificaciones_limpiar_pregunta)
            .setPositiveButton(R.string.actividad_limpiar) { _, _ ->
                viewModel.limpiar(getString(R.string.actividad_error_limpiar))
            }
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

/** P9 bloque 1: lo que muestra la lista: un encabezado por día y, debajo, sus avisos. */
sealed class FilaAviso {
    abstract val clave: Long

    data class Dia(val texto: String) : FilaAviso() {
        // Negativo: no choca con los id de los avisos
        override val clave: Long get() = -(texto.hashCode().toLong() and 0x7FFF_FFFF) - 1
    }

    data class Aviso(val n: NotificacionDTO) : FilaAviso() {
        override val clave: Long get() = n.id
    }

    companion object {
        /** Ya vienen de la más reciente a la más vieja: se agrega un encabezado cada vez que cambia el día. */
        fun agrupar(context: android.content.Context, lista: List<NotificacionDTO>): List<FilaAviso> {
            val filas = mutableListOf<FilaAviso>()
            var diaActual: String? = null
            lista.forEach { n ->
                val dia = FechaAviso.dia(context, n.fecha)
                if (dia != diaActual) { filas += Dia(dia); diaActual = dia }
                filas += Aviso(n)
            }
            return filas
        }
    }
}

class NotificacionAdapter(
    private val onAbrir: (NotificacionDTO) -> Unit,
    private val onEliminar: (NotificacionDTO) -> Unit
) : ListAdapter<FilaAviso, RecyclerView.ViewHolder>(DiffPorId { it.clave }) {

    class VH(val binding: ItemNotificacionBinding) : RecyclerView.ViewHolder(binding.root) {
        var accionEliminar: Int? = null
    }

    class VHDia(val binding: ItemNotificacionDiaBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int) = if (getItem(position) is FilaAviso.Dia) TIPO_DIA else TIPO_AVISO

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TIPO_DIA) VHDia(ItemNotificacionDiaBinding.inflate(inflater, parent, false))
        else VH(ItemNotificacionBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
        when (val fila = getItem(pos)) {
            is FilaAviso.Dia -> (holder as VHDia).binding.tvDia.text = fila.texto
            is FilaAviso.Aviso -> pintar(holder as VH, fila.n)
        }
    }

    private fun pintar(h: VH, n: NotificacionDTO) {
        val ctx = h.binding.root.context
        val nueva = !n.leida
        val tipo = TipoAviso.de(n.tipo)
        val color = ContextCompat.getColor(ctx, tipo.categoria.color)

        // Ícono de la categoría en su color, sobre un círculo del mismo color más suave
        h.binding.imgTipo.setImageResource(tipo.categoria.icono)
        h.binding.imgTipo.setColorFilter(color)
        h.binding.fondoIcono.background.mutate().setTint(ColorUtils.setAlphaComponent(color, 0x2E))
        h.binding.tvCategoria.setText(tipo.categoria.nombre)
        h.binding.tvCategoria.setTextColor(color)
        val estado = tipo.estado
        h.binding.tvEstado.visibility = if (estado != null) View.VISIBLE else View.GONE
        if (estado != null) {
            val colorEstado = ContextCompat.getColor(ctx, estado.color)
            h.binding.tvEstado.setText(estado.nombre)
            h.binding.tvEstado.setTextColor(colorEstado)
            h.binding.tvEstado.background.mutate().setTint(ColorUtils.setAlphaComponent(colorEstado, 0x2E))
        }
        h.binding.tvFecha.text = FechaAviso.cuando(ctx, n.fecha)
        h.binding.tvTitulo.setText(tipo.titulo)
        h.binding.tvMensaje.text = TipoAviso.detalle(ctx, n.mensaje, tipo)

        // Nueva: fondo resaltado, título en negrilla y punto. Leída: tarjeta normal.
        h.binding.root.setCardBackgroundColor(MaterialColors.getColor(
            h.binding.root, if (nueva) com.google.android.material.R.attr.colorPrimaryContainer
            else com.google.android.material.R.attr.colorSurfaceContainerLow
        ))
        h.binding.tvTitulo.setTypeface(null, if (nueva) Typeface.BOLD else Typeface.NORMAL)
        h.binding.puntoNueva.visibility = if (nueva) View.VISIBLE else View.GONE

        h.binding.root.setOnClickListener { onAbrir(n) }
        // TalkBack: "Nueva. Reservas. Nueva reserva. 25/10/2026 · Torre 5 Apto 502. hace 5 min" y la acción "Eliminar"
        h.binding.root.contentDescription = listOfNotNull(
            ctx.getString(R.string.notificacion_nueva).takeIf { nueva },
            ctx.getString(tipo.categoria.nombre), ctx.getString(tipo.titulo),
            estado?.let { ctx.getString(it.nombre) }, h.binding.tvMensaje.text, h.binding.tvFecha.text
        ).joinToString(". ")
        h.accionEliminar?.let { ViewCompat.removeAccessibilityAction(h.binding.root, it) }
        h.accionEliminar = ViewCompat.addAccessibilityAction(
            h.binding.root, ctx.getString(R.string.eliminar)
        ) { _, _: AccessibilityViewCommand.CommandArguments? -> onEliminar(n); true }
    }

    private companion object {
        const val TIPO_DIA = 0
        const val TIPO_AVISO = 1
    }
}
