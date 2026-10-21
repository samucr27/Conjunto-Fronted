package com.example.conjuntoresidencial.ui.comun

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.ViewEstadoPantallaBinding

/**
 * Estados de una pantalla con datos remotos: cargando, vacía, error o con contenido.
 *
 * Uso en el layout, en el mismo contenedor que la lista:
 *   <com.example.conjuntoresidencial.ui.comun.EstadoPantallaView
 *       android:id="@+id/estado" android:layout_width="match_parent"
 *       android:layout_height="match_parent" />
 * y en el fragment:
 *   binding.estado.vincular(binding.rvLista)
 *   binding.estado.mostrarCargando() / mostrarVacio(...) / mostrarError(...) / mostrarContenido()
 * El componente muestra u oculta la vista de contenido vinculada.
 */
class EstadoPantallaView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val binding = ViewEstadoPantallaBinding.inflate(LayoutInflater.from(context), this)
    private var contenido: View? = null

    fun vincular(contenido: View) {
        this.contenido = contenido
    }

    fun mostrarCargando() {
        visibility = VISIBLE
        contenido?.visibility = GONE
        binding.progreso.visibility = VISIBLE
        binding.bloqueMensaje.visibility = GONE
    }

    fun mostrarContenido() {
        visibility = GONE
        contenido?.visibility = VISIBLE
    }

    /** Sin datos. [accion] opcional, por ejemplo "Agregar vehículo". */
    fun mostrarVacio(
        @DrawableRes icono: Int,
        titulo: String,
        mensaje: String? = null,
        textoAccion: String? = null,
        accion: (() -> Unit)? = null
    ) = mostrarMensaje(icono, titulo, mensaje, textoAccion, accion)

    /** Error con el mensaje del backend y botón "Reintentar". */
    fun mostrarError(mensaje: String, reintentar: () -> Unit) = mostrarMensaje(
        R.drawable.ic_error_outline,
        context.getString(R.string.estado_error_titulo),
        mensaje,
        context.getString(R.string.reintentar),
        reintentar
    )

    private fun mostrarMensaje(
        @DrawableRes icono: Int,
        titulo: String,
        mensaje: String?,
        textoAccion: String?,
        accion: (() -> Unit)?
    ) {
        visibility = VISIBLE
        contenido?.visibility = GONE
        binding.progreso.visibility = GONE
        binding.bloqueMensaje.visibility = VISIBLE
        binding.icono.setImageResource(icono)
        binding.titulo.text = titulo
        binding.mensaje.text = mensaje
        binding.mensaje.visibility = if (mensaje.isNullOrBlank()) GONE else VISIBLE
        if (textoAccion != null && accion != null) {
            binding.accion.text = textoAccion
            binding.accion.setOnClickListener { accion() }
            binding.accion.visibility = VISIBLE
        } else {
            binding.accion.visibility = GONE
        }
    }
}
