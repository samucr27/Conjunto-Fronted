package com.example.conjuntoresidencial.util

import android.content.Context
import androidx.annotation.MenuRes
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.ConfigApp
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.EncabezadoGrandeBinding
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.ponerInfo

/**
 * P8 bloque 2: lo que se pide la primera vez que se abre la app, antes del login: la bienvenida,
 * el conjunto (menos en la plataforma) y, en la de residentes, si se entra como propietario o
 * arrendatario. Se guarda aparte de la sesión: cerrarla no lo borra. El conjunto está en
 * [ConjuntoElegido].
 */
object PrimerosPasos {

    const val PROPIETARIO = "PROPIETARIO"
    const val ARRENDATARIO = "ARRENDATARIO"

    private const val ARCHIVO = "residencial_primeros_pasos"

    private fun prefs(context: Context) = context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    fun bienvenidaVista(context: Context) = prefs(context).getBoolean("bienvenida", false)

    fun marcarBienvenida(context: Context) = prefs(context).edit { putBoolean("bienvenida", true) }

    /** PROPIETARIO, ARRENDATARIO o null si aún no se eligió. */
    fun rol(context: Context): String? = prefs(context).getString("rol", null)

    fun guardarRol(context: Context, rol: String) = prefs(context).edit { putString("rol", rol) }

    /** La pantalla que falta antes del login, o null si ya está todo. */
    fun pendiente(context: Context): Int? = when {
        !bienvenidaVista(context) -> R.id.bienvenidaFragment
        ConfigApp.usaConjunto && ConjuntoElegido.id(context) == null -> R.id.elegirConjuntoFragment
        ConfigApp.pideRol && rol(context) == null -> R.id.elegirRolFragment
        else -> null
    }
}

/**
 * En el login: si falta algún paso, abre esa pantalla en lugar del login (sin dejar el login
 * atrás, así "atrás" en la bienvenida cierra la app). Devuelve true si se fue a otra pantalla.
 */
fun Fragment.irAPasoPendiente(): Boolean {
    val destino = PrimerosPasos.pendiente(requireContext()) ?: return false
    findNavController().navigate(destino, null, NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build())
    return true
}

/**
 * Al terminar un paso: abre el que sigue o, si ya no falta ninguno, el login, que queda solo en
 * la pila (lo de antes no se vuelve a ver con "atrás").
 */
fun Fragment.seguirPrimerosPasos() {
    val destino = PrimerosPasos.pendiente(requireContext())
    if (destino != null) {
        findNavController().navigate(destino)
    } else {
        findNavController().navigate(ConfigApp.destinoLogin, null,
            NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build())
    }
}

/**
 * El encabezado del login: [titulo] con el rol ("Acceso propietario") y, debajo, el conjunto
 * elegido. El ⋮ ([menu]) lleva a cambiar el conjunto o el rol; sin [menu] no hay ⋮ (plataforma).
 */
fun Fragment.encabezadoDeLogin(encabezado: EncabezadoGrandeBinding, titulo: CharSequence, @MenuRes menu: Int?) {
    configurarEncabezado(encabezado, titulo,
        atras = findNavController().previousBackStackEntry != null,
        menu = menu) { id ->
        when (id) {
            R.id.accionCambiarConjunto -> { findNavController().navigate(R.id.elegirConjuntoFragment); true }
            R.id.accionCambiarRol -> { findNavController().navigate(R.id.elegirRolFragment); true }
            else -> false
        }
    }
    val c = ConjuntoElegido.actual(requireContext())
    encabezado.ponerInfo(c?.let { if (it.municipio.isBlank()) it.nombre else getString(R.string.conjunto_actual, it.nombre, it.municipio) })
}
