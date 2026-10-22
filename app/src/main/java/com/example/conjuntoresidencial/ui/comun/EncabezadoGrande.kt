package com.example.conjuntoresidencial.ui.comun

import android.view.View
import androidx.annotation.MenuRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.EncabezadoGrandeBinding
import com.google.android.material.appbar.AppBarLayout
import kotlin.math.abs

/** Parte de la pantalla que ocupa el encabezado desplegado (One UI: la información arriba). */
private const val PROPORCION_ENCABEZADO = 0.4f

/**
 * Configura el encabezado estilo One UI (layout/encabezado_grande.xml).
 *
 * @param descripcion texto fijo bajo el resumen (qué se hace en la pantalla)
 * @param atras muestra el botón redondo para volver (false en los paneles)
 * @param menu menú del botón "⋮"; [alElegir] recibe el id del elemento tocado
 */
fun Fragment.configurarEncabezado(
    encabezado: EncabezadoGrandeBinding,
    titulo: CharSequence,
    @StringRes descripcion: Int? = null,
    atras: Boolean = true,
    @MenuRes menu: Int? = null,
    alElegir: (Int) -> Boolean = { false }
) {
    // Alto desplegado: 40 % de la pantalla (no la mitad: con letra grande dejaría poco para la lista)
    encabezado.colapsable.layoutParams.height =
        (resources.displayMetrics.heightPixels * PROPORCION_ENCABEZADO).toInt()

    encabezado.ponerTitulo(titulo)
    encabezado.tvDescripcion.text = descripcion?.let { getString(it) }
    encabezado.tvDescripcion.visibility = if (descripcion == null) View.GONE else View.VISIBLE

    encabezado.btnVolver.visibility = if (atras) View.VISIBLE else View.GONE
    encabezado.btnVolver.setOnClickListener { findNavController().navigateUp() }

    encabezado.btnMas.visibility = if (menu == null) View.GONE else View.VISIBLE
    if (menu != null) {
        encabezado.btnMas.setOnClickListener { boton ->
            PopupMenu(requireContext(), boton).apply {
                inflate(menu)
                setOnMenuItemClickListener { alElegir(it.itemId) }
            }.show()
        }
    }

    // Al plegar, la información se desvanece y aparece el título pequeño de la barra
    encabezado.root.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { barra, desplazamiento ->
        val recorrido = barra.totalScrollRange.takeIf { it > 0 } ?: return@OnOffsetChangedListener
        val plegado = abs(desplazamiento).toFloat() / recorrido
        encabezado.zonaInfo.alpha = (1f - plegado * 1.6f).coerceIn(0f, 1f)
        encabezado.tvTituloCompacto.alpha = ((plegado - 0.6f) / 0.4f).coerceIn(0f, 1f)
    })
}

fun Fragment.configurarEncabezado(
    encabezado: EncabezadoGrandeBinding,
    @StringRes titulo: Int,
    @StringRes descripcion: Int? = null,
    atras: Boolean = true,
    @MenuRes menu: Int? = null,
    alElegir: (Int) -> Boolean = { false }
) = configurarEncabezado(encabezado, getString(titulo), descripcion, atras, menu, alElegir)

fun EncabezadoGrandeBinding.ponerTitulo(titulo: CharSequence) {
    tvTituloGrande.text = titulo
    tvTituloCompacto.text = titulo
}

/** Resumen bajo el título ("4 reservas · 2 pendientes"); null lo oculta (por ejemplo, si hubo error). */
fun EncabezadoGrandeBinding.ponerInfo(info: CharSequence?) {
    tvInfo.text = info
    tvInfo.visibility = if (info.isNullOrBlank()) View.GONE else View.VISIBLE
}

/**
 * Arma el resumen: el total y, a continuación, los detalles que no estén en cero.
 * Ej.: resumen(R.plurals.resumen_reservas, 4, R.plurals.resumen_pendientes to 2) = "4 reservas · 2 pendientes"
 */
fun Fragment.resumen(@PluralsRes total: Int, cantidad: Int, vararg detalles: Pair<Int, Int>): String =
    (listOf(total to cantidad) + detalles.filter { it.second > 0 })
        .joinToString(" · ") { (plural, n) -> resources.getQuantityString(plural, n, n) }

/** Menú "⋮" de las listas (menu/menu_lista.xml): "Actualizar" recarga con el indicador de deslizar. */
fun accionesDeLista(swipe: SwipeRefreshLayout, recargar: () -> Unit): (Int) -> Boolean = { id ->
    (id == R.id.accion_actualizar).also {
        if (it) {
            swipe.isRefreshing = true
            recargar()
        }
    }
}
