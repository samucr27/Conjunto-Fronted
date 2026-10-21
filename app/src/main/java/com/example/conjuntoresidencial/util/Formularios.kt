package com.example.conjuntoresidencial.util

import androidx.core.widget.doAfterTextChanged
import com.google.android.material.textfield.TextInputLayout

/**
 * Validación de campos obligatorios con el error en el propio campo (TalkBack lo anuncia
 * junto al campo, cosa que no pasa con un Snackbar).
 *
 * Uso: llamar [limpiarErrorAlEscribir] una vez al crear la vista y, al enviar,
 * `val ok = listOf(til1.exigirTexto(...), til2.exigirTexto(...)).all { it }`
 * (con una lista se marcan todos los campos vacíos a la vez, no solo el primero).
 */
fun TextInputLayout.exigirTexto(mensaje: String): Boolean {
    val vacio = editText?.text.isNullOrBlank()
    error = if (vacio) mensaje else null
    return !vacio
}

/** Quita el error del campo en cuanto el usuario escribe algo. */
fun TextInputLayout.limpiarErrorAlEscribir() {
    editText?.doAfterTextChanged { if (!it.isNullOrEmpty()) error = null }
}
