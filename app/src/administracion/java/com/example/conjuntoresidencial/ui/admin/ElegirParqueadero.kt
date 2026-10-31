package com.example.conjuntoresidencial.ui.admin

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.api.instance
import com.example.conjuntoresidencial.ui.comun.aviso
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * Elegir un parqueadero libre de la categoría [tipo] (P5): al aprobar un vehículo o al
 * asignárselo después. Si no hay libres, sigue sin parqueadero ([alElegir] con null) y lo avisa;
 * la última opción también es "Sin parqueadero".
 */
fun Fragment.elegirParqueadero(tipo: String, titulo: String, alElegir: (Long?) -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        val libres = try {
            RetrofitClient.instance.getParqueaderos(tipo, "LIBRE").body().orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
        if (view == null) return@launch
        if (libres.isEmpty()) {
            aviso(getString(R.string.parqueadero_sin_libres, tipo.lowercase())).show()
            alElegir(null)
            return@launch
        }
        val opciones = libres.map { it.numero } + getString(R.string.parqueadero_ninguno)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(titulo)
            .setItems(opciones.toTypedArray()) { _, i -> alElegir(libres.getOrNull(i)?.id) }
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
    }
}
