package com.example.conjuntoresidencial.ui.admin

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
import com.example.conjuntoresidencial.model.NotificacionDTO
import com.example.conjuntoresidencial.ui.comun.DestinoNotificacion
import com.example.conjuntoresidencial.ui.comun.DiffPorId
import com.example.conjuntoresidencial.ui.comun.NotificacionesViewModel
import com.example.conjuntoresidencial.ui.comun.accionesDeLista
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.mostrarRecurso
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.ui.comun.resumen
import com.example.conjuntoresidencial.util.Formatos
import com.example.conjuntoresidencial.util.SessionManager
import com.example.conjuntoresidencial.ui.comun.aviso
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

/**
 * Pestaña Notificaciones: del administrador (reservas y vehículos nuevos) o del residente
 * (respuestas a sus vehículos y reservas); cada uno ve solo las suyas (lo filtra el backend).
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
                adaptador.submitList(lista)
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
            override fun onSwiped(vh: RecyclerView.ViewHolder, direccion: Int) {
                @Suppress("DEPRECATION") // la versión de RecyclerView del proyecto no trae bindingAdapterPosition
                val posicion = vh.adapterPosition
                if (posicion != RecyclerView.NO_POSITION) eliminar(adaptador.currentList[posicion])
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

class NotificacionAdapter(
    private val onAbrir: (NotificacionDTO) -> Unit,
    private val onEliminar: (NotificacionDTO) -> Unit
) : ListAdapter<NotificacionDTO, NotificacionAdapter.VH>(DiffPorId { it.id }) {

    class VH(val binding: ItemNotificacionBinding) : RecyclerView.ViewHolder(binding.root) {
        var accionEliminar: Int? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemNotificacionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: VH, pos: Int) {
        val n = getItem(pos)
        val ctx = h.binding.root.context
        val nueva = !n.leida
        h.binding.imgTipo.setImageResource(
            if (n.tipo.startsWith(DestinoNotificacion.TIPO_VEHICULO)) R.drawable.ic_car else R.drawable.ic_event
        )
        h.binding.tvMensaje.text = n.mensaje
        // "2026-10-21T00:37:12" -> "21/10/2026 · 12:37 a. m."
        h.binding.tvFecha.text = Formatos.fechaHora(n.fecha.take(10), n.fecha.drop(11))

        // Nueva: fondo resaltado, texto en negrilla y punto. Leída: tarjeta normal y texto atenuado.
        h.binding.root.setCardBackgroundColor(MaterialColors.getColor(
            h.binding.root, if (nueva) com.google.android.material.R.attr.colorPrimaryContainer
            else com.google.android.material.R.attr.colorSurfaceContainerLow
        ))
        h.binding.tvMensaje.setTypeface(Typeface.create(h.binding.tvMensaje.typeface, if (nueva) Typeface.BOLD else Typeface.NORMAL))
        h.binding.tvMensaje.setTextColor(MaterialColors.getColor(
            h.binding.root, if (nueva) com.google.android.material.R.attr.colorOnPrimaryContainer
            else com.google.android.material.R.attr.colorOnSurfaceVariant
        ))
        h.binding.puntoNueva.visibility = if (nueva) View.VISIBLE else View.GONE
        h.binding.tvEtiquetaNueva.visibility = if (nueva) View.VISIBLE else View.GONE

        h.binding.root.setOnClickListener { onAbrir(n) }
        // TalkBack: "Nueva. Nueva reserva — Zona BBQ…, 21/10/2026 · 12:37 a. m." y la acción "Eliminar"
        h.binding.root.contentDescription = listOfNotNull(
            ctx.getString(R.string.notificacion_nueva).takeIf { nueva },
            n.mensaje, h.binding.tvFecha.text
        ).joinToString(". ")
        h.accionEliminar?.let { ViewCompat.removeAccessibilityAction(h.binding.root, it) }
        h.accionEliminar = ViewCompat.addAccessibilityAction(
            h.binding.root, ctx.getString(R.string.eliminar)
        ) { _, _: AccessibilityViewCommand.CommandArguments? -> onEliminar(n); true }
    }
}
