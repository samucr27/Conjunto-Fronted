package com.example.conjuntoresidencial.ui.comun

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.conjuntoresidencial.R
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import kotlin.math.abs

/**
 * P11: una tarjeta de una lista hecha a mano (LinearLayout) que se quita deslizándola de derecha a
 * izquierda, igual que las notificaciones: detrás aparece "Eliminar" en rojo. Hacia la derecha no
 * hace nada. Mientras el dedo va más de lado que hacia abajo, "jalar para actualizar" y el
 * desplazamiento de la lista quedan apagados. Con TalkBack, la acción "Eliminar".
 *
 * Uso: lista.addView(FilaDeslizable.envolver(fila, binding.swipe) { quitar(c) })
 */
@SuppressLint("ViewConstructor")
class FilaDeslizable private constructor(
    context: Context,
    private val fila: View,
    private val swipe: SwipeRefreshLayout?,
    private val alQuitar: () -> Unit
) : FrameLayout(context) {

    private val umbral = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private val velocidadMinima = ViewConfiguration.get(context).scaledMinimumFlingVelocity * 4f
    private val densidad = resources.displayMetrics.density
    private var x0 = 0f
    private var y0 = 0f
    private var deLado: Boolean? = null
    private var seguidor: VelocityTracker? = null
    private var quitada = false

    private val pinturaFondo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = MaterialColors.getColor(fila, androidx.appcompat.R.attr.colorError)
    }
    private val pinturaTexto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = MaterialColors.getColor(fila, com.google.android.material.R.attr.colorOnError)
        textSize = 15 * resources.displayMetrics.scaledDensity
        typeface = Typeface.DEFAULT_BOLD
    }
    private val papelera = ContextCompat.getDrawable(context, R.drawable.ic_delete)!!.mutate().apply {
        setTint(pinturaTexto.color)
    }

    init {
        setWillNotDraw(false)
        // Los márgenes de la tarjeta pasan al contenedor: el fondo rojo queda del mismo tamaño
        val lp = fila.layoutParams as? MarginLayoutParams
        layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            if (lp != null) setMargins(lp.leftMargin, lp.topMargin, lp.rightMargin, lp.bottomMargin)
        }
        addView(fila, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        ViewCompat.addAccessibilityAction(fila, context.getString(R.string.eliminar)) { _, _ -> quitar(); true }
    }

    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                x0 = e.rawX; y0 = e.rawY; deLado = null
                seguidor?.recycle(); seguidor = VelocityTracker.obtain().also { it.addMovement(e) }
            }
            MotionEvent.ACTION_MOVE -> {
                seguidor?.addMovement(e)
                val dx = e.rawX - x0
                val dy = abs(e.rawY - y0)
                if (deLado == null && (abs(dx) > umbral || dy > umbral)) {
                    deLado = abs(dx) > dy
                    if (deLado == true) {
                        parent?.requestDisallowInterceptTouchEvent(true)
                        swipe?.let { if (!it.isRefreshing) it.isEnabled = false }
                    }
                }
                // De lado: el gesto es de la fila (así la tarjeta no lo toma como un toque)
                if (deLado == true) return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> soltar()
        }
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        seguidor?.addMovement(e)
        when (e.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                // Solo hacia la izquierda
                fila.translationX = (e.rawX - x0).coerceAtMost(0f)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                seguidor?.computeCurrentVelocity(1000)
                val vx = seguidor?.xVelocity ?: 0f
                val lejos = -fila.translationX > width * 0.4f
                val rapido = vx < -velocidadMinima && fila.translationX < -umbral
                if (lejos || rapido) quitar() else volver()
                soltar()
            }
            MotionEvent.ACTION_CANCEL -> { volver(); soltar() }
        }
        return true
    }

    private fun soltar() {
        deLado = null
        seguidor?.recycle(); seguidor = null
        swipe?.isEnabled = true
    }

    private fun volver() {
        fila.animate().translationX(0f).setDuration(180).setUpdateListener { invalidate() }.start()
    }

    /** Sale hacia la izquierda, la fila se cierra y avisa. */
    private fun quitar() {
        if (quitada) return
        quitada = true
        fila.animate().translationX(-width.toFloat()).setDuration(180).setUpdateListener { invalidate() }
            .withEndAction {
                val alto = height
                val lp = layoutParams as? MarginLayoutParams
                val arriba = lp?.topMargin ?: 0
                val abajo = lp?.bottomMargin ?: 0
                ValueAnimator.ofFloat(1f, 0f).apply {
                    duration = 160
                    addUpdateListener {
                        val f = it.animatedValue as Float
                        layoutParams = layoutParams.apply { height = (alto * f).toInt() }
                        lp?.topMargin = (arriba * f).toInt(); lp?.bottomMargin = (abajo * f).toInt()
                        requestLayout()
                    }
                    doOnEnd { visibility = GONE; alQuitar() }
                    start()
                }
            }.start()
    }

    /** Fondo rojo redondeado del lado que queda al descubierto, con la papelera y "Eliminar". */
    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val dX = fila.translationX
        if (dX >= 0f) return
        val radio = (fila as? MaterialCardView)?.radius ?: (16 * densidad)
        c.drawRoundRect(width + dX, 0f, width.toFloat(), height.toFloat(), radio, radio, pinturaFondo)
        val texto = context.getString(R.string.eliminar)
        val icono = (24 * densidad).toInt()
        val margen = (20 * densidad).toInt()
        if (abs(dX) < margen + icono) return
        val anchoTexto = pinturaTexto.measureText(texto)
        val centroY = height / 2
        val x = (width - margen - icono - 8 * densidad - anchoTexto).toInt()
        papelera.setBounds(x, centroY - icono / 2, x + icono, centroY + icono / 2)
        papelera.draw(c)
        val base = centroY - (pinturaTexto.descent() + pinturaTexto.ascent()) / 2
        c.drawText(texto, x + icono + 8 * densidad, base, pinturaTexto)
    }

    private fun ValueAnimator.doOnEnd(accion: () -> Unit) {
        addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) = accion()
        })
    }

    companion object {
        /** Envuelve [fila] (ya inflada con su lista como padre) para poder quitarla deslizando. */
        fun envolver(fila: View, swipe: SwipeRefreshLayout?, alQuitar: () -> Unit): View =
            // Los márgenes de la tarjeta los toma el contenedor (al agregarla queda sin ellos)
            FilaDeslizable(fila.context, fila, swipe, alQuitar)
    }
}
