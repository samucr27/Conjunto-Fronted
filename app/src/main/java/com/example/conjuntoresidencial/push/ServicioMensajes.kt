package com.example.conjuntoresidencial.push

import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.ui.comun.DestinoNotificacion
import com.example.conjuntoresidencial.util.SessionManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Recibe los avisos de Firebase. El backend manda mensajes solo de datos, así que llegan aquí
 * con la app abierta o cerrada y la notificación siempre se arma igual (AvisosCelular).
 */
class ServicioMensajes : FirebaseMessagingService() {

    override fun onMessageReceived(mensaje: RemoteMessage) {
        // Desactivados en Ajustes: el celular ya se dio de baja; por si llega uno en camino
        if (!AvisosCelular.activados(this)) return
        val datos = mensaje.data
        // P8 bloque 3: la visita en portería suena como una alarma, con Autorizar y Rechazar
        val visita = datos["referenciaId"]?.toLongOrNull()
        if (datos["tipo"] == DestinoNotificacion.TIPO_VISITA && visita != null) {
            AlarmaVisita.sonar(this, visita, datos["titulo"] ?: getString(R.string.alarma_titulo_generico), datos["mensaje"].orEmpty())
            return
        }
        // P12 bloque 3: alguien quiere recoger la correspondencia: la misma alarma
        if (datos["tipo"] == DestinoNotificacion.TIPO_ENTREGA && visita != null) {
            AlarmaVisita.sonar(this, visita, datos["titulo"] ?: getString(R.string.aviso_entrega_correspondencia),
                datos["mensaje"].orEmpty(), entrega = true)
            return
        }
        // P10 bloque 7: mensaje del chat de una visita rechazada (P12: o de una entrega): al tocarlo se abre ese chat
        val chat = when {
            visita == null -> null
            datos["tipo"] == TIPO_CHAT_VISITA -> com.example.conjuntoresidencial.ui.comun.ChatVisitaActivity.intent(this, visita)
            datos["tipo"] == TIPO_CHAT_ENTREGA -> com.example.conjuntoresidencial.ui.comun.ChatVisitaActivity.intent(this, visita, entrega = true)
            else -> null
        }
        AvisosCelular.mostrar(
            this,
            datos["titulo"] ?: getString(R.string.app_name),
            datos["mensaje"].orEmpty(),
            DestinoNotificacion.de(datos),
            chat
        ) { id ->
            // P12: "¿tu visitante sigue adentro?" con el botón Ya salió
            if (datos["tipo"] == DestinoNotificacion.TIPO_VISITA_SIGUE_DENTRO && visita != null)
                listOf(SalidaVisitaReceiver.accion(this, visita, id)) else emptyList()
        }
    }

    private companion object {
        const val TIPO_CHAT_VISITA = "CHAT_VISITA"
        const val TIPO_CHAT_ENTREGA = "CHAT_ENTREGA"
    }

    override fun onNewToken(token: String) {
        // Solo con sesión: si no, se registra al iniciarla (MainActivity)
        if (SessionManager(this).getToken() != null) AvisosCelular.enviarRegistro(this, token)
    }
}
