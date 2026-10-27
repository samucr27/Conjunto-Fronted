package com.example.conjuntoresidencial.ui.admin

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.DialogApartamentoBinding
import com.example.conjuntoresidencial.model.Apartamento
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Crear ([actual] null, con la torre de la lista ya puesta) o editar un apartamento (P4 bloque 8).
 * Al crearlo se pide la contraseña; al editarlo no (tiene su propio botón en el detalle).
 */
fun Fragment.pedirApartamento(actual: Apartamento?, torre: String? = null, alGuardar: (String, String, String?) -> Unit) {
    val vista = DialogApartamentoBinding.inflate(layoutInflater)
    vista.etTorre.setText(actual?.torre ?: torre)
    vista.etApto.setText(actual?.apto)
    vista.tilContrasenaApto.visibility = if (actual == null) android.view.View.VISIBLE else android.view.View.GONE
    val dialogo = MaterialAlertDialogBuilder(requireContext())
        .setTitle(if (actual == null) R.string.apartamento_agregar else R.string.apartamento_editar)
        .setView(vista.root)
        .setPositiveButton(R.string.btn_guardar, null)
        .setNegativeButton(R.string.btn_cancelar, null)
        .show()
    dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
        val t = vista.etTorre.text?.toString()?.trim().orEmpty()
        val a = vista.etApto.text?.toString()?.trim().orEmpty()
        val clave = vista.etContrasenaApto.text?.toString().orEmpty()
        vista.tilTorre.error = if (t.isBlank()) getString(R.string.login_falta_torre) else null
        vista.tilApto.error = if (a.isBlank()) getString(R.string.login_falta_apartamento) else null
        vista.tilContrasenaApto.error = if (actual == null && clave.length < 6) getString(R.string.error_contrasena_corta) else null
        if (vista.tilTorre.error != null || vista.tilApto.error != null || vista.tilContrasenaApto.error != null) {
            return@setOnClickListener
        }
        dialogo.dismiss()
        alGuardar(t, a, clave.takeIf { actual == null })
    }
}
