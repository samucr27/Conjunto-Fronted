package com.example.conjuntoresidencial.util

import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView

/** Valores que acepta el backend (ver Catalogo.java en Conjunto-Backend). */
object Opciones {
    val TIPOS_HABITANTE = listOf("PROPIETARIO", "ARRENDATARIO")
    val TIPOS_VEHICULO = listOf("Carro", "Moto", "Bicicleta", "Patineta", "Patineta eléctrica", "Otro")
    val TIPOS_CON_PLACA = setOf("Carro", "Moto")

    /** Si no se pudieron cargar los cupos del backend: los mismos valores iniciales. */
    val CUPOS_POR_DEFECTO = listOf(1, 1, 2, 2, 1, 1).zip(TIPOS_VEHICULO) { maximo, tipo ->
        com.example.conjuntoresidencial.model.CupoVehiculoDTO(tipo, maximo, tipo in TIPOS_CON_PLACA)
    }
}

/** Convierte un AutoCompleteTextView (ExposedDropdownMenu) en una lista cerrada con la primera opción elegida. */
fun AutoCompleteTextView.comoLista(opciones: List<String>) {
    setAdapter(ArrayAdapter(context, android.R.layout.simple_list_item_1, opciones))
    setText(opciones.first(), false)
}
