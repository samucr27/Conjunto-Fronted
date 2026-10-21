package com.example.conjuntoresidencial.ui.comun

import android.annotation.SuppressLint
import androidx.recyclerview.widget.DiffUtil

/**
 * DiffUtil para ListAdapter: dos elementos son "el mismo" si tienen el mismo id,
 * y no se redibujan si su contenido no cambió (comparación de data classes).
 * Así, al recargar o cambiar un estado, la lista no salta ni parpadea.
 */
class DiffPorId<T : Any>(private val id: (T) -> Any?) : DiffUtil.ItemCallback<T>() {
    override fun areItemsTheSame(viejo: T, nuevo: T) = id(viejo) == id(nuevo)

    @SuppressLint("DiffUtilEquals") // los elementos son data classes
    override fun areContentsTheSame(viejo: T, nuevo: T) = viejo == nuevo
}
