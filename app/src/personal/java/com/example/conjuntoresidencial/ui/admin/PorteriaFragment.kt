package com.example.conjuntoresidencial.ui.admin

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.addCallback
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.ConfigApp
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.util.SessionManager
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.FragmentPorteriaBinding
import com.example.conjuntoresidencial.databinding.ItemParqueaderoInvitadoBinding
import com.example.conjuntoresidencial.databinding.ItemVisitaBinding
import com.example.conjuntoresidencial.model.NuevaVisita
import com.example.conjuntoresidencial.model.ParqueaderoInvitadoDTO
import com.example.conjuntoresidencial.model.RespuestaVisita
import com.example.conjuntoresidencial.model.VisitaDTO
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.confirmar
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors
import com.example.conjuntoresidencial.ui.comun.elegirOpcion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pantalla de la portería (P6): registra una visita, al apartamento le llega un aviso con 2
 * minutos para responder, y esta lista se actualiza sola cada 7 segundos. Si el residente
 * contesta por teléfono, el portero registra su respuesta ("por llamada").
 * P8 bloque 4: el visitante entra a pie o en vehículo; con vehículo se anota la placa y se le da
 * un parqueadero de invitados libre, que vuelve a quedar libre al registrar su salida.
 */
class PorteriaFragment : Fragment() {
    private var _binding: FragmentPorteriaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPorteriaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // P7 bloque 4: en la recepción, quién tiene el turno y el botón para bloquear (otro guardia lo abre con su PIN)
        val bloqueo = ConfigApp.destinoBloqueo
        configurarEncabezado(binding.encabezado, R.string.porteria_titulo, R.string.porteria_descripcion, atras = false,
            menu = if (bloqueo != null) R.menu.menu_porteria_recepcion else null) { id ->
            (id == R.id.accion_bloquear && bloqueo != null).also { tocado ->
                if (tocado) bloqueo?.let { findNavController().navigate(it, null, NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()) }
            }
        }
        SessionManager(requireContext()).getGuardia()?.let { binding.encabezado.ponerInfo(getString(R.string.porteria_turno_de, it)) }
        // En la recepción es la primera pantalla: "atrás" deja la app abierta para no borrar la sesión del puesto
        if (bloqueo != null) {
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { requireActivity().moveTaskToBack(true) }
        }
        binding.btnRegistrarVisita.setOnClickListener { registrar() }
        binding.grupoIngreso.addOnButtonCheckedListener { _, _, _ -> mostrarVehiculo() }
        mostrarVehiculo()
        // Consulta cada 7 segundos mientras la pantalla está a la vista
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    cargar()
                    delay(INTERVALO_MS)
                }
            }
        }
    }

    private fun registrar() {
        val torre = binding.etTorreVisita.text?.toString()?.trim().orEmpty()
        val apto = binding.etAptoVisita.text?.toString()?.trim().orEmpty()
        val nombre = binding.etNombreVisita.text?.toString()?.trim().orEmpty()
        binding.tilTorreVisita.error = if (torre.isEmpty()) getString(R.string.login_falta_torre) else null
        binding.tilAptoVisita.error = if (apto.isEmpty()) getString(R.string.login_falta_apartamento) else null
        binding.tilNombreVisita.error = if (nombre.isEmpty()) getString(R.string.porteria_falta_visitante) else null
        val enVehiculo = enVehiculo()
        val placa = binding.etPlacaVisita.text?.toString()?.trim().orEmpty()
        binding.tilPlacaVisita.error = if (enVehiculo && placa.isEmpty()) getString(R.string.ingreso_falta_placa) else null
        if (torre.isEmpty() || apto.isEmpty() || nombre.isEmpty() || (enVehiculo && placa.isEmpty())) return
        // El chip marcado; sin parqueaderos (o sin saberlo aún) el backend decide
        val parqueaderoId = if (enVehiculo) parqueaderoMarcado() else null
        binding.btnRegistrarVisita.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val r = RetrofitClient.comun.registrarVisita(NuevaVisita(
                    torre, apto, nombre,
                    documento = binding.etDocumentoVisita.text?.toString()?.trim().orEmpty().ifEmpty { null },
                    placa = if (enVehiculo) placa else null,
                    motivo = binding.etMotivoVisita.text?.toString()?.trim().orEmpty().ifEmpty { null },
                    tipoIngreso = if (enVehiculo) "VEHICULO" else "PEATONAL",
                    parqueaderoId = parqueaderoId
                ))
                if (_binding == null) return@launch
                if (r.isSuccessful) {
                    listOf(binding.etNombreVisita, binding.etDocumentoVisita, binding.etPlacaVisita, binding.etMotivoVisita)
                        .forEach { it.setText("") }
                    binding.grupoIngreso.check(R.id.btnIngresoPeatonal)
                    aviso(R.string.porteria_registrada).show()
                    cargar()
                } else {
                    aviso(RetrofitClient.mensajeDeError(r, getString(R.string.porteria_error))).show()
                }
            } catch (e: Exception) {
                if (_binding != null) aviso(RetrofitClient.mensajeDeExcepcion(e)).show()
            }
            if (_binding != null) binding.btnRegistrarVisita.isEnabled = true
        }
    }

    private fun enVehiculo() = binding.grupoIngreso.checkedButtonId == R.id.btnIngresoVehiculo

    private fun mostrarVehiculo() {
        binding.grupoVehiculo.isVisible = enVehiculo()
        if (!enVehiculo()) binding.tilPlacaVisita.error = null
    }

    private fun parqueaderoMarcado(): Long? = binding.chipsParqueadero.checkedChipId.takeIf { it != View.NO_ID }
        ?.let { binding.chipsParqueadero.findViewById<Chip>(it)?.tag as? Long }

    /** Los chips de los parqueaderos libres; se conserva el que estaba marcado si sigue libre. */
    private fun pintarChips(lista: List<ParqueaderoInvitadoDTO>) {
        val marcado = parqueaderoMarcado()
        val libres = lista.filter { it.libre }
        val elegido = libres.firstOrNull { it.id == marcado } ?: libres.firstOrNull()
        binding.chipsParqueadero.removeAllViews()
        libres.forEach { p ->
            binding.chipsParqueadero.addView(Chip(requireContext()).apply {
                id = View.generateViewId()
                tag = p.id
                text = p.numero
                isCheckable = true
                isCheckedIconVisible = true
                chipIcon = ContextCompat.getDrawable(context, R.drawable.ic_car)
                isChipIconVisible = true
                isChecked = p.id == elegido?.id
            })
        }
        binding.tvSinParqueadero.isVisible = libres.isEmpty()
        binding.tvSinParqueadero.setText(if (lista.isEmpty()) R.string.ingreso_sin_parqueaderos else R.string.ingreso_todos_ocupados)
    }

    private fun pintarParqueaderos(lista: List<ParqueaderoInvitadoDTO>) {
        binding.seccionParqueaderos.isVisible = lista.isNotEmpty()
        binding.tvResumenParqueaderos.text = getString(R.string.parqueaderos_resumen, lista.count { it.libre }, lista.size)
        binding.listaParqueaderos.removeAllViews()
        lista.forEach { p ->
            val fila = ItemParqueaderoInvitadoBinding.inflate(layoutInflater, binding.listaParqueaderos, false)
            fila.tvNumero.text = p.numero
            val fondo = if (p.libre) com.google.android.material.R.attr.colorSecondaryContainer else com.google.android.material.R.attr.colorErrorContainer
            val texto = if (p.libre) com.google.android.material.R.attr.colorOnSecondaryContainer else com.google.android.material.R.attr.colorOnErrorContainer
            fila.tvNumero.background = ContextCompat.getDrawable(requireContext(), R.drawable.fondo_etiqueta_parqueadero)?.mutate()?.apply {
                setTint(MaterialColors.getColor(fila.root, fondo))
            }
            fila.tvNumero.setTextColor(MaterialColors.getColor(fila.root, texto))
            if (p.libre) {
                fila.tvEstado.setText(R.string.parqueadero_libre)
                fila.tvDetalle.isVisible = false
                fila.btnSalida.isVisible = false
            } else {
                fila.tvEstado.text = getString(R.string.parqueadero_ocupado, p.placa.orEmpty(), p.visitante.orEmpty()) +
                    if (p.estado == "PENDIENTE") " " + getString(R.string.parqueadero_esperando) else ""
                fila.tvDetalle.text = getString(R.string.parqueadero_ocupado_detalle, p.torre.orEmpty(), p.apto.orEmpty(),
                    p.desde?.substringAfter('T')?.take(5).orEmpty())
                fila.btnSalida.setOnClickListener {
                    val visita = p.visitaId ?: return@setOnClickListener
                    confirmar(getString(R.string.ingreso_salida_confirmar, p.visitante.orEmpty(), p.placa.orEmpty()), null,
                        R.string.ingreso_registrar_salida) { salida(visita, p.numero) }
                }
            }
            binding.listaParqueaderos.addView(fila.root)
        }
    }

    private fun salida(visitaId: Long, numero: String?) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val r = RetrofitClient.comun.registrarSalidaVisita(visitaId)
                if (_binding == null) return@launch
                aviso(if (r.isSuccessful) {
                    if (numero != null) getString(R.string.ingreso_salida_ok, numero) else getString(R.string.ingreso_salida_ok_sin_puesto)
                } else RetrofitClient.mensajeDeError(r, getString(R.string.porteria_error))).show()
                cargar()
            } catch (e: Exception) {
                if (_binding != null) aviso(RetrofitClient.mensajeDeExcepcion(e)).show()
            }
        }
    }

    private suspend fun cargarParqueaderos() {
        try {
            val r = RetrofitClient.comun.getParqueaderosInvitados()
            val lista = r.body()
            if (_binding != null && r.isSuccessful && lista != null) {
                pintarChips(lista)
                pintarParqueaderos(lista)
            }
        } catch (_: Exception) {
            // Sin conexión: se vuelve a intentar en el siguiente ciclo
        }
    }

    private suspend fun cargar() {
        cargarParqueaderos()
        try {
            val r = RetrofitClient.comun.getVisitas()
            val lista = r.body()
            if (_binding != null && r.isSuccessful && lista != null) pintar(lista)
        } catch (_: Exception) {
            // Sin conexión: se vuelve a intentar en el siguiente ciclo
        }
    }

    private fun pintar(visitas: List<VisitaDTO>) {
        binding.tvSinVisitas.visibility = if (visitas.isEmpty()) View.VISIBLE else View.GONE
        binding.listaVisitas.removeAllViews()
        visitas.forEach { v ->
            val fila = ItemVisitaBinding.inflate(layoutInflater, binding.listaVisitas, false)
            fila.tvVisitaTitulo.text = getString(R.string.porteria_visita_titulo, v.visitante, v.torre, v.apto)
            // P8 bloque 4: a pie o la placa con su parqueadero; y si ya salió
            val ingreso = when {
                v.tipoIngreso == "VEHICULO" && v.parqueadero != null -> getString(R.string.ingreso_en_vehiculo, v.placa.orEmpty(), v.parqueadero)
                v.tipoIngreso == "VEHICULO" -> v.placa
                else -> getString(R.string.ingreso_a_pie)
            }
            fila.tvVisitaDetalle.text = listOfNotNull(
                v.creadaEn.substringAfter('T').take(5), ingreso, v.motivo, v.documento,
                v.salidaEn?.let { getString(R.string.ingreso_salio, it.substringAfter('T').take(5)) },
                v.guardia?.let { getString(R.string.porteria_registro_de, it) }
            ).joinToString(" · ")
            fila.tvVisitaEstado.text = when (v.estado) {
                "AUTORIZADA" -> getString(R.string.porteria_estado_autorizada, canal(v))
                "RECHAZADA" -> getString(R.string.porteria_estado_rechazada, canal(v))
                "EXPIRADA" -> getString(R.string.porteria_estado_expirada)
                else -> getString(R.string.porteria_estado_pendiente)
            }
            val pendiente = v.estado == "PENDIENTE"
            fila.accionesVisita.visibility = if (pendiente) View.VISIBLE else View.GONE
            fila.btnLlamarApto.isEnabled = !v.telefono.isNullOrBlank()
            fila.btnLlamarApto.setOnClickListener { llamar(v.telefono) }
            fila.btnRespuestaLlamada.setOnClickListener { respuestaPorLlamada(v) }
            binding.listaVisitas.addView(fila.root)
        }
    }

    private fun canal(v: VisitaDTO) = getString(if (v.canal == "LLAMADA") R.string.porteria_por_llamada else R.string.porteria_por_app)

    /** Abre el marcador con el teléfono del apartamento (no llama solo). */
    private fun llamar(telefono: String?) {
        if (telefono.isNullOrBlank()) return
        try {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$telefono")))
        } catch (_: ActivityNotFoundException) {
            aviso(R.string.porteria_sin_marcador).show()
        }
    }

    private fun respuestaPorLlamada(v: VisitaDTO) {
        elegirOpcion(getString(R.string.porteria_respuesta_llamada_titulo, v.visitante), listOf(
            R.string.porteria_autorizar to { responder(v, "AUTORIZAR") },
            R.string.porteria_rechazar to { responder(v, "RECHAZAR") }
        ))
    }

    private fun responder(v: VisitaDTO, decision: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val r = RetrofitClient.comun.responderVisita(v.id, RespuestaVisita(decision))
                if (_binding == null) return@launch
                if (!r.isSuccessful) aviso(RetrofitClient.mensajeDeError(r, getString(R.string.porteria_error))).show()
                cargar()
            } catch (e: Exception) {
                if (_binding != null) aviso(RetrofitClient.mensajeDeExcepcion(e)).show()
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    private companion object {
        const val INTERVALO_MS = 7_000L
    }
}
