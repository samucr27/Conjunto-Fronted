package com.example.conjuntoresidencial.util

import android.content.Context
import androidx.core.content.edit

class SessionManager(context: Context) {
    // Este archivo está excluido de las copias de seguridad (ver res/xml/backup_rules.xml)
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveToken(token: String) = prefs.edit { putString("token", token) }
    fun getToken(): String? = prefs.getString("token", null)

    fun saveApartamentoId(id: Long) = prefs.edit { putLong("apartamento_id", id) }
    fun getApartamentoId(): Long = prefs.getLong("apartamento_id", -1L)

    fun saveTorre(torre: String) = prefs.edit { putString("torre", torre) }
    fun getTorre(): String = prefs.getString("torre", "") ?: ""

    fun saveApto(apto: String) = prefs.edit { putString("apto", apto) }
    fun getApto(): String = prefs.getString("apto", "") ?: ""

    fun getTorreApto(): String {
        val torre = getTorre()
        val apto = getApto()
        return if (torre.isNotEmpty() && apto.isNotEmpty()) "Torre $torre Apto $apto" else ""
    }

    fun saveRol(rol: String) = prefs.edit { putString("rol", rol) }
    fun getRol(): String? = prefs.getString("rol", null)

    fun clearSession() = prefs.edit { clear() }

    companion object {
        const val PREFS_NAME = "residencial_session"
    }
}
