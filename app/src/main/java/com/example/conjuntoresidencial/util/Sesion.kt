package com.example.conjuntoresidencial.util

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.ConfigApp
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.push.AvisosCelular
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Lo que se espera al servidor para cerrar la sesión; sin conexión no se deja al usuario atrapado. */
private const val ESPERA_LOGOUT_MS = 4_000L

/**
 * Pide confirmación, avisa al servidor (para que el token deje de valer), borra la sesión y
 * vuelve al acceso de esta app vaciando el historial de pantallas: así "atrás" no regresa a un
 * panel sin sesión. El celular deja de recibir los avisos de esa cuenta.
 */
fun Fragment.confirmarCerrarSesion() {
    MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.cerrar_sesion)
        .setMessage(R.string.cerrar_sesion_pregunta)
        .setPositiveButton(R.string.cerrar_sesion_confirmar) { _, _ ->
            val contexto = requireContext().applicationContext
            AvisosCelular.darDeBaja(requireContext())
            viewLifecycleOwner.lifecycleScope.launch {
                // Antes de borrar la sesión: la petición lleva el token que se invalida
                withTimeoutOrNull(ESPERA_LOGOUT_MS) {
                    try { RetrofitClient.comun.cerrarSesion() } catch (_: Exception) { }
                }
                SessionManager(contexto).clearSession()
                findNavController().navigate(
                    ConfigApp.destinoLogin,
                    null,
                    NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
                )
            }
        }
        .setNegativeButton(R.string.btn_cancelar, null)
        .show()
}
