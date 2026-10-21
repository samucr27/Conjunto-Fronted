package com.example.conjuntoresidencial.ui.comun

import androidx.annotation.DrawableRes
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.conjuntoresidencial.util.Resource

/**
 * Aplica el resultado de una carga a una pantalla de lista: indicador mientras carga,
 * mensaje si no hay datos, error con "Reintentar", o la lista.
 *
 * Con datos ya en pantalla, al recargar deslizando solo se ve el indicador del gesto.
 *
 * @param alCargar recibe la lista para entregarla al adaptador (submitList)
 */
fun <T> EstadoPantallaView.mostrarRecurso(
    resource: Resource<List<T>>,
    swipe: SwipeRefreshLayout?,
    @DrawableRes iconoVacio: Int,
    tituloVacio: String,
    mensajeVacio: String? = null,
    reintentar: () -> Unit,
    alCargar: (List<T>) -> Unit
) {
    when (resource) {
        is Resource.Loading -> if (swipe?.isRefreshing != true) mostrarCargando()
        is Resource.Success -> {
            swipe?.isRefreshing = false
            val lista = resource.data.orEmpty()
            alCargar(lista)
            if (lista.isEmpty()) mostrarVacio(iconoVacio, tituloVacio, mensajeVacio) else mostrarContenido()
        }
        is Resource.Error -> {
            swipe?.isRefreshing = false
            mostrarError(resource.message, reintentar)
        }
    }
}
