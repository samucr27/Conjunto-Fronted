package com.example.conjuntoresidencial.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.conjuntoresidencial.BuildConfig
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.model.RespuestaVisita
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * P8 bloque 3: cuando la portería anuncia una visita, el celular del residente suena y vibra
 * como una alarma (aunque la app esté cerrada o la pantalla bloqueada) hasta que responda o
 * pase [DURACION_MS]. Se ve a pantalla completa ([VisitaAlarmaActivity]) y el aviso trae
 * Autorizar y Rechazar para contestar sin abrir la app ([RespuestaVisitaReceiver]).
 */
object AlarmaVisita {

    const val DURACION_MS = 60_000L
    const val EXTRA_VISITA = "visita_id"
    const val EXTRA_TITULO = "visita_titulo"
    const val EXTRA_MENSAJE = "visita_mensaje"
    const val EXTRA_DECISION = "visita_decision"
    const val AUTORIZAR = "AUTORIZAR"
    const val RECHAZAR = "RECHAZAR"

    // Canal propio: el sonido y la vibración de un canal no se pueden cambiar después de creado
    private val CANAL = "alarma_visita_" + BuildConfig.TIPO_APP.lowercase()
    // Para que no se cruce con los ids de las demás notificaciones (el id de la notificación)
    private const val BASE_ID = 0x4000_0000

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun idNotificacion(visitaId: Long): Int = BASE_ID + (visitaId % 0x3FFF_FFFF).toInt()

    fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val canal = NotificationChannel(CANAL, context.getString(R.string.canal_alarma_visita), NotificationManager.IMPORTANCE_HIGH).apply {
            description = context.getString(R.string.canal_alarma_visita_descripcion)
            setSound(sonido(), AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build())
            enableVibration(true)
            vibrationPattern = VIBRACION
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            setBypassDnd(true)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    private val VIBRACION = longArrayOf(0, 800, 400, 800, 400, 800, 1200)

    private fun sonido() = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    /** Hace sonar la alarma de la visita [visitaId]. */
    fun sonar(context: Context, visitaId: Long, titulo: String, mensaje: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        crearCanal(context)
        val id = idNotificacion(visitaId)

        val pantalla = Intent(context, VisitaAlarmaActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
            .putExtra(EXTRA_VISITA, visitaId)
            .putExtra(EXTRA_TITULO, titulo)
            .putExtra(EXTRA_MENSAJE, mensaje)
        val abrirPantalla = PendingIntent.getActivity(context, id, pantalla,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val aviso = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_people)
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // Antes de Android 8 el sonido y la vibración van en la notificación y no en el canal
            .setSound(sonido(), android.media.AudioManager.STREAM_ALARM)
            .setVibrate(VIBRACION)
            .setOngoing(true)
            .setAutoCancel(false)
            .setTimeoutAfter(DURACION_MS)
            .setContentIntent(abrirPantalla)
            .setFullScreenIntent(abrirPantalla, true)
            .addAction(0, context.getString(R.string.porteria_rechazar), accion(context, visitaId, RECHAZAR))
            .addAction(0, context.getString(R.string.porteria_autorizar), accion(context, visitaId, AUTORIZAR))
            .build()
            // Repite el sonido hasta que se responda o se quite el aviso
            .apply { flags = flags or android.app.Notification.FLAG_INSISTENT }
        NotificationManagerCompat.from(context).notify(id, aviso)
    }

    private fun accion(context: Context, visitaId: Long, decision: String): PendingIntent {
        val intent = Intent(context, RespuestaVisitaReceiver::class.java)
            .putExtra(EXTRA_VISITA, visitaId)
            .putExtra(EXTRA_DECISION, decision)
        // Un request code por visita y decisión: si no, los dos botones serían el mismo
        val codigo = idNotificacion(visitaId) xor (if (decision == AUTORIZAR) 1 else 2)
        return PendingIntent.getBroadcast(context, codigo, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    /** Quita el aviso y calla la alarma (respondida aquí, en Inicio o en otro celular). */
    fun detener(context: Context, visitaId: Long) {
        NotificationManagerCompat.from(context).cancel(idNotificacion(visitaId))
    }

    /**
     * Contesta la visita. [alTerminar] recibe el texto para mostrar (en el hilo principal).
     * Sin sesión en el celular el backend responde 401 y se pide abrir la app.
     */
    fun responder(context: Context, visitaId: Long, decision: String, alTerminar: (ok: Boolean, texto: String) -> Unit) {
        val app = context.applicationContext
        alcance.launch {
            val (ok, texto) = try {
                val r = RetrofitClient.comun.responderVisita(visitaId, RespuestaVisita(decision))
                when {
                    r.isSuccessful -> true to app.getString(if (decision == AUTORIZAR) R.string.porteria_autorizada_ok else R.string.porteria_rechazada_ok)
                    r.code() == 401 || r.code() == 403 -> false to app.getString(R.string.alarma_sin_sesion)
                    // 409: ya expiró o la respondió otra persona del apartamento
                    else -> true to RetrofitClient.mensajeDeError(r, app.getString(R.string.porteria_error))
                }
            } catch (e: Exception) {
                false to RetrofitClient.mensajeDeExcepcion(e)
            }
            if (ok) detener(app, visitaId)
            withContext(Dispatchers.Main) { alTerminar(ok, texto) }
        }
    }
}

/** Autorizar o Rechazar desde el aviso, sin abrir la app. */
class RespuestaVisitaReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val visita = intent.getLongExtra(AlarmaVisita.EXTRA_VISITA, -1)
        val decision = intent.getStringExtra(AlarmaVisita.EXTRA_DECISION) ?: return
        if (visita < 0) return
        val pendiente = goAsync()
        AlarmaVisita.responder(context, visita, decision) { _, texto ->
            // Sin sesión o sin red la alarma sigue: al tocarla se abre la pantalla completa
            Toast.makeText(context.applicationContext, texto, Toast.LENGTH_LONG).show()
            pendiente.finish()
        }
    }
}
