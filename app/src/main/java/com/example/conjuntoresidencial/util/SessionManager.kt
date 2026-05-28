package com.example.conjuntoresidencial.util

import android.content.Context
import androidx.core.content.edit

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("residencial_session", Context.MODE_PRIVATE)

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
}