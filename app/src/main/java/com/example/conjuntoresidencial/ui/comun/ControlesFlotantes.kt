package com.example.conjuntoresidencial.ui.comun

import androidx.annotation.MenuRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData

/**
 * Botones redondos "volver" y "⋮" y franja superior difuminada, que flotan sobre todas las
 * pantallas (activity_main.xml). Los implementa MainActivity; cada pantalla los configura con
 * configurarEncabezado. Al cambiar de pantalla se ocultan hasta que la nueva los pida.
 */
interface ControlesFlotantes {

    /** Alto de la barra de estado: el contenido llega hasta arriba y debe dejar ese espacio. */
    val margenSuperior: Int

    /**
     * Espacio que tapa la barra inferior translúcida (0 si no se muestra). El contenido pasa por
     * debajo de ella, así que las listas lo dejan libre al final para que el último elemento se lea.
     */
    val espacioInferior: LiveData<Int>

    fun mostrarControles(atras: Boolean, alVolver: () -> Unit, @MenuRes menu: Int?, alElegir: (Int) -> Boolean)

    /**
     * Cuánto ha salido el encabezado grande de la pantalla: 0 = desplegado, 1 = fuera.
     * La franja difuminada aparece al final, cuando el contenido empieza a pasar por debajo.
     */
    fun ponerPlegado(fraccion: Float)
}

val Fragment.controlesFlotantes: ControlesFlotantes?
    get() = activity as? ControlesFlotantes
