package com.example.conjuntoresidencial.ui.comun

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

/**
 * Mensaje breve abajo (Snackbar). Va en la capa de avisos de MainActivity, encima del contenido
 * difuminado, de la barra inferior y del botón flotante; dentro de la pantalla quedaba tapado
 * por ellos y borroso.
 */
fun Fragment.aviso(texto: CharSequence, duracion: Int = Snackbar.LENGTH_LONG): Snackbar =
    controlesFlotantes?.crearAviso(texto, duracion) ?: Snackbar.make(requireView(), texto, duracion)

fun Fragment.aviso(@StringRes texto: Int, duracion: Int = Snackbar.LENGTH_LONG): Snackbar =
    aviso(getText(texto), duracion)
