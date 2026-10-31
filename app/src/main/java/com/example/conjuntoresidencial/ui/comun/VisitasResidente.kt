package com.example.conjuntoresidencial.ui.comun

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.model.RespuestaVisita
import com.example.conjuntoresidencial.model.VisitaDTO
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * P6 bloque 5: mientras Inicio está a la vista, el residente consulta cada 7 segundos si la
 * portería registró una visita para su apartamento y, si hay, la muestra para autorizarla o
 * rechazarla (el aviso del celular lleva a esta misma pantalla). Tiene 2 minutos.
 */
fun Fragment.vigilarVisitas() {
    var abierta: Pair<Long, AlertDialog>? = null
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val pendientes = try {
                    RetrofitClient.comun.getVisitasPendientes().body().orEmpty()
                } catch (_: Exception) {
                    null
                }
                if (pendientes != null && view != null) {
                    // Si la que se muestra ya expiró o la respondieron en otro celular, se quita
                    abierta?.let { (id, dialogo) ->
                        if (pendientes.none { it.id == id }) { dialogo.dismiss(); abierta = null }
                    }
                    if (abierta == null) {
                        pendientes.firstOrNull()?.let { v -> abierta = v.id to mostrar(v) { abierta = null } }
                    }
                }
                delay(7_000L)
            }
        }
    }
}

private fun Fragment.mostrar(v: VisitaDTO, alCerrar: () -> Unit): AlertDialog {
    val detalle = listOfNotNull(v.documento?.let { getString(R.string.porteria_doc_linea, it) }, v.placa, v.motivo)
        .joinToString("\n")
    return MaterialAlertDialogBuilder(requireContext())
        .setIcon(R.drawable.ic_people)
        .setTitle(getString(R.string.porteria_residente_titulo, v.visitante))
        .setMessage(listOf(getString(R.string.porteria_residente_mensaje), detalle).filter { it.isNotBlank() }.joinToString("\n\n"))
        .setCancelable(false)
        .setPositiveButton(R.string.porteria_autorizar) { _, _ -> responder(v, "AUTORIZAR") }
        .setNegativeButton(R.string.porteria_rechazar) { _, _ -> responder(v, "RECHAZAR") }
        .setOnDismissListener { alCerrar() }
        .show()
}

private fun Fragment.responder(v: VisitaDTO, decision: String) {
    viewLifecycleOwner.lifecycleScope.launch {
        try {
            val r = RetrofitClient.comun.responderVisita(v.id, RespuestaVisita(decision))
            if (view == null) return@launch
            // 409: ya expiró o la respondió otra persona del apartamento
            aviso(if (r.isSuccessful) getString(if (decision == "AUTORIZAR") R.string.porteria_autorizada_ok else R.string.porteria_rechazada_ok)
                  else RetrofitClient.mensajeDeError(r, getString(R.string.porteria_error))).show()
        } catch (e: Exception) {
            if (view != null) aviso(RetrofitClient.mensajeDeExcepcion(e)).show()
        }
    }
}
