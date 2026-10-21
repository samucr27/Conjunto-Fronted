package com.example.conjuntoresidencial.util

import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView

/** Valores que acepta el backend (ver Catalogo.java en Conjunto-Backend). */
object Opciones {
    val TIPOS_HABITANTE = listOf("PROPIETARIO", "ARRENDATARIO")
    val TIPOS_VEHICULO = listOf("Carro", "Moto")
}

/** Convierte un AutoCompleteTextView (ExposedDropdownMenu) en una lista cerrada con la primera opción elegida. */
fun AutoCompleteTextView.comoLista(opciones: List<String>) {
    setAdapter(ArrayAdapter(context, android.R.layout.simple_list_item_1, opciones))
    setText(opciones.first(), false)
}
