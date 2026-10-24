package com.example.conjuntoresidencial.ui.comun

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.LinearLayout
import android.widget.Switch
import androidx.annotation.DrawableRes
import androidx.core.content.res.use
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.ViewFilaOpcionBinding

/**
 * Fila de opción estilo One UI para Cuenta, Ajustes, Permisos y Legal: ícono, título,
 * subtítulo y, según [Tipo], una flecha (lleva a otra pantalla), un interruptor o un estado.
 * Toda la fila es el área táctil (mín. 56 dp) y TalkBack la lee como una sola cosa.
 */
class FilaOpcion @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    enum class Tipo { ENLACE, INTERRUPTOR, ESTADO }

    private val vista = ViewFilaOpcionBinding.inflate(LayoutInflater.from(context), this)
    var tipo: Tipo = Tipo.ENLACE
        private set

    /** Al tocar un interruptor se invierte solo y avisa aquí; para revertir, cambiar [activado]. */
    var alCambiar: ((Boolean) -> Unit)? = null
    private var alTocar: (() -> Unit)? = null

    var activado: Boolean
        get() = vista.interruptor.isChecked
        set(valor) { vista.interruptor.isChecked = valor }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = (56 * resources.displayMetrics.density).toInt()
        val relleno = (16 * resources.displayMetrics.density).toInt()
        setPadding(relleno, (12 * resources.displayMetrics.density).toInt(), relleno, (12 * resources.displayMetrics.density).toInt())
        isClickable = true
        isFocusable = true
        TypedValue().also { context.theme.resolveAttribute(android.R.attr.selectableItemBackground, it, true) }
            .let { setBackgroundResource(it.resourceId) }

        context.obtainStyledAttributes(attrs, R.styleable.FilaOpcion).use { a ->
            a.getResourceId(R.styleable.FilaOpcion_filaIcono, 0).let { if (it != 0) ponerIcono(it) else vista.marcoIcono.visibility = View.GONE }
            titulo = a.getText(R.styleable.FilaOpcion_filaTitulo)
            subtitulo = a.getText(R.styleable.FilaOpcion_filaSubtitulo)
            ponerTipo(Tipo.entries[a.getInt(R.styleable.FilaOpcion_filaTipo, 0)])
        }
        super.setOnClickListener {
            if (tipo == Tipo.INTERRUPTOR) {
                activado = !activado
                alCambiar?.invoke(activado)
            }
            alTocar?.invoke()
        }
    }

    override fun setOnClickListener(l: OnClickListener?) {
        alTocar = l?.let { { it.onClick(this) } }
    }

    fun ponerIcono(@DrawableRes icono: Int) {
        vista.marcoIcono.visibility = View.VISIBLE
        vista.imgIcono.setImageResource(icono)
    }

    var titulo: CharSequence?
        get() = vista.tvTitulo.text
        set(valor) { vista.tvTitulo.text = valor; nombrar() }

    var subtitulo: CharSequence?
        get() = vista.tvSubtitulo.text
        set(valor) {
            vista.tvSubtitulo.text = valor
            vista.tvSubtitulo.visibility = if (valor.isNullOrBlank()) View.GONE else View.VISIBLE
            nombrar()
        }

    /** Texto a la derecha en las filas de estado ("Permitidas", "No configurada"). */
    var estado: CharSequence?
        get() = vista.tvEstado.text
        set(valor) { vista.tvEstado.text = valor; nombrar() }

    // Nombre propio para TalkBack ("Legal, Términos, privacidad y licencias"): con letra grande
    // la fila puede quedar cortada bajo la barra inferior y sus textos no la nombrarían
    private fun nombrar() {
        contentDescription = listOfNotNull(vista.tvTitulo.text, vista.tvSubtitulo.text, vista.tvEstado.text)
            .filter { it.isNotBlank() }.joinToString(", ")
    }

    fun ponerTipo(nuevo: Tipo) {
        tipo = nuevo
        vista.imgFlecha.visibility = if (nuevo == Tipo.ENLACE) View.VISIBLE else View.GONE
        vista.interruptor.visibility = if (nuevo == Tipo.INTERRUPTOR) View.VISIBLE else View.GONE
        vista.tvEstado.visibility = if (nuevo == Tipo.ESTADO) View.VISIBLE else View.GONE
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        alpha = if (enabled) 1f else 0.5f
        vista.interruptor.isEnabled = enabled
    }

    // TalkBack: una fila con interruptor se anuncia como interruptor con su estado
    override fun getAccessibilityClassName(): CharSequence =
        if (tipo == Tipo.INTERRUPTOR) Switch::class.java.name else android.widget.Button::class.java.name

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        if (tipo == Tipo.INTERRUPTOR) {
            info.isCheckable = true
            info.isChecked = activado
        }
    }
}
