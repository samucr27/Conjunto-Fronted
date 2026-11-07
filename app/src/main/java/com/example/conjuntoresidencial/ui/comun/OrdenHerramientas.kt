package com.example.conjuntoresidencial.ui.comun

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.util.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.color.MaterialColors

/**
 * P11: cada persona ordena a su gusto las tarjetas de Herramientas (arrastrándolas en una lista).
 * El orden se guarda en el celular, por app y por cuenta; las tarjetas nuevas o sin orden guardado
 * van al final, en su orden de siempre.
 */
object OrdenHerramientas {

    private const val PREFS = "orden_herramientas"

    private fun clave(ctx: Context): String {
        val s = SessionManager(ctx)
        val quien = s.getUsuario().ifEmpty { s.getApartamentoId().toString() }
        return "${s.getRol()}:$quien"
    }

    private fun guardado(ctx: Context): List<String> =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(clave(ctx), null)
            ?.split(',')?.filter { it.isNotBlank() }.orEmpty()

    private fun nombre(v: View): String = v.resources.getResourceEntryName(v.id)

    /** [vistas] en el orden guardado; las que no están en él, al final y en su orden. */
    fun ordenar(ctx: Context, vistas: List<View>): List<View> {
        val orden = guardado(ctx)
        if (orden.isEmpty()) return vistas
        return vistas.sortedBy { v -> orden.indexOf(nombre(v)).let { if (it < 0) Int.MAX_VALUE else it } }
    }

    /** Las tarjetas de la cuadrícula (las que tienen ícono, título y subtítulo). */
    private fun tarjetas(grid: ViewGroup): List<View> = (0 until grid.childCount).map { grid.getChildAt(it) }
        .filter { it.id != View.NO_ID && it.findViewById<View>(R.id.titulo) != null && it.findViewById<View>(R.id.subtitulo) != null }

    /**
     * Pone las tarjetas de [grid] en el orden guardado, en los mismos lugares que ocupan. La
     * primera vez recuerda el orden de siempre (para "Restablecer").
     */
    fun aplicar(grid: ViewGroup) {
        val actuales = tarjetas(grid)
        if (actuales.size < 2) return
        @Suppress("UNCHECKED_CAST")
        val porDefecto = (grid.getTag(R.id.orden_por_defecto) as? List<View>)?.filter { it in actuales }
            ?.takeIf { it.size == actuales.size }
            ?: actuales.also { grid.setTag(R.id.orden_por_defecto, it) }
        val lugares = actuales.map { grid.indexOfChild(it) }
        val ordenadas = ordenar(grid.context, porDefecto)
        actuales.forEach { grid.removeView(it) }
        lugares.zip(ordenadas).sortedBy { it.first }.forEach { (lugar, v) -> grid.addView(v, lugar) }
    }

    private fun guardar(ctx: Context, visibles: List<String>) {
        // Las que hoy no se ven (según el rol o si está arrendado) conservan su lugar relativo al final
        val resto = guardado(ctx).filter { it !in visibles }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(clave(ctx), (visibles + resto).joinToString(",")).apply()
    }

    private fun restablecer(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(clave(ctx)).apply()
    }

    private class Item(val nombre: String, val titulo: CharSequence, val icono: Drawable?)

    /** La lista para arrastrar. [alCambiar] vuelve a armar la pantalla con el orden nuevo. */
    @SuppressLint("ClickableViewAccessibility")
    fun editar(fragment: Fragment, grid: ViewGroup, alCambiar: () -> Unit) {
        val ctx = fragment.requireContext()
        val items = tarjetas(grid).map { v ->
            Item(nombre(v), v.findViewById<TextView>(R.id.titulo).text, v.findViewById<ImageView>(R.id.icono)?.drawable?.constantState?.newDrawable()?.mutate())
        }.toMutableList()
        val d = ctx.resources.displayMetrics.density
        val lista = RecyclerView(ctx).apply {
            layoutManager = LinearLayoutManager(ctx)
            setPadding(0, (8 * d).toInt(), 0, 0)
            clipToPadding = false
        }
        lateinit var arrastre: ItemTouchHelper
        val adaptador = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun getItemCount() = items.size
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val fila = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    minimumHeight = (56 * d).toInt()
                    setPadding((24 * d).toInt(), 0, (16 * d).toInt(), 0)
                    layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    addView(ImageView(ctx).apply {
                        id = R.id.icono
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                        imageTintList = android.content.res.ColorStateList.valueOf(
                            MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary))
                    }, LinearLayout.LayoutParams((24 * d).toInt(), (24 * d).toInt()))
                    addView(TextView(ctx).apply {
                        id = R.id.titulo
                        setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
                        setPadding((16 * d).toInt(), 0, (16 * d).toInt(), 0)
                    }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                    addView(ImageView(ctx).apply {
                        id = R.id.subtitulo
                        setImageResource(R.drawable.ic_drag_handle)
                        contentDescription = ctx.getString(R.string.herramientas_arrastrar)
                        setPadding((12 * d).toInt(), (12 * d).toInt(), (12 * d).toInt(), (12 * d).toInt())
                    }, LinearLayout.LayoutParams((48 * d).toInt(), (48 * d).toInt()))
                }
                return object : RecyclerView.ViewHolder(fila) {}.also { vh ->
                    // Se arrastra desde la agarradera; con TalkBack, "Subir" y "Bajar"
                    fila.findViewById<View>(R.id.subtitulo).setOnTouchListener { _, e ->
                        if (e.actionMasked == MotionEvent.ACTION_DOWN) arrastre.startDrag(vh)
                        false
                    }
                }
            }

            @Suppress("DEPRECATION")
            override fun onBindViewHolder(vh: RecyclerView.ViewHolder, posicion: Int) {
                val item = items[posicion]
                vh.itemView.findViewById<ImageView>(R.id.icono).setImageDrawable(item.icono)
                vh.itemView.findViewById<TextView>(R.id.titulo).text = item.titulo
                vh.itemView.contentDescription = ctx.getString(R.string.herramientas_posicion, item.titulo, posicion + 1, items.size)
                ViewCompat.removeAccessibilityAction(vh.itemView, R.id.accion_subir)
                ViewCompat.removeAccessibilityAction(vh.itemView, R.id.accion_bajar)
                if (posicion > 0) ViewCompat.replaceAccessibilityAction(vh.itemView,
                    AccessibilityActionCompat(R.id.accion_subir, ctx.getString(R.string.herramientas_subir), null), null) { _, _ ->
                    mover(vh.adapterPosition, vh.adapterPosition - 1); true
                }
                if (posicion < items.size - 1) ViewCompat.replaceAccessibilityAction(vh.itemView,
                    AccessibilityActionCompat(R.id.accion_bajar, ctx.getString(R.string.herramientas_bajar), null), null) { _, _ ->
                    mover(vh.adapterPosition, vh.adapterPosition + 1); true
                }
            }

            fun mover(desde: Int, hasta: Int) {
                if (desde < 0 || hasta < 0 || desde >= items.size || hasta >= items.size) return
                items.add(hasta, items.removeAt(desde))
                notifyItemMoved(desde, hasta)
                // Las acciones y la posición leída cambian en las dos filas
                notifyItemRangeChanged(minOf(desde, hasta), kotlin.math.abs(desde - hasta) + 1, Unit)
            }
        }
        arrastre = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            // Solo desde la agarradera
            override fun isLongPressDragEnabled() = true
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, destino: RecyclerView.ViewHolder): Boolean {
                @Suppress("DEPRECATION")
                val desde = vh.adapterPosition
                @Suppress("DEPRECATION")
                val hasta = destino.adapterPosition
                items.add(hasta, items.removeAt(desde))
                adaptador.notifyItemMoved(desde, hasta)
                return true
            }
            override fun onSwiped(vh: RecyclerView.ViewHolder, direccion: Int) {}
            override fun onSelectedChanged(vh: RecyclerView.ViewHolder?, estado: Int) {
                super.onSelectedChanged(vh, estado)
                if (estado == ItemTouchHelper.ACTION_STATE_DRAG) vh?.itemView?.apply { elevation = 8 * d; alpha = 0.95f }
            }
            override fun clearView(rv: RecyclerView, vh: RecyclerView.ViewHolder) {
                super.clearView(rv, vh)
                vh.itemView.elevation = 0f; vh.itemView.alpha = 1f
                adaptador.notifyItemRangeChanged(0, items.size, Unit)
            }
        })
        lista.adapter = adaptador
        arrastre.attachToRecyclerView(lista)

        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.herramientas_ordenar)
            .setMessage(R.string.herramientas_ordenar_ayuda)
            .setView(lista)
            .setPositiveButton(R.string.btn_guardar) { _, _ -> guardar(ctx, items.map { it.nombre }); alCambiar() }
            .setNeutralButton(R.string.herramientas_restablecer) { _, _ -> restablecer(ctx); alCambiar() }
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
    }
}
