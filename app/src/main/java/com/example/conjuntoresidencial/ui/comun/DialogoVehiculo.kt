package com.example.conjuntoresidencial.ui.comun

import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.DialogAgregarVehiculoBinding
import com.example.conjuntoresidencial.model.CupoVehiculoDTO
import com.example.conjuntoresidencial.model.VehiculoDTO
import com.example.conjuntoresidencial.util.Opciones
import com.example.conjuntoresidencial.util.limpiarErrorAlEscribir
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Lo que se escribió en el diálogo; placa null si el tipo no lleva. */
data class VehiculoNuevo(val tipo: String, val placa: String?, val marca: String, val color: String)

/**
 * Diálogo para registrar un vehículo (residente en Mis vehículos o administrador en el detalle
 * del apartamento). Primero el tipo, con lo que le queda de cupo al apartamento; la placa solo
 * se pide para carros y motos.
 *
 * @param cupos los del backend (GET /api/vehiculos/cupos); si no cargaron, los de por defecto
 * @param vehiculos los que ya tiene el apartamento, para calcular lo que le queda
 * @param limitar el residente no puede pasar del cupo; el administrador sí
 */
fun Fragment.pedirVehiculo(
    cupos: List<CupoVehiculoDTO>?,
    vehiculos: List<VehiculoDTO>,
    limitar: Boolean,
    alGuardar: (VehiculoNuevo) -> Unit
) {
    val lista = cupos?.takeIf { it.isNotEmpty() } ?: Opciones.CUPOS_POR_DEFECTO
    val vista = DialogAgregarVehiculoBinding.inflate(layoutInflater)
    vista.etTipo.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, lista.map { it.tipo }))
    vista.tilPlaca.limpiarErrorAlEscribir()

    // Lo que ocupa cupo: pendientes y aprobados (los de antes de P4 no traen estado: aprobados)
    fun ocupados(tipo: String) = vehiculos.count { it.tipo == tipo && it.estado != ESTADO_RECHAZADO }

    var elegido = lista.first()
    fun elegir(cupo: CupoVehiculoDTO) {
        elegido = cupo
        vista.etTipo.setText(cupo.tipo, false)
        vista.tilPlaca.visibility = if (cupo.llevaPlaca) View.VISIBLE else View.GONE
        vista.tilPlaca.error = null
        val quedan = (cupo.maximo - ocupados(cupo.tipo)).coerceAtLeast(0)
        vista.tilTipo.error = null
        vista.tilTipo.helperText = when {
            !limitar -> getString(R.string.vehiculo_cupo_admin, cupo.maximo)
            cupo.maximo == 0 -> getString(R.string.vehiculo_cupo_no_permitido)
            else -> resources.getQuantityString(R.plurals.vehiculo_cupo_quedan, quedan, quedan, cupo.maximo)
        }
    }
    vista.etTipo.setOnItemClickListener { _, _, posicion, _ -> elegir(lista[posicion]) }
    // Empieza en el primer tipo que todavía tenga cupo
    elegir(lista.firstOrNull { !limitar || ocupados(it.tipo) < it.maximo } ?: lista.first())

    val dialogo = MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.vehiculo_nuevo)
        .setView(vista.root)
        .setPositiveButton(R.string.btn_guardar, null)
        .setNegativeButton(R.string.btn_cancelar, null)
        .show()

    // Se asigna después de show() para que el diálogo no se cierre si falta algo
    dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
        if (limitar && ocupados(elegido.tipo) >= elegido.maximo) {
            vista.tilTipo.error = if (elegido.maximo == 0) getString(R.string.vehiculo_cupo_no_permitido)
                else getString(R.string.vehiculo_cupo_lleno, elegido.maximo)
            return@setOnClickListener
        }
        val placa = vista.etPlaca.text.toString().trim().uppercase()
        if (elegido.llevaPlaca && placa.isBlank()) {
            vista.tilPlaca.error = getString(R.string.vehiculo_placa_obligatoria)
            return@setOnClickListener
        }
        alGuardar(VehiculoNuevo(
            elegido.tipo, placa.takeIf { elegido.llevaPlaca },
            vista.etMarca.text.toString().trim(), vista.etColor.text.toString().trim()
        ))
        dialogo.dismiss()
    }
}

private const val ESTADO_RECHAZADO = "RECHAZADO"
