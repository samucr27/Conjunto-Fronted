package com.example.conjuntoresidencial.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.conjuntoresidencial.MainActivity
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.model.DispositivoRequest
import com.example.conjuntoresidencial.ui.comun.DestinoNotificacion
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Avisos en el panel de notificaciones del celular (Firebase Cloud Messaging).
 *
 * Firebase es opcional: sin app/google-services.json la app compila y funciona igual, solo
 * que no llegan avisos ([disponible] = false). Ver la guía en docs/firebase.md.
 */
object AvisosCelular {

    private const val CANAL = "avisos"
    private const val TAG = "AvisosCelular"
    // Los registros no deben depender de la pantalla: siguen aunque se cierre
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun disponible(context: Context): Boolean = FirebaseApp.getApps(context).isNotEmpty()

    /** Canal "Avisos" (Android 8+): el usuario puede silenciarlo desde los ajustes del sistema. */
    fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val canal = NotificationChannel(CANAL, context.getString(R.string.canal_avisos), NotificationManager.IMPORTANCE_HIGH)
            .apply { description = context.getString(R.string.canal_avisos_descripcion) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    /** Asocia este celular a la sesión actual para que le lleguen sus avisos. */
    fun registrar(context: Context) {
        if (!disponible(context)) return
        alcance.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                enviarRegistro(token)
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo registrar el celular para avisos", e)
            }
        }
    }

    /** Firebase cambió el token (ServicioMensajes): se vuelve a registrar si hay sesión. */
    internal fun enviarRegistro(token: String) {
        alcance.launch {
            try {
                RetrofitClient.instance.registrarDispositivo(DispositivoRequest(token))
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo registrar el celular para avisos", e)
            }
        }
    }

    /** Al cerrar sesión: este celular deja de recibir avisos de esa cuenta. */
    fun darDeBaja(context: Context) {
        if (!disponible(context)) return
        alcance.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                RetrofitClient.instance.darDeBajaDispositivo(DispositivoRequest(token))
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo dar de baja el celular", e)
            }
        }
    }

    /** Muestra el aviso; al tocarlo, MainActivity abre la pantalla correspondiente. */
    fun mostrar(context: Context, titulo: String, mensaje: String, destino: DestinoNotificacion?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val abrir = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        destino?.ponerEn(abrir)
        // Un id por notificación: cada aviso abre lo suyo y no se pisan entre sí
        val id = destino?.notificacionId?.toInt() ?: System.currentTimeMillis().toInt()
        val alTocar = PendingIntent.getActivity(
            context, id, abrir, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val aviso = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_notifications)
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(alTocar)
            .build()
        NotificationManagerCompat.from(context).notify(id, aviso)
    }
}
