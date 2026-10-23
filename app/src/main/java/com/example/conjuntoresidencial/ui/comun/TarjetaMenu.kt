package com.example.conjuntoresidencial.ui.comun

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.conjuntoresidencial.databinding.ItemTarjetaMenuBinding

/** Configura una tarjeta de panel (layout/item_tarjeta_menu.xml). */
fun ItemTarjetaMenuBinding.configurar(
    @DrawableRes icono: Int,
    @StringRes titulo: Int,
    @StringRes subtitulo: Int,
    alTocar: () -> Unit
) {
    this.icono.setImageResource(icono)
    this.titulo.setText(titulo)
    this.subtitulo.setText(subtitulo)
    // Nombre propio para TalkBack ("Directorio, Apartamentos"): con letra grande la tarjeta puede
    // quedar cortada bajo la barra inferior y sus textos, fuera de la pantalla, no la nombrarían
    root.contentDescription = "${this.titulo.text}, ${this.subtitulo.text}"
    root.setOnClickListener { alTocar() }
}
