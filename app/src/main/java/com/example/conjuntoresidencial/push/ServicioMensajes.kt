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
        AvisosCelular.mostrar(
            this,
            datos["titulo"] ?: getString(R.string.app_name),
            datos["mensaje"].orEmpty(),
            DestinoNotificacion.de(datos)
        )
    }

    override fun onNewToken(token: String) {
        // Solo con sesión: si no, se registra al iniciarla (MainActivity)
        if (SessionManager(this).getToken() != null) AvisosCelular.enviarRegistro(this, token)
    }
}
