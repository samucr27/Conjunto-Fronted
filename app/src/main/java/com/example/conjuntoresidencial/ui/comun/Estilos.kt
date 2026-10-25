package com.example.conjuntoresidencial.ui.comun

import android.util.TypedValue
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.core.widget.TextViewCompat
import com.google.android.material.color.MaterialColors

/**
 * Para textos creados en código: aplica la apariencia del tema (?attr/textAppearance…) como
 * en los layouts, así usan la tipografía de la app (Poppins) y no la de Material por defecto.
 */
fun TextView.aparienciaDelTema(@AttrRes apariencia: Int, @AttrRes color: Int) {
    val valor = TypedValue()
    if (context.theme.resolveAttribute(apariencia, valor, true)) {
        TextViewCompat.setTextAppearance(this, valor.resourceId)
    }
    setTextColor(MaterialColors.getColor(this, color))
}
