package com.example.conjuntoresidencial.ui.obras

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.DialogPersonalBinding
import com.example.conjuntoresidencial.model.PersonalObraDTO
import com.example.conjuntoresidencial.ui.comun.confirmar
import com.example.conjuntoresidencial.ui.comun.ejecutarAccion
import com.example.conjuntoresidencial.ui.comun.elegirOpcion
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Tocar un trabajador (Personal o Control de obras): editarlo o quitarlo; luego [recargar]. */
fun Fragment.opcionesPersonal(p: PersonalObraDTO, recargar: () -> Unit) {
    elegirOpcion(p.nombreTrabajador, listOf(
        R.string.editar to { editarPersonal(p, recargar) },
        R.string.personal_eliminar to {
            confirmar(getString(R.string.personal_eliminar_titulo, p.nombreTrabajador), null, R.string.residente_eliminar_si) {
                ejecutarAccion(getString(R.string.personal_eliminado), { RetrofitClient.comun.deletePersonalObra(p.id!!) }, recargar)
            }
        }
    ))
}

fun Fragment.editarPersonal(p: PersonalObraDTO, recargar: () -> Unit) {
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
            RetrofitClient.comun.updatePersonalObra(p.id!!, p.copy(nombreTrabajador = nombre, cedula = cedula,
                arlEstado = vista.switchArl.isChecked))
        }, recargar)
    }
}
