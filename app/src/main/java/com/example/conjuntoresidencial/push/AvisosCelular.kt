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
import com.example.conjuntoresidencial.BuildConfig
import com.example.conjuntoresidencial.MainActivity
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.model.DispositivoRequest
import com.example.conjuntoresidencial.ui.comun.CategoriaAviso
import com.example.conjuntoresidencial.ui.comun.DestinoNotificacion
import com.example.conjuntoresidencial.ui.comun.TipoAviso
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

    // P6: un canal por app, para ajustar el sonido de cada una por separado
    private val CANAL = "avisos_" + BuildConfig.TIPO_APP.lowercase()
    private const val TAG = "AvisosCelular"
    private const val PREFS = "ajustes"
    private const val CLAVE_ACTIVADOS = "avisos_activados"
    // P9 bloque 1: un grupo por categoría y el id de su resumen (no se cruza con los demás)
    private const val GRUPO = "avisos_"
    private const val BASE_RESUMEN = 0x3000_0000
    // Los registros no deben depender de la pantalla: siguen aunque se cierre
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun disponible(context: Context): Boolean = FirebaseApp.getApps(context).isNotEmpty()

    /** Cuenta → Ajustes → "Avisos en este celular" (sí por defecto). Va aparte de la sesión. */
    fun activados(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(CLAVE_ACTIVADOS, true)

    fun cambiarActivados(context: Context, activados: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(CLAVE_ACTIVADOS, activados).apply()
        if (activados) registrar(context) else darDeBaja(context)
    }

    /** Canal "Avisos" (Android 8+): el usuario puede silenciarlo desde los ajustes del sistema. */
    fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val canal = NotificationChannel(CANAL, context.getString(R.string.canal_avisos), NotificationManager.IMPORTANCE_HIGH)
            .apply { description = context.getString(R.string.canal_avisos_descripcion) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    /** Asocia este celular a la sesión actual para que le lleguen sus avisos. */
    fun registrar(context: Context) {
        if (!disponible(context) || !activados(context)) return
        alcance.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                enviarRegistro(context, token)
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo registrar el celular para avisos", e)
            }
        }
    }

    /** Firebase cambió el token (ServicioMensajes): se vuelve a registrar si hay sesión. */
    internal fun enviarRegistro(context: Context, token: String) {
        if (!activados(context)) return
        alcance.launch {
            try {
                RetrofitClient.comun.registrarDispositivo(DispositivoRequest(token, BuildConfig.TIPO_APP, BuildConfig.APPLICATION_ID))
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
                RetrofitClient.comun.darDeBajaDispositivo(DispositivoRequest(token, BuildConfig.TIPO_APP, BuildConfig.APPLICATION_ID))
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo dar de baja el celular", e)
            }
        }
    }

    /** Muestra el aviso; al tocarlo, MainActivity abre la pantalla correspondiente. */
    fun mostrar(context: Context, titulo: String, mensaje: String, destino: DestinoNotificacion?, abrirEn: Intent? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val abrir = abrirEn?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) ?: Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        destino?.ponerEn(abrir)
        // Un id por notificación: cada aviso abre lo suyo y no se pisan entre sí
        val id = destino?.notificacionId?.toInt() ?: System.currentTimeMillis().toInt()
        val alTocar = PendingIntent.getActivity(
            context, id, abrir, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // P9 bloque 1: ícono, color y categoría según el tipo; título corto y el detalle completo al desplegar
        val tipo = TipoAviso.de(destino?.tipo)
        val categoria = tipo.categoria
        val tituloClaro = if (tipo.titulo != R.string.aviso_general) context.getString(tipo.titulo) else titulo
        val detalle = TipoAviso.detalle(context, mensaje, tipo)
        val color = ContextCompat.getColor(context, categoria.color)
        val grupo = GRUPO + categoria.name
        val aviso = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(categoria.icono)
            .setColor(color)
            .setContentTitle(tituloClaro)
            .setContentText(detalle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detalle).setBigContentTitle(tituloClaro))
            // Arriba, junto al nombre de la app: "MiConjunto · Reservas"
            .setSubText(context.getString(categoria.nombre))
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setGroup(grupo)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(if (categoria == CategoriaAviso.CHAT) NotificationCompat.CATEGORY_MESSAGE else NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(alTocar)
            .build()
        val gestor = NotificationManagerCompat.from(context)
        gestor.notify(id, aviso)
        resumirGrupo(context, categoria, grupo, color)
    }

    /**
     * Con dos o más avisos de la misma categoría, el celular los junta bajo uno solo
     * ("3 avisos de Reservas") con la primera línea de cada uno; al desplegarlo se ven todos.
     */
    private fun resumirGrupo(context: Context, categoria: CategoriaAviso, grupo: String, color: Int) {
        val activos = context.getSystemService(NotificationManager::class.java)?.activeNotifications.orEmpty()
            .filter { it.notification.group == grupo && it.id != idResumen(categoria) }
        if (activos.size < 2) return
        val nombre = context.getString(categoria.nombre)
        val estilo = NotificationCompat.InboxStyle()
            .setSummaryText(context.resources.getQuantityString(R.plurals.aviso_grupo_resumen, activos.size, activos.size, nombre))
        activos.sortedByDescending { it.postTime }.take(5).forEach { n ->
            val extras = n.notification.extras
            val linea = listOfNotNull(extras.getCharSequence(android.app.Notification.EXTRA_TITLE),
                extras.getCharSequence(android.app.Notification.EXTRA_TEXT)).joinToString(" · ")
            estilo.addLine(linea)
        }
        val resumen = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(categoria.icono)
            .setColor(color)
            .setContentTitle(nombre)
            .setContentText(context.resources.getQuantityString(R.plurals.aviso_grupo_resumen, activos.size, activos.size, nombre))
            .setSubText(nombre)
            .setStyle(estilo)
            .setGroup(grupo)
            .setGroupSummary(true)
            // Suena una vez el aviso nuevo, no también el resumen
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(idResumen(categoria), resumen)
    }

    private fun idResumen(categoria: CategoriaAviso) = BASE_RESUMEN + categoria.ordinal
}
