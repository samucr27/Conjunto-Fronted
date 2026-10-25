package com.example.conjuntoresidencial.ui.comun

import androidx.annotation.DrawableRes
import androidx.annotation.MenuRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar

/**
 * Botones redondos "volver" y "⋮" y franja superior difuminada, que flotan sobre todas las
 * pantallas (activity_main.xml). Los implementa MainActivity; cada pantalla los configura con
 * configurarEncabezado. Al cambiar de pantalla se ocultan hasta que la nueva los pida.
 */
interface ControlesFlotantes {

    /** Alto de la barra de estado: el contenido llega hasta arriba y debe dejar ese espacio. */
    val margenSuperior: Int

    /**
     * Espacio que las listas dejan libre al final: lo que ocupan abajo la zona difuminada, la
     * barra inferior translúcida y el botón flotante. Así el último elemento se puede subir
     * hasta verse nítido y nada lo tapa.
     */
    val espacioInferior: LiveData<Int>

    fun mostrarControles(atras: Boolean, alVolver: () -> Unit, @MenuRes menu: Int?, alElegir: (Int) -> Boolean)

    /**
     * Acción principal de la pantalla (Registrar zona, Agregar vehículo, Registrar obra): un botón
     * que flota abajo a la derecha, nítido encima del difuminado. Al cambiar de pantalla se oculta.
     * Devuelve el botón para encogerlo al desplazar o desactivarlo mientras se guarda.
     */
    fun mostrarBotonFlotante(@StringRes texto: Int, @DrawableRes icono: Int, alTocar: () -> Unit): ExtendedFloatingActionButton

    /**
     * Ejecuta [accion] cuando termina el logo animado de la carga (enseguida si ya terminó):
     * así lo que se muestre en ese momento (la huella al abrir) no queda detrás del logo.
     */
    fun alTerminarCarga(accion: () -> Unit)

    /**
     * Mensaje breve (Snackbar) en la capa de avisos, justo encima de la barra inferior o del
     * botón flotante si se muestran. Usar con Fragment.aviso (ui/comun/Avisos.kt).
     */
    fun crearAviso(texto: CharSequence, duracion: Int): Snackbar

    /**
     * Cuánto ha salido el encabezado grande de la pantalla: 0 = desplegado, 1 = fuera.
     * La franja difuminada aparece al final, cuando el contenido empieza a pasar por debajo.
     */
    fun ponerPlegado(fraccion: Float)
}

val Fragment.controlesFlotantes: ControlesFlotantes?
    get() = activity as? ControlesFlotantes
