package com.example.conjuntoresidencial.ui.comun

import android.view.View
import androidx.core.widget.doAfterTextChanged
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FiltroTorreAptoBinding
import com.google.android.material.chip.Chip
import java.text.Normalizer
import java.util.Locale

/**
 * P10: filtro de la portería en dos pasos. Primero se toca la torre ("Todas" o una) y después se
 * escribe el apartamento (o un nombre o una placa, según la pantalla). [alCambiar] recibe la torre
 * elegida (null = todas) y el texto ya normalizado (sin tildes, en minúsculas).
 */
class FiltroTorreApto(private val b: FiltroTorreAptoBinding, private val alCambiar: (torre: String?, texto: String) -> Unit) {

    var torre: String? = null
        private set
    val texto: String get() = normalizar(b.etApto.text?.toString().orEmpty().trim())

    init {
        b.etApto.doAfterTextChanged { alCambiar(torre, texto) }
        b.chipsTorre.setOnCheckedStateChangeListener { grupo, ids ->
            torre = ids.firstOrNull()?.let { grupo.findViewById<Chip>(it)?.tag as? String }
            alCambiar(torre, texto)
        }
        ponerTorres(emptyList())
    }

    /** Las torres que hay (se ordenan como números si lo son); conserva la elegida si sigue. */
    fun ponerTorres(torres: Collection<String>) {
        val ordenadas = torres.distinct().sortedWith(compareBy<String>({ it.toIntOrNull() ?: Int.MAX_VALUE }, { it }))
        val actual = torre
        val contexto = b.root.context
        b.chipsTorre.setOnCheckedStateChangeListener(null)
        b.chipsTorre.removeAllViews()
        (listOf<String?>(null) + ordenadas).forEach { t ->
            b.chipsTorre.addView(Chip(contexto).apply {
                id = View.generateViewId()
                tag = t
                text = t?.let { contexto.getString(R.string.filtro_torre_n, it) } ?: contexto.getString(R.string.filtro_todas)
                isCheckable = true
                isChecked = t == (actual?.takeIf { it in ordenadas })
            })
        }
        torre = actual?.takeIf { it in ordenadas }
        b.chipsTorre.setOnCheckedStateChangeListener { grupo, ids ->
            torre = ids.firstOrNull()?.let { grupo.findViewById<Chip>(it)?.tag as? String }
            alCambiar(torre, texto)
        }
    }

    /** Elige una torre desde fuera (por ejemplo, al tocar una torre de la lista). */
    fun elegirTorre(t: String?) {
        for (i in 0 until b.chipsTorre.childCount) {
            val chip = b.chipsTorre.getChildAt(i) as Chip
            if (chip.tag == t) { chip.isChecked = true; return }
        }
    }

    fun ponerPista(textoPista: Int) { b.tilApto.setHint(textoPista) }

    companion object {
        fun normalizar(s: String) = Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase(Locale.ROOT)

        /** El apartamento coincide si empieza por lo escrito ("30" encuentra 301 y 302). */
        fun coincideApto(apto: String?, texto: String) = texto.isEmpty() || normalizar(apto.orEmpty()).startsWith(texto)
    }
}
