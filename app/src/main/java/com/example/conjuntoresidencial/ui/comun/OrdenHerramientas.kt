package com.example.conjuntoresidencial.ui.comun

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.Gravity
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
        igualarAltos(grid)
        permitirArrastrar(grid)
    }

    /**
     * P12: todas las tarjetas del alto de la más alta (si no, cada fila mide distinto según si el
     * texto ocupa una o dos líneas). Se recalcula cuando cambia el ancho (girar el celular).
     */
    fun igualarAltos(grid: ViewGroup) {
        if (grid.getTag(R.id.orden_igualar) == null) {
            grid.setTag(R.id.orden_igualar, true)
            grid.addOnLayoutChangeListener { v, l, _, r, _, ol, _, or, _ ->
                if (r - l != or - ol) (v as ViewGroup).post { medirYIgualar(v) }
            }
        }
        grid.post { medirYIgualar(grid) }
    }

    private fun medirYIgualar(grid: ViewGroup) {
        val lista = tarjetas(grid)
        val ancho = lista.firstOrNull()?.width ?: 0
        if (ancho <= 0) return
        val alto = lista.maxOf {
            it.measure(View.MeasureSpec.makeMeasureSpec(ancho, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            it.measuredHeight
        }
        var cambio = false
        lista.forEach { t ->
            if (t.layoutParams.height != alto) { t.layoutParams.height = alto; cambio = true }
        }
        if (cambio) grid.requestLayout()
    }

    /**
     * P12: mantener pulsada una tarjeta y arrastrarla a otro lugar de la cuadrícula. Las demás se
     * corren mientras se arrastra y el orden se guarda al soltarla.
     */
    private fun permitirArrastrar(grid: ViewGroup) {
        val auto = (grid.getTag(R.id.orden_autodesplazar) as? Autodesplazar)
            ?: Autodesplazar(grid).also { grid.setTag(R.id.orden_autodesplazar, it) }
        tarjetas(grid).forEach { tarjeta ->
            tarjeta.setOnLongClickListener { v ->
                v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                val sombra = View.DragShadowBuilder(v)
                val inicio = v.startDragAndDrop(null, sombra, v, 0)
                if (inicio) v.alpha = 0.25f
                inicio
            }
            tarjeta.setOnDragListener { destino, e ->
                val arrastrada = e.localState as? View ?: return@setOnDragListener false
                if (arrastrada.parent !== grid) return@setOnDragListener false
                if (e.action == android.view.DragEvent.ACTION_DRAG_LOCATION) auto.dedo(destino, e.y)
                // Al entrar en otra tarjeta, la arrastrada toma su lugar (después del evento: no se
                // cambian las vistas mientras se reparte)
                if (e.action == android.view.DragEvent.ACTION_DRAG_ENTERED && destino !== arrastrada) {
                    grid.post { if (destino.parent === grid && arrastrada.parent === grid) mover(grid, arrastrada, destino) }
                }
                true
            }
        }
        // El final del arrastre lo atiende la cuadrícula (la tarjeta movida pudo salir y volver a entrar)
        grid.setOnDragListener { _, e ->
            val arrastrada = e.localState as? View ?: return@setOnDragListener false
            when (e.action) {
                android.view.DragEvent.ACTION_DRAG_STARTED -> auto.empezar()
                android.view.DragEvent.ACTION_DRAG_LOCATION -> auto.dedo(grid, e.y)
                android.view.DragEvent.ACTION_DRAG_ENDED -> auto.parar()
            }
            if (e.action == android.view.DragEvent.ACTION_DRAG_ENDED && arrastrada.alpha < 1f) {
                arrastrada.alpha = 1f
                if (arrastrada.parent === grid) guardar(grid.context, tarjetas(grid).map { nombre(it) })
            }
            true
        }
    }

    /**
     * Mientras se arrastra una tarjeta, si el dedo está cerca del borde de arriba o de abajo de lo
     * que se ve (abajo: encima de la barra flotante), la pantalla se desplaza sola; más rápido cuanto
     * más cerca del borde.
     */
    private class Autodesplazar(private val grid: ViewGroup) : Runnable {
        private var y = -1f
        private var activo = false
        private val d = grid.resources.displayMetrics.density

        private fun desplazable(): View? {
            var p = grid.parent
            while (p is View) {
                if (p is androidx.core.widget.NestedScrollView || p is android.widget.ScrollView) return p
                p = p.parent
            }
            return null
        }

        fun empezar() {
            y = -1f
            if (!activo) { activo = true; grid.postOnAnimation(this) }
        }

        fun parar() { activo = false; y = -1f }

        /** [enY] dentro de [vista] → en la pantalla. */
        fun dedo(vista: View, enY: Float) {
            val pos = IntArray(2)
            vista.getLocationOnScreen(pos)
            y = pos[1] + enY
        }

        override fun run() {
            if (!activo) return
            val scroll = desplazable() ?: return
            if (y >= 0) {
                val pos = IntArray(2)
                scroll.getLocationOnScreen(pos)
                val arriba = maxOf(pos[1], 0).toFloat()
                var abajo = (pos[1] + scroll.height).toFloat()
                grid.rootView?.findViewById<View>(R.id.barraInferior)?.takeIf { it.isShown }?.let { barra ->
                        barra.getLocationOnScreen(pos)
                        abajo = minOf(abajo, pos[1].toFloat())
                    }
                abajo = minOf(abajo, grid.resources.displayMetrics.heightPixels.toFloat())
                val zona = 110 * d
                val maxPaso = 22 * d
                val paso = when {
                    y < arriba + zona -> -maxPaso * ((arriba + zona - y) / zona).coerceIn(0.2f, 1f)
                    y > abajo - zona -> maxPaso * ((y - (abajo - zona)) / zona).coerceIn(0.2f, 1f)
                    else -> 0f
                }
                if (paso != 0f) scroll.scrollBy(0, paso.toInt())
            }
            grid.postOnAnimation(this)
        }
    }

    /** Pone [arrastrada] en el lugar de [destino]; las del medio se corren un puesto. */
    private fun mover(grid: ViewGroup, arrastrada: View, destino: View) {
        val lista = tarjetas(grid)
        val lugares = lista.map { grid.indexOfChild(it) }
        val nuevo = lista.toMutableList()
        val hasta = nuevo.indexOf(destino)
        nuevo.remove(arrastrada)
        nuevo.add(hasta, arrastrada)
        android.transition.TransitionManager.beginDelayedTransition(grid, android.transition.ChangeBounds().setDuration(150))
        lista.forEach { grid.removeView(it) }
        lugares.zip(nuevo).sortedBy { it.first }.forEach { (lugar, v) -> grid.addView(v, lugar) }
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

    /**
     * La lista para ordenar, en una hoja que sube desde abajo: el título y la ayuda arriba, la lista
     * en el medio (se desplaza sola al arrastrar hacia un borde) y los botones siempre visibles abajo.
     * Se arrastra desde las rayas de la derecha (sin esperar) o manteniendo pulsada la fila.
     * [alCambiar] vuelve a armar la pantalla con el orden nuevo.
     */
    @android.annotation.SuppressLint("ClickableViewAccessibility")
    fun editar(fragment: Fragment, grid: ViewGroup, alCambiar: () -> Unit) {
        val ctx = fragment.requireContext()
        val items = tarjetas(grid).map { v ->
            Item(nombre(v), v.findViewById<TextView>(R.id.titulo).text, v.findViewById<ImageView>(R.id.icono)?.drawable?.constantState?.newDrawable()?.mutate())
        }.toMutableList()
        val d = ctx.resources.displayMetrics.density
        fun dp(n: Int) = (n * d).toInt()
        val dialogo = com.google.android.material.bottomsheet.BottomSheetDialog(ctx)

        val lista = RecyclerView(ctx).apply {
            layoutManager = LinearLayoutManager(ctx)
            setPadding(0, dp(4), 0, dp(4))
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        lateinit var arrastre: ItemTouchHelper
        val adaptador = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun getItemCount() = items.size
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val fila = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    minimumHeight = dp(56)
                    setPadding(dp(24), dp(4), dp(8), dp(4))
                    layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    setBackgroundColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainerLow))
                    addView(ImageView(ctx).apply {
                        id = R.id.icono
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                        imageTintList = android.content.res.ColorStateList.valueOf(
                            MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary))
                    }, LinearLayout.LayoutParams(dp(24), dp(24)))
                    addView(TextView(ctx).apply {
                        id = R.id.titulo
                        setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
                        setPadding(dp(16), 0, dp(8), 0)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
                            hyphenationFrequency = android.text.Layout.HYPHENATION_FREQUENCY_NORMAL
                    }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                    // Agarradera: al tocarla empieza a arrastrar de una vez
                    addView(ImageView(ctx).apply {
                        id = R.id.agarradera
                        setImageResource(R.drawable.ic_drag_handle)
                        contentDescription = ctx.getString(R.string.herramientas_arrastrar)
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                        setPadding(dp(12), dp(12), dp(12), dp(12))
                    }, LinearLayout.LayoutParams(dp(48), dp(48)))
                }
                return object : RecyclerView.ViewHolder(fila) {}.also { vh ->
                    fila.findViewById<View>(R.id.agarradera).setOnTouchListener { _, e ->
                        if (e.actionMasked == android.view.MotionEvent.ACTION_DOWN) arrastre.startDrag(vh)
                        false
                    }
                }
            }

            @Suppress("DEPRECATION")
            override fun onBindViewHolder(vh: RecyclerView.ViewHolder, posicion: Int) {
                val item = items[posicion]
                vh.itemView.findViewById<ImageView>(R.id.icono).setImageDrawable(item.icono)
                vh.itemView.findViewById<TextView>(R.id.titulo).text = item.titulo
                // Con TalkBack: la posición y las acciones "Subir" y "Bajar"
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
                notifyItemRangeChanged(minOf(desde, hasta), kotlin.math.abs(desde - hasta) + 1, Unit)
            }
        }
        arrastre = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun isLongPressDragEnabled() = true
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, destino: RecyclerView.ViewHolder): Boolean {
                @Suppress("DEPRECATION")
                val desde = vh.adapterPosition
                @Suppress("DEPRECATION")
                val hasta = destino.adapterPosition
                if (desde < 0 || hasta < 0) return false
                items.add(hasta, items.removeAt(desde))
                adaptador.notifyItemMoved(desde, hasta)
                return true
            }
            override fun onSwiped(vh: RecyclerView.ViewHolder, direccion: Int) {}
            override fun onSelectedChanged(vh: RecyclerView.ViewHolder?, estado: Int) {
                super.onSelectedChanged(vh, estado)
                if (estado == ItemTouchHelper.ACTION_STATE_DRAG) vh?.itemView?.apply {
                    performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                    elevation = 8 * d
                }
            }
            override fun clearView(rv: RecyclerView, vh: RecyclerView.ViewHolder) {
                super.clearView(rv, vh)
                vh.itemView.elevation = 0f
                adaptador.notifyItemRangeChanged(0, items.size, Unit)
            }
        })
        lista.adapter = adaptador
        arrastre.attachToRecyclerView(lista)

        val botones = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(16))
            fun boton(texto: Int, estilo: Int, accion: () -> Unit) =
                com.google.android.material.button.MaterialButton(ctx, null, estilo).apply {
                    setText(texto)
                    setOnClickListener { accion() }
                }
            addView(boton(R.string.herramientas_restablecer, com.google.android.material.R.attr.borderlessButtonStyle) {
                restablecer(ctx); dialogo.dismiss(); alCambiar()
            })
            addView(View(ctx), LinearLayout.LayoutParams(0, 1, 1f))
            addView(boton(R.string.btn_cancelar, com.google.android.material.R.attr.borderlessButtonStyle) { dialogo.dismiss() })
            addView(boton(R.string.btn_guardar, com.google.android.material.R.attr.materialButtonStyle) {
                guardar(ctx, items.map { it.nombre }); dialogo.dismiss(); alCambiar()
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginStart = dp(8) })
        }

        // Hasta el 85 % de la pantalla: la lista toma lo que sobra y los botones quedan siempre abajo
        val maximo = (ctx.resources.displayMetrics.heightPixels * 0.85f).toInt()
        val contenido = object : LinearLayout(ctx) {
            override fun onMeasure(ancho: Int, alto: Int) {
                val disponible = View.MeasureSpec.getSize(alto).takeIf { View.MeasureSpec.getMode(alto) != View.MeasureSpec.UNSPECIFIED && it > 0 } ?: maximo
                super.onMeasure(ancho, View.MeasureSpec.makeMeasureSpec(minOf(maximo, disponible), View.MeasureSpec.AT_MOST))
            }
        }.apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(ctx).apply {
                setText(R.string.herramientas_ordenar)
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_HeadlineSmall)
                setPadding(dp(24), dp(8), dp(24), dp(8))
            })
            addView(TextView(ctx).apply {
                setText(R.string.herramientas_ordenar_ayuda)
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
                setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant))
                setPadding(dp(24), 0, dp(24), dp(8))
            })
            addView(lista, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(botones)
        }
        // La lista mide su alto natural y, si no cabe, se encoge ella (no los botones)
        lista.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

        dialogo.setContentView(contenido)
        dialogo.behavior.apply {
            // La hoja no se arrastra (si no, al arrastrar una herramienta hacia abajo se cerraba)
            isDraggable = false
            skipCollapsed = true
            state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        }
        dialogo.show()
    }
}
