package com.example.conjuntoresidencial.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit

/**
 * Tema elegido en Cuenta: el del sistema, claro u oscuro. Se guarda aparte de la sesión
 * (SessionManager.clearSession no lo borra) y se aplica al iniciar la app (ConjuntoApp).
 */
object Tema {
    private const val PREFS = "ajustes"
    private const val CLAVE = "tema"

    enum class Modo(val nocturno: Int) {
        SISTEMA(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
        CLARO(AppCompatDelegate.MODE_NIGHT_NO),
        OSCURO(AppCompatDelegate.MODE_NIGHT_YES)
    }

    fun actual(context: Context): Modo {
        val guardado = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CLAVE, null)
        return Modo.entries.firstOrNull { it.name == guardado } ?: Modo.SISTEMA
    }

    fun aplicar(context: Context) = AppCompatDelegate.setDefaultNightMode(actual(context).nocturno)

    /** Guarda y aplica; la actividad se recrea con el tema nuevo (la sesión se conserva). */
    fun cambiar(context: Context, modo: Modo) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putString(CLAVE, modo.name) }
        AppCompatDelegate.setDefaultNightMode(modo.nocturno)
    }
}
