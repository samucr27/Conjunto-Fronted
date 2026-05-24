package com.example.conjuntoresidencial.util

import android.content.Context
import androidx.core.content.edit

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("residencial_session", Context.MODE_PRIVATE)

    fun saveApartamentoId(id: Long) = prefs.edit { putLong("apartamento_id", id) }
    fun getApartamentoId(): Long = prefs.getLong("apartamento_id", -1L)

    fun saveRol(rol: String) = prefs.edit { putString("rol", rol) }
    fun getRol(): String? = prefs.getString("rol", null)

    fun clearSession() = prefs.edit { clear() }
}