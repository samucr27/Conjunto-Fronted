package com.example.conjuntoresidencial.ui.admin

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.FragmentPorteriaBinding
import com.example.conjuntoresidencial.databinding.ItemVisitaBinding
import com.example.conjuntoresidencial.model.NuevaVisita
import com.example.conjuntoresidencial.model.RespuestaVisita
import com.example.conjuntoresidencial.model.VisitaDTO
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.elegirOpcion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pantalla de la portería (P6): registra una visita, al apartamento le llega un aviso con 2
 * minutos para responder, y esta lista se actualiza sola cada 7 segundos. Si el residente
 * contesta por teléfono, el portero registra su respuesta ("por llamada").
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
        configurarEncabezado(binding.encabezado, R.string.porteria_titulo, R.string.porteria_descripcion, atras = false)
        binding.btnRegistrarVisita.setOnClickListener { registrar() }
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
        if (torre.isEmpty() || apto.isEmpty() || nombre.isEmpty()) return
        binding.btnRegistrarVisita.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val r = RetrofitClient.comun.registrarVisita(NuevaVisita(
                    torre, apto, nombre,
                    documento = binding.etDocumentoVisita.text?.toString()?.trim().orEmpty().ifEmpty { null },
                    placa = binding.etPlacaVisita.text?.toString()?.trim().orEmpty().ifEmpty { null },
                    motivo = binding.etMotivoVisita.text?.toString()?.trim().orEmpty().ifEmpty { null }
                ))
                if (_binding == null) return@launch
                if (r.isSuccessful) {
                    listOf(binding.etNombreVisita, binding.etDocumentoVisita, binding.etPlacaVisita, binding.etMotivoVisita)
                        .forEach { it.setText("") }
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

    private suspend fun cargar() {
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
            fila.tvVisitaDetalle.text = listOfNotNull(
                v.creadaEn.substringAfter('T').take(5), v.placa, v.motivo, v.documento
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
