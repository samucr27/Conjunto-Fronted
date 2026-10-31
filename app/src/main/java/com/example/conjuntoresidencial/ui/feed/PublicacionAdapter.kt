package com.example.conjuntoresidencial.ui.feed

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.ItemPublicacionBinding
import com.example.conjuntoresidencial.model.AdjuntoPublicacion
import com.example.conjuntoresidencial.model.PublicacionDTO
import com.example.conjuntoresidencial.ui.comun.DiffPorId
import com.example.conjuntoresidencial.util.Formatos
import com.google.android.material.button.MaterialButton

/**
 * Publicación del feed: quién y cuándo, el texto, las imágenes en fila (deslizables) y los
 * archivos como botones. Tocar una imagen o un archivo lo abre ([onAbrir]); con [onOpciones]
 * (administración) tocar la tarjeta permite editarla o eliminarla.
 */
class PublicacionAdapter(
    private val onAbrir: (AdjuntoPublicacion) -> Unit,
    private val onOpciones: ((PublicacionDTO) -> Unit)? = null,
    // Tocar la tarjeta (o un adjunto) la marca como leída
    private val onLeer: ((PublicacionDTO) -> Unit)? = null
) : ListAdapter<PublicacionDTO, PublicacionAdapter.VH>(DiffPorId { it.id }) {

    class VH(val binding: ItemPublicacionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemPublicacionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: VH, position: Int) {
        val p = getItem(position)
        val ctx = h.binding.root.context
        h.binding.tvAutorPublicacion.text = p.autor
        h.binding.tvFechaPublicacion.text = listOfNotNull(
            "${Formatos.fecha(p.creadaEn)} · ${Formatos.hora(p.creadaEn.substringAfter('T').take(5))}",
            if (p.editadaEn != null) ctx.getString(R.string.feed_editada) else null
        ).joinToString(" · ")
        h.binding.tvTextoPublicacion.text = p.texto
        h.binding.tvTextoPublicacion.visibility = if (p.texto.isBlank()) View.GONE else View.VISIBLE

        h.binding.tvTituloPublicacion.text = p.titulo
        h.binding.tvTituloPublicacion.visibility = if (p.titulo.isNullOrBlank()) View.GONE else View.VISIBLE
        // "Urgente · Torre 3 · Fijada": solo lo que aporta algo
        val etiquetas = listOfNotNull(
            ctx.getString(EtiquetasPublicacion.categoria(p.categoria)).takeIf { p.categoria != "GENERAL" },
            ctx.getString(EtiquetasPublicacion.audiencia(p.audiencia)).takeIf { p.audiencia != "TODOS" },
            p.torre?.let { ctx.getString(R.string.feed_torre_etiqueta, it) },
            ctx.getString(R.string.feed_fijada).takeIf { p.fijado }
        )
        h.binding.tvEtiquetasPublicacion.text = etiquetas.joinToString(" · ")
        h.binding.tvEtiquetasPublicacion.visibility = if (etiquetas.isEmpty()) View.GONE else View.VISIBLE
        h.binding.puntoNoLeida.visibility = if (p.leida) View.GONE else View.VISIBLE
        h.binding.root.setOnClickListener { if (!p.leida) onLeer?.invoke(p) }

        val imagenes = p.adjuntos.filter { it.tipo == "IMAGEN" }
        h.binding.scrollImagenes.visibility = if (imagenes.isEmpty()) View.GONE else View.VISIBLE
        h.binding.contenedorImagenes.removeAllViews()
        val alto = ctx.resources.getDimensionPixelSize(R.dimen.feed_alto_imagen)
        imagenes.forEach { adj ->
            val img = LayoutInflater.from(ctx).inflate(R.layout.item_publicacion_imagen, h.binding.contenedorImagenes, false) as ImageView
            // Una sola imagen ocupa el ancho; varias, en fila
            if (imagenes.size == 1) img.layoutParams = img.layoutParams.apply { width = ViewGroup.LayoutParams.MATCH_PARENT }
            img.layoutParams.height = alto
            img.contentDescription = ctx.getString(R.string.feed_desc_imagen, adj.nombre)
            img.load(adj.url) {
                crossfade(true)
                placeholder(R.drawable.fondo_imagen_cargando)
                error(R.drawable.fondo_imagen_cargando)
            }
            img.setOnClickListener { if (!p.leida) onLeer?.invoke(p); onAbrir(adj) }
            h.binding.contenedorImagenes.addView(img)
        }

        h.binding.contenedorArchivos.removeAllViews()
        p.adjuntos.filter { it.tipo != "IMAGEN" }.forEach { adj ->
            val boton = LayoutInflater.from(ctx).inflate(R.layout.item_publicacion_archivo, h.binding.contenedorArchivos, false) as MaterialButton
            boton.text = adj.nombre
            boton.setOnClickListener { if (!p.leida) onLeer?.invoke(p); onAbrir(adj) }
            h.binding.contenedorArchivos.addView(boton)
        }

        h.binding.btnOpcionesPublicacion.visibility = if (onOpciones != null) View.VISIBLE else View.GONE
        h.binding.btnOpcionesPublicacion.setOnClickListener { onOpciones?.invoke(p) }
    }
}
