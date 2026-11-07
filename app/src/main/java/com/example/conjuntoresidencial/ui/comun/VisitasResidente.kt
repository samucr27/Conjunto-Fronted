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
import coil.load

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
                        if (pendientes.none { it.id == id }) {
                            dialogo.dismiss(); abierta = null
                            context?.let { com.example.conjuntoresidencial.push.AlarmaVisita.detener(it, id) }
                        }
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
    // P10 bloque 7: la foto de la cara que tomó la portería, grande, para saber quién es
    val foto = v.fotoUrl?.let { url ->
        val ctx = requireContext()
        val alto = (240 * ctx.resources.displayMetrics.density).toInt()
        val margen = (24 * ctx.resources.displayMetrics.density).toInt()
        android.widget.FrameLayout(ctx).apply {
            setPadding(margen, margen / 2, margen, 0)
            addView(com.google.android.material.imageview.ShapeableImageView(ctx).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, alto)
                scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                shapeAppearanceModel = shapeAppearanceModel.withCornerSize(18 * ctx.resources.displayMetrics.density)
                contentDescription = getString(R.string.foto_del_visitante, v.visitante)
                load(url) { crossfade(true); placeholder(R.drawable.fondo_imagen_cargando) }
                setOnClickListener { FotosPorteria.verGrande(ctx, url, contentDescription) }
            })
        }
    }
    return MaterialAlertDialogBuilder(requireContext())
        .setIcon(R.drawable.ic_people)
        .setTitle(getString(R.string.porteria_residente_titulo, v.visitante))
        .apply { if (foto != null) setView(foto) }
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
            // P8 bloque 3: contestada aquí, la alarma del celular deja de sonar
            context?.let { com.example.conjuntoresidencial.push.AlarmaVisita.detener(it, v.id) }
            if (view == null) return@launch
            // 409: ya expiró o la respondió otra persona del apartamento
            val barra = aviso(if (r.isSuccessful) getString(if (decision == "AUTORIZAR") R.string.porteria_autorizada_ok else R.string.porteria_rechazada_ok)
                  else RetrofitClient.mensajeDeError(r, getString(R.string.porteria_error)))
            // P10 bloque 7: rechazada, puede escribirle a la portería
            if (r.isSuccessful && decision == "RECHAZAR") {
                barra.setDuration(12_000)
                    .setAction(R.string.chat_visita_abrir) { context?.let { ChatVisitaActivity.abrir(it, v.id) } }
            }
            barra.show()
        } catch (e: Exception) {
            if (view != null) aviso(RetrofitClient.mensajeDeExcepcion(e)).show()
        }
    }
}
