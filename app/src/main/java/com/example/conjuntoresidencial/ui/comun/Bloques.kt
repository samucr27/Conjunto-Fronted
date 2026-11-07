package com.example.conjuntoresidencial.ui.comun

import android.content.Context
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import com.example.conjuntoresidencial.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * P11: piezas para armar desde el código las pantallas de la hoja (layout/fragment_pantalla_contenido):
 * títulos de sección, tarjetas con etiqueta de estado, filas "dato: valor", cifras y campos.
 * Mantienen el estilo del resto de la app (tarjetas redondeadas, etiquetas de color, letra del tema).
 */
object Bloques {

    private fun dp(c: Context, v: Int) = (v * c.resources.displayMetrics.density).toInt()

    private fun ancho(arriba: Int = 0, abajo: Int = 0, c: Context) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = dp(c, arriba); bottomMargin = dp(c, abajo) }

    fun texto(c: Context, t: CharSequence?, estilo: Int, color: Int = com.google.android.material.R.attr.colorOnSurface): TextView =
        TextView(c).apply {
            TextViewCompat.setTextAppearance(this, estilo)
            setTextColor(MaterialColors.getColor(c, color, 0))
            text = t
        }

    /** Título de sección ("Cuotas", "Historial"...). */
    fun seccion(padre: ViewGroup, t: CharSequence): TextView =
        texto(padre.context, t, R.style.TextAppearance_App_TitleSmall, androidx.appcompat.R.attr.colorPrimary).apply {
            isFocusable = true
            androidx.core.view.ViewCompat.setAccessibilityHeading(this, true)
            padre.addView(this, ancho(20, 8, padre.context))
        }

    /** Texto suelto de ayuda o de aviso. */
    fun parrafo(padre: ViewGroup, t: CharSequence, apagado: Boolean = true): TextView =
        texto(padre.context, t, R.style.TextAppearance_App_BodyMedium,
            if (apagado) com.google.android.material.R.attr.colorOnSurfaceVariant else com.google.android.material.R.attr.colorOnSurface)
            .also { padre.addView(it, ancho(4, 4, padre.context)) }

    /** Etiqueta de color (como las de estado del resto de la app). */
    fun etiqueta(c: Context, t: String, tono: TonoEstado): TextView = TextView(c).apply {
        mostrarEstado(EstadoVisual(R.string.etiqueta_libre, tono, t))
    }

    /**
     * Tarjeta con título, línea de detalle y, a la derecha, una etiqueta. Tocable si hay [alTocar].
     * Devuelve el contenedor interno para agregarle más cosas (botones...).
     */
    fun tarjeta(
        padre: ViewGroup, titulo: CharSequence, detalle: CharSequence? = null, extra: CharSequence? = null,
        etiqueta: Pair<String, TonoEstado>? = null, alTocar: (() -> Unit)? = null
    ): LinearLayout {
        val c = padre.context
        val card = MaterialCardView(c).apply {
            radius = dp(c, 20).toFloat()
            cardElevation = 0f
            strokeWidth = 0
            setCardBackgroundColor(MaterialColors.getColor(c, com.google.android.material.R.attr.colorSurfaceContainerLow, 0))
            if (alTocar != null) { isClickable = true; isFocusable = true; setOnClickListener { alTocar() } }
        }
        val cuerpo = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(c, 16), dp(c, 14), dp(c, 16), dp(c, 14))
        }
        val fila = LinearLayout(c).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        fila.addView(texto(c, titulo, R.style.TextAppearance_App_TitleSmall),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        etiqueta?.let { (t, tono) -> fila.addView(etiqueta(c, t, tono), LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginStart = dp(c, 8) }) }
        cuerpo.addView(fila)
        detalle?.let { cuerpo.addView(texto(c, it, R.style.TextAppearance_App_BodyMedium,
            com.google.android.material.R.attr.colorOnSurfaceVariant), ancho(4, 0, c)) }
        extra?.let { cuerpo.addView(texto(c, it, R.style.TextAppearance_App_BodySmall,
            com.google.android.material.R.attr.colorOnSurfaceVariant), ancho(2, 0, c)) }
        card.addView(cuerpo)
        padre.addView(card, ancho(0, 10, c))
        return cuerpo
    }

    /** "Dato: valor" en dos columnas, dentro de una tarjeta o suelto. */
    fun dato(padre: ViewGroup, etiqueta: CharSequence, valor: CharSequence?) {
        val c = padre.context
        val fila = LinearLayout(c).apply { orientation = LinearLayout.HORIZONTAL }
        fila.addView(texto(c, etiqueta, R.style.TextAppearance_App_BodyMedium, com.google.android.material.R.attr.colorOnSurfaceVariant),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.42f))
        fila.addView(texto(c, valor ?: "—", R.style.TextAppearance_App_BodyMedium),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.58f).apply { marginStart = dp(c, 8) })
        fila.contentDescription = "$etiqueta: ${valor ?: "—"}"
        fila.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        padre.addView(fila, ancho(4, 4, c))
    }

    /** Fila de cifras grandes (como las del resumen de la cartera). */
    fun cifras(padre: ViewGroup, vararg cifras: Pair<String, String>) {
        val c = padre.context
        val fila = LinearLayout(c).apply { orientation = LinearLayout.HORIZONTAL }
        cifras.forEachIndexed { i, (valor, etiqueta) ->
            val card = MaterialCardView(c).apply {
                radius = dp(c, 20).toFloat(); cardElevation = 0f; strokeWidth = 0
                setCardBackgroundColor(MaterialColors.getColor(c, com.google.android.material.R.attr.colorSurfaceContainerLow, 0))
                contentDescription = "$etiqueta: $valor"
            }
            val col = LinearLayout(c).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(c, 12), dp(c, 12), dp(c, 12), dp(c, 12))
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            }
            col.addView(texto(c, valor, R.style.TextAppearance_App_TitleMedium, androidx.appcompat.R.attr.colorPrimary))
            col.addView(texto(c, etiqueta, R.style.TextAppearance_App_LabelMedium, com.google.android.material.R.attr.colorOnSurfaceVariant))
            card.addView(col)
            fila.addView(card, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                if (i > 0) marginStart = dp(c, 8)
            })
        }
        padre.addView(fila, ancho(0, 10, c))
    }

    fun boton(padre: ViewGroup, t: CharSequence, relleno: Boolean = true, icono: Int? = null, alTocar: () -> Unit): MaterialButton {
        val c = padre.context
        val b = if (relleno) MaterialButton(c) else MaterialButton(c, null, com.google.android.material.R.attr.materialButtonOutlinedStyle)
        b.text = t
        b.minHeight = dp(c, 52)
        icono?.let { b.setIconResource(it); b.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START }
        b.setOnClickListener { alTocar() }
        padre.addView(b, ancho(4, 4, c))
        return b
    }

    /** Campo de texto con borde. [tipo]: InputType. Devuelve el campo (para leerlo o ponerle error). */
    fun campo(padre: ViewGroup, pista: CharSequence, tipo: Int = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES,
              ayuda: CharSequence? = null, valor: CharSequence? = null, maximo: Int? = null): TextInputLayout {
        val c = padre.context
        val til = TextInputLayout(c, null, com.google.android.material.R.attr.textInputOutlinedStyle).apply {
            hint = pista
            ayuda?.let { helperText = it }
            maximo?.let { isCounterEnabled = true; counterMaxLength = it }
        }
        til.addView(TextInputEditText(til.context).apply {
            inputType = tipo
            if (tipo and InputType.TYPE_TEXT_FLAG_MULTI_LINE != 0) maxLines = 4 else maxLines = 1
            setText(valor)
            maximo?.let { filters = arrayOf(android.text.InputFilter.LengthFilter(it)) }
        })
        padre.addView(til, ancho(8, 0, c))
        return til
    }

    val TextInputLayout.textoLimpio: String get() = editText?.text?.toString()?.trim().orEmpty()
}
