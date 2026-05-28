package com.example.conjuntoresidencial.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AdminNotificacion(
    val id: Long,
    val tipo: String,       // "VEHICULO" o "RESERVA"
    val mensaje: String,
    val fecha: String,
    val leida: Boolean
)

object AdminNotificacionManager {

    private const val PREFS_NAME = "admin_notificaciones"
    private const val KEY_NOTIFICACIONES = "notificaciones"
    private const val KEY_CONTADOR = "contador"

    fun agregarNotificacion(context: Context, tipo: String, mensaje: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = getJsonArray(prefs)
        val id = prefs.getLong(KEY_CONTADOR, 0L) + 1
        val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

        val obj = JSONObject().apply {
            put("id", id)
            put("tipo", tipo)
            put("mensaje", mensaje)
            put("fecha", fecha)
            put("leida", false)
        }
        array.put(obj)

        prefs.edit()
            .putString(KEY_NOTIFICACIONES, array.toString())
            .putLong(KEY_CONTADOR, id)
            .apply()
    }

    fun getNotificaciones(context: Context): List<AdminNotificacion> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = getJsonArray(prefs)
        val lista = mutableListOf<AdminNotificacion>()
        for (i in array.length() - 1 downTo 0) {          // más reciente primero
            val obj = array.getJSONObject(i)
            lista.add(
                AdminNotificacion(
                    id = obj.getLong("id"),
                    tipo = obj.getString("tipo"),
                    mensaje = obj.getString("mensaje"),
                    fecha = obj.getString("fecha"),
                    leida = obj.getBoolean("leida")
                )
            )
        }
        return lista
    }

    fun contarNoLeidas(context: Context): Int {
        return getNotificaciones(context).count { !it.leida }
    }

    fun marcarTodasLeidas(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = getJsonArray(prefs)
        val nuevo = JSONArray()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            obj.put("leida", true)
            nuevo.put(obj)
        }
        prefs.edit().putString(KEY_NOTIFICACIONES, nuevo.toString()).apply()
    }

    fun limpiarTodas(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    private fun getJsonArray(prefs: android.content.SharedPreferences): JSONArray {
        val raw = prefs.getString(KEY_NOTIFICACIONES, null) ?: return JSONArray()
        return try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
    }
}