package com.example.conjuntoresidencial.ui.cuenta

import android.text.InputType
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.model.AceptarTextos
import com.example.conjuntoresidencial.model.NuevaSolicitudDatos
import com.example.conjuntoresidencial.model.ResolverSolicitudDatos
import com.example.conjuntoresidencial.model.SolicitudDatosDTO
import com.example.conjuntoresidencial.model.TextoLegalDTO
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.ejecutarAccion
import com.example.conjuntoresidencial.ui.comun.elegirOpcion
import com.example.conjuntoresidencial.ui.comun.pedirTexto
import com.example.conjuntoresidencial.util.confirmarCerrarSesion
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/*
 * P6 bloque 4: textos legales con versión (hay que aceptarlos), y las solicitudes de datos
 * personales (Ley 1581): el residente pide consultar o borrar sus datos y la administración responde.
 */

/** La sesión (su token) para la que ya se resolvió: no se vuelve a preguntar en cada vez que se abre Inicio. */
private object ConsentimientoPedido { var token: String? = null }

/**
 * Si hay textos legales vigentes sin aceptar, los muestra y pide aceptarlos. Quien no acepta
 * puede cerrar sesión. Si el servidor no responde no bloquea a nadie.
 */
fun Fragment.pedirConsentimientos() {
    val sesion = com.example.conjuntoresidencial.util.SessionManager(requireContext()).getToken()
    if (sesion == null || ConsentimientoPedido.token == sesion) return
    viewLifecycleOwner.lifecycleScope.launch {
        val pendientes = try {
            val r = RetrofitClient.comun.getTextosPendientes()
            if (r.isSuccessful) r.body().orEmpty() else return@launch
        } catch (_: Exception) {
            return@launch
        }
        if (view == null) return@launch
        if (pendientes.isEmpty()) {
            ConsentimientoPedido.token = sesion
            return@launch
        }
        mostrarTextos(pendientes, sesion)
    }
}

private fun Fragment.mostrarTextos(textos: List<TextoLegalDTO>, sesion: String) {
    val cuerpo = textos.joinToString("<br><br>") { "<b>${it.titulo}</b> (v${it.version})<br>${it.contenido}" }
    val vista = TextView(requireContext()).apply {
        text = HtmlCompat.fromHtml(cuerpo, HtmlCompat.FROM_HTML_MODE_COMPACT)
        val m = (24 * resources.displayMetrics.density).toInt()
        setPadding(m, m / 2, m, 0)
    }
    val scroll = android.widget.ScrollView(requireContext()).apply { addView(vista) }
    MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.consentimiento_titulo)
        .setView(scroll)
        .setCancelable(false)
        .setPositiveButton(R.string.consentimiento_acepto) { _, _ ->
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val r = RetrofitClient.comun.aceptarTextos(AceptarTextos(textos.map { it.id }))
                    if (r.isSuccessful) ConsentimientoPedido.token = sesion
                    else if (view != null) aviso(RetrofitClient.mensajeDeError(r, getString(R.string.consentimiento_error))).show()
                } catch (e: Exception) {
                    if (view != null) aviso(RetrofitClient.mensajeDeExcepcion(e)).show()
                }
            }
        }
        .setNegativeButton(R.string.consentimiento_no_acepto) { _, _ -> confirmarCerrarSesion() }
        .show()
}

/** Cuenta → Legal → Mis datos personales: pedir consultarlos o borrarlos, y ver cómo van las solicitudes. */
fun Fragment.misDatosPersonales() {
    elegirOpcion(getString(R.string.datos_titulo), listOf(
        R.string.datos_consultar to { pedirDetalle("CONSULTA") },
        R.string.datos_suprimir to { pedirDetalle("SUPRESION") },
        R.string.datos_ver_mis_solicitudes to { verMisSolicitudes() }
    ))
}

private fun Fragment.pedirDetalle(tipo: String) {
    pedirTexto(getString(R.string.datos_detalle_titulo), R.string.datos_detalle_hint, null,
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES,
        validar = { null }) { detalle ->
        ejecutarAccion(getString(R.string.datos_enviada),
            { RetrofitClient.comun.solicitarDatos(NuevaSolicitudDatos(tipo, detalle.ifBlank { null })) })
    }
}

private fun Fragment.verMisSolicitudes() {
    viewLifecycleOwner.lifecycleScope.launch {
        val lista = try {
            val r = RetrofitClient.comun.getMisSolicitudesDatos()
            if (r.isSuccessful) r.body().orEmpty() else { aviso(RetrofitClient.mensajeDeError(r, getString(R.string.datos_error))).show(); return@launch }
        } catch (e: Exception) {
            if (view != null) aviso(RetrofitClient.mensajeDeExcepcion(e)).show()
            return@launch
        }
        if (view == null) return@launch
        val texto = if (lista.isEmpty()) getString(R.string.datos_sin_solicitudes)
            else lista.joinToString("\n\n") { resumen(it) + (it.respuesta?.let { r -> "\n" + getString(R.string.datos_respuesta, r) } ?: "") }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.datos_ver_mis_solicitudes)
            .setMessage(texto)
            .setPositiveButton(R.string.btn_cerrar, null)
            .show()
    }
}

/** Administración → Legal: las solicitudes de los residentes; tocar una pendiente para atenderla o rechazarla. */
fun Fragment.solicitudesDeDatos() {
    viewLifecycleOwner.lifecycleScope.launch {
        val lista = try {
            val r = RetrofitClient.comun.getSolicitudesDatos()
            if (r.isSuccessful) r.body().orEmpty() else { aviso(RetrofitClient.mensajeDeError(r, getString(R.string.datos_error))).show(); return@launch }
        } catch (e: Exception) {
            if (view != null) aviso(RetrofitClient.mensajeDeExcepcion(e)).show()
            return@launch
        }
        if (view == null) return@launch
        if (lista.isEmpty()) {
            MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.datos_solicitudes_admin)
                .setMessage(R.string.datos_sin_solicitudes).setPositiveButton(R.string.btn_cerrar, null).show()
            return@launch
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.datos_solicitudes_admin)
            .setItems(lista.map { resumen(it) }.toTypedArray()) { _, i -> resolver(lista[i]) }
            .setNegativeButton(R.string.btn_cerrar, null)
            .show()
    }
}

private fun Fragment.resolver(s: SolicitudDatosDTO) {
    if (s.estado != "PENDIENTE") {
        MaterialAlertDialogBuilder(requireContext()).setTitle(resumen(s))
            .setMessage(listOfNotNull(s.detalle, s.respuesta?.let { getString(R.string.datos_respuesta, it) }).joinToString("\n\n"))
            .setPositiveButton(R.string.btn_cerrar, null).show()
        return
    }
    MaterialAlertDialogBuilder(requireContext())
        .setTitle(resumen(s))
        .setMessage(s.detalle ?: getString(R.string.datos_sin_detalle))
        .setPositiveButton(R.string.datos_atender) { _, _ -> responder(s, "ATENDIDA") }
        .setNeutralButton(R.string.datos_rechazar) { _, _ -> responder(s, "RECHAZADA") }
        .setNegativeButton(R.string.btn_cancelar, null)
        .show()
}

private fun Fragment.responder(s: SolicitudDatosDTO, estado: String) {
    pedirTexto(getString(R.string.datos_respuesta_titulo), R.string.datos_respuesta_hint, null,
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES,
        validar = { null }) { respuesta ->
        ejecutarAccion(getString(R.string.datos_resuelta),
            { RetrofitClient.comun.resolverSolicitudDatos(s.id, ResolverSolicitudDatos(estado, respuesta.ifBlank { null })) })
    }
}

private fun Fragment.resumen(s: SolicitudDatosDTO): String {
    val tipo = getString(if (s.tipo == "SUPRESION") R.string.datos_tipo_supresion else R.string.datos_tipo_consulta)
    val estado = getString(when (s.estado) {
        "ATENDIDA" -> R.string.datos_estado_atendida
        "RECHAZADA" -> R.string.datos_estado_rechazada
        else -> R.string.datos_estado_pendiente
    })
    return listOfNotNull(s.nombre, tipo, estado, s.creadaEn.take(10)).joinToString(" · ")
}
