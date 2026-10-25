package com.example.conjuntoresidencial.util

import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.push.AvisosCelular
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Pide confirmación, borra la sesión y vuelve al selector de acceso vaciando el
 * historial de pantallas: así "atrás" no regresa a un panel sin sesión. El celular deja de
 * recibir los avisos de esa cuenta.
 */
fun Fragment.confirmarCerrarSesion() {
    MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.cerrar_sesion)
        .setMessage(R.string.cerrar_sesion_pregunta)
        .setPositiveButton(R.string.cerrar_sesion_confirmar) { _, _ ->
            AvisosCelular.darDeBaja(requireContext())
            SessionManager(requireContext()).clearSession()
            findNavController().navigate(
                R.id.loginSelectorFragment,
                null,
                NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
            )
        }
        .setNegativeButton(R.string.btn_cancelar, null)
        .show()
}
