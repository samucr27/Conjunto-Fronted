package com.example.conjuntoresidencial.push

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * P12: el recordatorio "¿tu visitante sigue adentro?" trae el botón Ya salió, que da la salida
 * sin abrir la app. Si ya tenía salida (409) también se quita el aviso.
 */
class SalidaVisitaReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val visita = intent.getLongExtra(EXTRA_VISITA, -1)
        val aviso = intent.getIntExtra(EXTRA_AVISO, 0)
        if (visita < 0) return
        val app = context.applicationContext
        val pendiente = goAsync()
        alcance.launch {
            val (quitar, texto) = try {
                val r = RetrofitClient.comun.registrarSalidaVisita(visita)
                when {
                    r.isSuccessful -> true to app.getString(R.string.aviso_visita_salida_ok)
                    r.code() == 401 || r.code() == 403 -> false to app.getString(R.string.alarma_sin_sesion)
                    else -> (r.code() == 409 || r.code() == 404) to RetrofitClient.mensajeDeError(r, app.getString(R.string.porteria_error))
                }
            } catch (e: Exception) {
                false to RetrofitClient.mensajeDeExcepcion(e)
            }
            if (quitar) NotificationManagerCompat.from(app).cancel(aviso)
            withContext(Dispatchers.Main) {
                Toast.makeText(app, texto, Toast.LENGTH_LONG).show()
                pendiente.finish()
            }
        }
    }

    companion object {
        private const val EXTRA_VISITA = "salida_visita"
        private const val EXTRA_AVISO = "salida_aviso"
        private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** El botón Ya salió del aviso [aviso] (su id de notificación). */
        fun accion(context: Context, visita: Long, aviso: Int): NotificationCompat.Action {
            val intent = Intent(context, SalidaVisitaReceiver::class.java)
                .putExtra(EXTRA_VISITA, visita).putExtra(EXTRA_AVISO, aviso)
            val pi = PendingIntent.getBroadcast(context, aviso, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            return NotificationCompat.Action(R.drawable.ic_schedule, context.getString(R.string.aviso_visita_ya_salio), pi)
        }
    }
}
