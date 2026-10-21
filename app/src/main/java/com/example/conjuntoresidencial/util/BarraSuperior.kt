package com.example.conjuntoresidencial.util

import androidx.annotation.MenuRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.R
import com.google.android.material.appbar.MaterialToolbar

/**
 * Configura la barra superior común (layout/barra_superior.xml).
 *
 * @param atras muestra la flecha para volver (false en pantallas raíz, como los paneles)
 * @param menu menú de acciones opcional; [alElegir] recibe el id del elemento tocado
 */
fun Fragment.configurarBarra(
    toolbar: MaterialToolbar,
    @StringRes titulo: Int,
    @StringRes subtitulo: Int? = null,
    atras: Boolean = true,
    @MenuRes menu: Int? = null,
    alElegir: (Int) -> Boolean = { false }
) {
    toolbar.title = getString(titulo)
    toolbar.subtitle = subtitulo?.let { getString(it) }
    if (atras) {
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        toolbar.setNavigationContentDescription(R.string.volver)
        toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
    } else {
        toolbar.navigationIcon = null
    }
    toolbar.menu.clear()
    if (menu != null) {
        toolbar.inflateMenu(menu)
        toolbar.setOnMenuItemClickListener { alElegir(it.itemId) }
    }
}
