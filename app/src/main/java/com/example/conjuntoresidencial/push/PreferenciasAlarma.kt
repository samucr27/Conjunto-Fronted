package com.example.conjuntoresidencial.push

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.edit

/**
 * P10: cómo avisa este celular cuando llega una visita: con sonido y vibración, solo sonido o
 * solo vibración, y con qué tono (el de alarma del celular si no se eligió otro). Lo cambia el
 * residente en Cuenta → Ajustes. En Android 8+ el sonido es del canal y no se puede cambiar
 * después de creado, así que cada combinación tiene su propio canal ([idCanal]).
 */
object PreferenciasAlarma {

    enum class Modo { SONIDO_Y_VIBRACION, SOLO_SONIDO, SOLO_VIBRACION }

    private const val ARCHIVO = "alarma_visita"
    private const val CLAVE_MODO = "modo"
    private const val CLAVE_TONO = "tono"

    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    fun modo(c: Context): Modo =
        prefs(c).getString(CLAVE_MODO, null)?.let { runCatching { Modo.valueOf(it) }.getOrNull() } ?: Modo.SONIDO_Y_VIBRACION

    fun ponerModo(c: Context, m: Modo) = prefs(c).edit { putString(CLAVE_MODO, m.name) }

    /** El tono elegido o null (el de alarma del celular). */
    fun tonoElegido(c: Context): Uri? = prefs(c).getString(CLAVE_TONO, null)?.let(Uri::parse)

    fun ponerTono(c: Context, tono: Uri?) = prefs(c).edit { putString(CLAVE_TONO, tono?.toString()) }

    fun suena(c: Context) = modo(c) != Modo.SOLO_VIBRACION
    fun vibra(c: Context) = modo(c) != Modo.SOLO_SONIDO

    /** El tono que suena: el elegido o el de alarma (o el de llamada o notificación si no hay). */
    fun tono(c: Context): Uri? = tonoElegido(c)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    /** Nombre del tono para mostrarlo en Ajustes ("Radar", "Predeterminado"...). */
    fun nombreTono(c: Context): String? = tonoElegido(c)?.let { uri ->
        runCatching { RingtoneManager.getRingtone(c, uri)?.getTitle(c) }.getOrNull()
    }

    /** Un canal por combinación de modo y tono: si cambian, se usa (y se crea) otro. */
    fun idCanal(c: Context, base: String): String =
        base + "_" + modo(c).name.lowercase() + "_" + (tonoElegido(c)?.toString()?.hashCode()?.toUInt()?.toString(16) ?: "def")

    /** Borra los canales viejos de la alarma para que en Ajustes del sistema no se acumulen. */
    fun limpiarCanales(c: Context, base: String, actual: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.notificationChannels.filter { it.id.startsWith(base) && it.id != actual }.forEach { nm.deleteNotificationChannel(it.id) }
    }

    /**
     * Android 14+: la alerta a pantalla completa (encima del bloqueo) necesita que el usuario la
     * permita a las apps que no son de llamadas ni alarmas. true si ya se puede.
     */
    fun puedePantallaCompleta(c: Context): Boolean =
        Build.VERSION.SDK_INT < 34 || c.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

    /** La pantalla del sistema para permitirla. */
    fun intentPantallaCompleta(c: Context): Intent =
        if (Build.VERSION.SDK_INT >= 34) Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:" + c.packageName))
        else Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, c.packageName)
}
