package com.example.conjuntoresidencial.ui.comun

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.annotation.StringRes
import androidx.core.content.getSystemService
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.conjuntoresidencial.databinding.BarraBusquedaBinding
import com.google.android.material.chip.Chip
import java.text.Normalizer
import java.util.Locale

/**
 * Barra de búsqueda con filtros (P5) para las listas largas: el texto y un chip filtran lo ya
 * cargado, sin pedir nada al backend. "1 101", "torre 2" o "juan": todas las palabras deben
 * aparecer (sin importar tildes ni mayúsculas). Lo escrito y el filtro elegido viven en los
 * argumentos del fragment, así sobreviven a girar la pantalla y a volver de un detalle.
 *
 * [filtros]: (etiqueta, valor); el primero suele ser "Todos" con valor null. Sin filtros, solo el texto.
 */
class BarraBusqueda(
    private val fragment: Fragment,
    private val vista: BarraBusquedaBinding,
    @StringRes pista: Int,
    filtros: List<Pair<Int, String?>> = emptyList(),
    private val alCambiar: () -> Unit
) {
    val texto: String get() = fragment.arguments?.getString(CLAVE_TEXTO).orEmpty()
    val filtro: String? get() = fragment.arguments?.getString(CLAVE_FILTRO)

    /** Hay algo escrito: si no queda nada, la lista dice "ninguno coincide" en vez de "no hay". */
    val activa: Boolean get() = texto.isNotBlank()

    init {
        vista.tilBuscar.hint = fragment.getString(pista)
        vista.etBuscar.setText(texto)
        vista.etBuscar.doAfterTextChanged {
            val nuevo = it?.toString().orEmpty()
            if (nuevo == texto) return@doAfterTextChanged
            argumentos().putString(CLAVE_TEXTO, nuevo)
            alCambiar()
        }
        // "Buscar" en el teclado solo lo cierra: la lista ya se filtró al escribir
        vista.etBuscar.setOnEditorActionListener { v, accion, _ ->
            (accion == EditorInfo.IME_ACTION_SEARCH).also {
                if (it) v.context.getSystemService<InputMethodManager>()?.hideSoftInputFromWindow(v.windowToken, 0)
            }
        }
        vista.scrollFiltros.visibility = if (filtros.isEmpty()) View.GONE else View.VISIBLE
        vista.chipsFiltros.removeAllViews()
        filtros.forEach { (etiqueta, valor) ->
            vista.chipsFiltros.addView(Chip(fragment.requireContext(), null, com.google.android.material.R.attr.chipStyle).apply {
                setText(etiqueta)
                isCheckable = true
                isChecked = valor == filtro
                setOnClickListener {
                    if (valor == filtro) { isChecked = true; return@setOnClickListener }
                    argumentos().putString(CLAVE_FILTRO, valor)
                    alCambiar()
                }
            })
        }
    }

    /** Todas las palabras escritas están en alguno de [campos]. */
    fun coincide(vararg campos: String?): Boolean {
        val palabras = normal(texto).split(Regex("\\s+")).filter { it.isNotBlank() }
        if (palabras.isEmpty()) return true
        val en = normal(campos.filterNotNull().joinToString(" "))
        return palabras.all { en.contains(it) }
    }

    /** La lista no es hija directa del swipe: solo se actualiza al jalar desde arriba de la lista. */
    fun vincular(swipe: SwipeRefreshLayout, lista: RecyclerView) {
        swipe.setOnChildScrollUpCallback { _, _ -> lista.canScrollVertically(-1) }
    }

    private fun argumentos(): Bundle = fragment.arguments ?: Bundle().also { fragment.arguments = it }

    companion object {
        private const val CLAVE_TEXTO = "busqueda_texto"
        private const val CLAVE_FILTRO = "busqueda_filtro"

        fun normal(s: String): String =
            Normalizer.normalize(s.lowercase(Locale.ROOT), Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")
    }
}

/** Vuelve a entregar el último valor (para filtrar otra vez lo ya cargado sin pedirlo al backend). */
fun <T> LiveData<T>.repetir() {
    (this as? MutableLiveData<T>)?.let { it.value = it.value }
}
