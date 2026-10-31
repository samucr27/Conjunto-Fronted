package com.example.conjuntoresidencial.ui.admin

import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.DialogApartamentoBinding
import com.example.conjuntoresidencial.model.Apartamento
import com.example.conjuntoresidencial.model.OcupacionYBanderas
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch

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

/**
 * Cambiar cómo está ocupado el apartamento y qué le deja ver o hacer al arrendatario (P6):
 * la cartera, subir comprobantes y pagar, y las obras. Solo envía lo que cambió de verdad.
 */
fun Fragment.pedirOcupacion(actual: Apartamento, alGuardar: (OcupacionYBanderas) -> Unit) {
    val contexto = requireContext()
    val dp = contexto.resources.displayMetrics.density
    val opciones = listOf(
        "PROPIETARIO" to R.string.ocupacion_propietario,
        "ARRENDATARIO" to R.string.ocupacion_arrendado,
        "DESOCUPADO" to R.string.ocupacion_desocupado
    )
    val grupo = RadioGroup(contexto)
    val botones = opciones.map { (tipo, texto) ->
        RadioButton(contexto).apply {
            id = View.generateViewId()
            setText(texto)
            isChecked = (actual.tipoOcupacion ?: "PROPIETARIO") == tipo
            grupo.addView(this)
        }
    }
    fun interruptor(texto: Int, valor: Boolean?) = MaterialSwitch(contexto).apply {
        setText(texto)
        isChecked = valor == true
    }
    val verCartera = interruptor(R.string.bandera_cartera, actual.arrendatarioVeCartera)
    val pagar = interruptor(R.string.bandera_pagar, actual.arrendatarioPuedePagar)
    val verObras = interruptor(R.string.bandera_obras, actual.arrendatarioVeObras)
    val contenido = LinearLayout(contexto).apply {
        orientation = LinearLayout.VERTICAL
        val lado = (24 * dp).toInt()
        setPadding(lado, (8 * dp).toInt(), lado, 0)
        addView(grupo)
        addView(TextView(contexto).apply {
            setText(R.string.ocupacion_banderas_titulo)
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleSmall)
            setPadding(0, (16 * dp).toInt(), 0, (4 * dp).toInt())
        })
        addView(verCartera); addView(pagar); addView(verObras)
    }
    MaterialAlertDialogBuilder(contexto)
        .setTitle(R.string.ocupacion_titulo)
        .setView(contenido)
        .setPositiveButton(R.string.btn_guardar) { _, _ ->
            val tipo = opciones[botones.indexOfFirst { it.id == grupo.checkedRadioButtonId }.coerceAtLeast(0)].first
            alGuardar(OcupacionYBanderas(tipo, verCartera.isChecked, pagar.isChecked, verObras.isChecked))
        }
        .setNegativeButton(R.string.btn_cancelar, null)
        .show()
}
