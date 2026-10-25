package com.example.conjuntoresidencial.ui.admin

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.DialogApartamentoBinding
import com.example.conjuntoresidencial.databinding.DialogPersonalBinding
import com.example.conjuntoresidencial.model.Apartamento
import com.example.conjuntoresidencial.model.PersonalObraDTO
import com.example.conjuntoresidencial.ui.comun.confirmar
import com.example.conjuntoresidencial.ui.comun.ejecutarAccion
import com.example.conjuntoresidencial.ui.comun.elegirOpcion
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

/** Tocar un trabajador (Personal o Control de obras): editarlo o quitarlo; luego [recargar]. */
fun Fragment.opcionesPersonal(p: PersonalObraDTO, recargar: () -> Unit) {
    elegirOpcion(p.nombreTrabajador, listOf(
        R.string.editar to { editarPersonal(p, recargar) },
        R.string.personal_eliminar to {
            confirmar(getString(R.string.personal_eliminar_titulo, p.nombreTrabajador), null, R.string.residente_eliminar_si) {
                ejecutarAccion(getString(R.string.personal_eliminado), { RetrofitClient.instance.deletePersonalObra(p.id!!) }, recargar)
            }
        }
    ))
}

private fun Fragment.editarPersonal(p: PersonalObraDTO, recargar: () -> Unit) {
    val vista = DialogPersonalBinding.inflate(layoutInflater)
    vista.etNombrePersonal.setText(p.nombreTrabajador)
    vista.etCedulaPersonal.setText(p.cedula)
    vista.switchArl.isChecked = p.arlEstado
    val dialogo = MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.personal_editar)
        .setView(vista.root)
        .setPositiveButton(R.string.btn_guardar, null)
        .setNegativeButton(R.string.btn_cancelar, null)
        .show()
    dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
        val nombre = vista.etNombrePersonal.text?.toString()?.trim().orEmpty()
        val cedula = vista.etCedulaPersonal.text?.toString()?.trim().orEmpty()
        vista.tilNombrePersonal.error = if (nombre.isBlank()) getString(R.string.personal_nombre_obligatorio) else null
        vista.tilCedulaPersonal.error = if (cedula.isBlank()) getString(R.string.personal_cedula_obligatoria) else null
        if (nombre.isBlank() || cedula.isBlank()) return@setOnClickListener
        dialogo.dismiss()
        ejecutarAccion(getString(R.string.personal_editado), {
            RetrofitClient.instance.updatePersonalObra(p.id!!, p.copy(nombreTrabajador = nombre, cedula = cedula,
                arlEstado = vista.switchArl.isChecked))
        }, recargar)
    }
}
