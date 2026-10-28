package com.example.conjuntoresidencial.ui.feed

import androidx.annotation.StringRes
import com.example.conjuntoresidencial.R

/** Códigos que acepta el backend y su texto en pantalla (P6: categoría y audiencia de una publicación). */
object EtiquetasPublicacion {
    val CATEGORIAS: List<Pair<String, Int>> = listOf(
        "GENERAL" to R.string.categoria_general,
        "URGENTE" to R.string.categoria_urgente,
        "MANTENIMIENTO" to R.string.categoria_mantenimiento,
        "EVENTO" to R.string.categoria_evento,
        "FINANZAS" to R.string.categoria_finanzas
    )
    val AUDIENCIAS: List<Pair<String, Int>> = listOf(
        "TODOS" to R.string.audiencia_todos,
        "PROPIETARIOS" to R.string.audiencia_propietarios,
        "ARRENDATARIOS" to R.string.audiencia_arrendatarios
    )

    @StringRes
    fun categoria(codigo: String): Int = CATEGORIAS.firstOrNull { it.first == codigo }?.second ?: R.string.categoria_general

    @StringRes
    fun audiencia(codigo: String): Int = AUDIENCIAS.firstOrNull { it.first == codigo }?.second ?: R.string.audiencia_todos
}
