package com.example.conjuntoresidencial.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FragmentNotificacionesBinding
import com.example.conjuntoresidencial.databinding.ItemNotificacionBinding
import com.example.conjuntoresidencial.model.NotificacionDTO
import com.example.conjuntoresidencial.ui.comun.DiffPorId
import com.example.conjuntoresidencial.ui.comun.NotificacionesViewModel
import com.example.conjuntoresidencial.ui.comun.accionesDeLista
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.mostrarRecurso
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.ui.comun.resumen
import com.example.conjuntoresidencial.util.Formatos
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

/** Pestaña Notificaciones del administrador: nuevas reservas y vehículos registrados. */
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
        configurarEncabezado(
            binding.encabezado, R.string.notificaciones, R.string.notificaciones_descripcion,
            atras = false, menu = R.menu.menu_notificaciones
        ) { item ->
            when (item) {
                R.id.accion_limpiar -> { confirmarLimpiar(); true }
                else -> actualizar(item)
            }
        }

        val adaptador = NotificacionAdapter()
        binding.rvNotificaciones.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNotificaciones.adapter = adaptador
        binding.estado.vincular(binding.rvNotificaciones)
        binding.swipe.setOnRefreshListener { cargar() }

        viewModel.lista.observe(viewLifecycleOwner) { resource ->
            binding.estado.mostrarRecurso(
                resource, binding.swipe, R.drawable.ic_notifications, getString(R.string.actividad_vacia),
                getString(R.string.notificaciones_vacio_mensaje),
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
            evento.obtenerSiNoManejado()?.let { Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show() }
        }

        // Siempre se recarga al entrar: es lo que el administrador viene a ver
        cargar()
    }

    private fun cargar() = viewModel.cargar(getString(R.string.actividad_error_cargar))

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

class NotificacionAdapter : ListAdapter<NotificacionDTO, NotificacionAdapter.VH>(DiffPorId { it.id }) {

    class VH(val binding: ItemNotificacionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemNotificacionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: VH, pos: Int) {
        val n = getItem(pos)
        val ctx = h.binding.root.context
        h.binding.imgTipo.setImageResource(if (n.tipo == "VEHICULO") R.drawable.ic_car else R.drawable.ic_event)
        h.binding.tvMensaje.text = n.mensaje
        // "2026-10-21T00:37:12" -> "21/10/2026 · 12:37 a. m."
        h.binding.tvFecha.text = Formatos.fechaHora(n.fecha.take(10), n.fecha.drop(11))
        h.binding.puntoNueva.visibility = if (n.leida) View.GONE else View.VISIBLE
        // TalkBack: "Nueva. Nueva reserva — Zona BBQ…, 21/10/2026 · 12:37 a. m."
        h.binding.root.contentDescription = listOfNotNull(
            ctx.getString(R.string.notificacion_nueva).takeIf { !n.leida },
            n.mensaje, h.binding.tvFecha.text
        ).joinToString(". ")
    }
}
