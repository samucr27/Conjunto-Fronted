package com.example.conjuntoresidencial.ui.propietario

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.FragmentMisReservasBinding
import com.example.conjuntoresidencial.model.ReservaDTO
import com.example.conjuntoresidencial.ui.admin.ReservaAdapter
import com.example.conjuntoresidencial.ui.admin.ReservaConZona
import com.example.conjuntoresidencial.ui.comun.*
import com.example.conjuntoresidencial.ui.reservas.ReservarZonaFragment
import com.example.conjuntoresidencial.util.Formatos
import com.example.conjuntoresidencial.util.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Mis reservas (P5): las próximas; tocar una muestra su detalle con la opción de editarla
 * (mientras esté pendiente) o cancelarla (mientras no haya empezado). Las que ya pasaron
 * están en Historial (menú ⋮).
 */
class MisReservasFragment : Fragment() {
    private var _binding: FragmentMisReservasBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MisReservasViewModel by viewModels()
    private val historial get() = arguments?.getBoolean(ARG_HISTORIAL) == true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMisReservasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val aptoId = SessionManager(requireContext()).getApartamentoId()
        val recargar = { viewModel.fetchReservas(aptoId) }
        configurarEncabezado(
            binding.encabezado, if (historial) R.string.reservas_historial else R.string.menu_mis_reservas,
            if (historial) R.string.reservas_historial_sub else R.string.menu_mis_reservas_sub,
            menu = if (historial) R.menu.menu_lista else R.menu.menu_mis_reservas
        ) { item ->
            when (item) {
                R.id.accion_historial -> { findNavController().navigate(R.id.misReservasFragment, bundleOf(ARG_HISTORIAL to true)); true }
                else -> accionesDeLista(binding.swipe, recargar)(item)
            }
        }

        val adaptador = ReservaAdapter(onTocar = { detalle(it, aptoId) })
        binding.rvReservasPropietario.layoutManager = LinearLayoutManager(requireContext())
        binding.rvReservasPropietario.adapter = adaptador
        binding.estado.vincular(binding.rvReservasPropietario)
        binding.swipe.setOnRefreshListener { recargar() }

        viewModel.reservas.observe(viewLifecycleOwner) { resource ->
            val filtrado = when (resource) {
                is com.example.conjuntoresidencial.util.Resource.Success ->
                    com.example.conjuntoresidencial.util.Resource.Success(resource.data.orEmpty().filter { yaPaso(it) == historial })
                else -> resource
            }
            binding.estado.mostrarRecurso(
                filtrado, binding.swipe, R.drawable.ic_event,
                getString(if (historial) R.string.reservas_historial_vacio else R.string.mis_reservas_vacio),
                if (historial) null else getString(R.string.mis_reservas_vacio_mensaje),
                reintentar = recargar
            ) { lista ->
                val ordenadas = if (historial) lista.sortedByDescending { it.fecha + it.hora } else lista.sortedBy { it.fecha + it.hora }
                adaptador.submitList(ReservaConZona.de(ordenadas, viewModel.nombresZonas))
                binding.encabezado.ponerInfo(resumen(
                    R.plurals.resumen_reservas, lista.size,
                    R.plurals.resumen_pendientes to lista.count { it.estado == "PENDIENTE" }
                ))
            }
        }

        // Al volver de editar se ve el cambio
        recargar()
    }

    /** Detalle con lo que se puede hacer: editar (pendiente) o cancelar (no ha empezado). */
    private fun detalle(item: ReservaConZona, aptoId: Long) {
        val (r, zona) = item
        val mensaje = listOfNotNull(
            Formatos.horario(r.fecha, r.hora, r.horaFin),
            r.asistentes?.let { resources.getQuantityString(R.plurals.reserva_personas, it, it) },
            getString(Estados.reserva(r.estado).etiqueta)
        ).joinToString("\n")
        val dialogo = MaterialAlertDialogBuilder(requireContext()).setTitle(zona).setMessage(mensaje)
            .setNeutralButton(R.string.btn_cerrar, null)
        val futura = !yaEmpezo(r)
        if (r.estado == "PENDIENTE" && futura) dialogo.setPositiveButton(R.string.editar) { _, _ ->
            findNavController().navigate(R.id.reservarZonaFragment, ReservarZonaFragment.argumentosEditar(r, zona))
        }
        if (r.estado != "RECHAZADA" && futura) dialogo.setNegativeButton(R.string.reserva_cancelar) { _, _ ->
            confirmar(getString(R.string.reserva_cancelar_titulo), getString(R.string.reserva_cancelar_mensaje, zona,
                Formatos.horario(r.fecha, r.hora, r.horaFin)), R.string.reserva_cancelar) {
                ejecutarAccion(getString(R.string.reserva_cancelada), { RetrofitClient.instance.deleteReserva(r.id!!) }) {
                    viewModel.fetchReservas(aptoId)
                }
            }
        }
        dialogo.show()
    }

    private fun momento(fecha: String, hora: String?): Long? = try {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).parse("$fecha ${hora.orEmpty().take(5)}")?.time
    } catch (_: Exception) {
        null
    }

    private fun yaEmpezo(r: ReservaDTO) = (momento(r.fecha, r.hora) ?: Long.MAX_VALUE) <= System.currentTimeMillis()

    /** Ya terminó: su hora de fin (o 2 horas después del inicio, en las antiguas) quedó atrás. */
    private fun yaPaso(r: ReservaDTO): Boolean {
        val fin = r.horaFin?.let { momento(r.fecha, it) } ?: momento(r.fecha, r.hora)?.plus(2 * 3_600_000L)
        return fin != null && fin <= System.currentTimeMillis()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    companion object {
        const val ARG_HISTORIAL = "historial"
    }
}
