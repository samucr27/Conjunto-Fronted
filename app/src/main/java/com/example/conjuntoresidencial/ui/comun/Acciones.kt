package com.example.conjuntoresidencial.ui.comun

import android.text.InputType
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import retrofit2.Response

/*
 * Lo común de editar y eliminar (P4 bloque 8): elegir qué hacer con un elemento, confirmar lo
 * que no se puede deshacer y ejecutar la petición avisando cómo salió.
 */

/** Opciones al tocar un elemento ("Editar", "Eliminar"...); cada una con lo que hace. */
fun Fragment.elegirOpcion(titulo: String, opciones: List<Pair<Int, () -> Unit>>) {
    MaterialAlertDialogBuilder(requireContext())
        .setTitle(titulo)
        .setItems(opciones.map { getString(it.first) }.toTypedArray()) { _, i -> opciones[i].second() }
        .setNegativeButton(R.string.btn_cancelar, null)
        .show()
}

/**
 * Pide un texto (valor, descripción, contraseña...). [validar] devuelve el error o null; el
 * diálogo no se cierra mientras haya error.
 */
fun Fragment.pedirTexto(
    titulo: String, @StringRes hint: Int, inicial: String?, tipoEntrada: Int,
    validar: (String) -> String?, alAceptar: (String) -> Unit
) {
    val campo = TextInputEditText(requireContext()).apply {
        inputType = tipoEntrada
        setText(inicial)
    }
    val til = TextInputLayout(requireContext()).apply {
        this.hint = getString(hint)
        if (tipoEntrada and InputType.TYPE_TEXT_VARIATION_PASSWORD != 0) endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
        addView(campo)
        val margen = (24 * resources.displayMetrics.density).toInt()
        setPadding(margen, margen / 2, margen, 0)
    }
    val dialogo = MaterialAlertDialogBuilder(requireContext())
        .setTitle(titulo)
        .setView(til)
        .setPositiveButton(R.string.btn_guardar, null)
        .setNegativeButton(R.string.btn_cancelar, null)
        .show()
    dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
        val texto = campo.text?.toString()?.trim().orEmpty()
        til.error = validar(texto)
        if (til.error == null) {
            dialogo.dismiss()
            alAceptar(texto)
        }
    }
}

/** Pregunta antes de algo que no se puede deshacer (eliminar, cancelar). */
fun Fragment.confirmar(titulo: String, mensaje: String?, @StringRes boton: Int, accion: () -> Unit) {
    MaterialAlertDialogBuilder(requireContext())
        .setTitle(titulo)
        .setMessage(mensaje)
        .setPositiveButton(boton) { _, _ -> accion() }
        .setNegativeButton(R.string.btn_cancelar, null)
        .show()
}

/**
 * Ejecuta [peticion]; si sale bien avisa [exito] y llama [despues] (normalmente recargar).
 * Si falla, el mensaje del backend (p. ej. "Primero quita sus residentes...").
 */
fun Fragment.ejecutarAccion(exito: String, peticion: suspend () -> Response<*>, despues: () -> Unit = {}) {
    viewLifecycleOwner.lifecycleScope.launch {
        val mensaje = try {
            val r = peticion()
            if (r.isSuccessful) exito.also { despues() }
            else RetrofitClient.mensajeDeError(r, getString(R.string.residente_error_accion))
        } catch (e: Exception) {
            RetrofitClient.mensajeDeExcepcion(e)
        }
        if (view != null) aviso(mensaje, Snackbar.LENGTH_LONG).show()
    }
}
