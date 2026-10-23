package com.example.conjuntoresidencial.ui.comun

import android.view.View
import android.view.ViewTreeObserver
import android.widget.EditText
import androidx.annotation.MenuRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
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
    // Alto desplegado: 40 % de la pantalla (no la mitad: con letra grande dejaría poco para la lista),
    // más la barra de estado, porque el encabezado empieza detrás de ella
    val controles = controlesFlotantes
    val margenSuperior = controles?.margenSuperior ?: 0
    encabezado.colapsable.layoutParams.height =
        (resources.displayMetrics.heightPixels * PROPORCION_ENCABEZADO).toInt() + margenSuperior
    // La información empieza bajo la barra de estado y la fila de botones flotantes
    encabezado.zonaInfo.updatePadding(
        top = margenSuperior + resources.getDimensionPixelSize(R.dimen.alto_botones_superiores)
    )

    encabezado.ponerTitulo(titulo)
    encabezado.tvDescripcion.text = descripcion?.let { getString(it) }
    encabezado.tvDescripcion.visibility = if (descripcion == null) View.GONE else View.VISIBLE

    // Botones redondos "volver" y "⋮": flotan sobre todas las pantallas (MainActivity)
    controles?.mostrarControles(atras, { findNavController().navigateUp() }, menu, alElegir)

    // Al escribir en un campo (logins, formularios) el encabezado se recoge: con el teclado
    // abierto, desplegado ocuparía casi todo el espacio y taparía los demás campos
    val alEnfocar = ViewTreeObserver.OnGlobalFocusChangeListener { _, nuevo ->
        if (nuevo is EditText) encabezado.root.setExpanded(false, true)
    }
    encabezado.root.viewTreeObserver.addOnGlobalFocusChangeListener(alEnfocar)
    viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            encabezado.root.viewTreeObserver.takeIf { it.isAlive }?.removeOnGlobalFocusChangeListener(alEnfocar)
        }
    })

    // Al deslizar, la información se desvanece mientras sale de la pantalla y, al final,
    // aparece la franja difuminada bajo los botones
    encabezado.root.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { barra, desplazamiento ->
        val recorrido = barra.totalScrollRange.takeIf { it > 0 } ?: return@OnOffsetChangedListener
        val plegado = abs(desplazamiento).toFloat() / recorrido
        encabezado.zonaInfo.alpha = (1f - plegado * 1.4f).coerceIn(0f, 1f)
        if (isResumed || isVisible) controlesFlotantes?.ponerPlegado(plegado)
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
