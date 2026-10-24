package com.example.conjuntoresidencial.ui.cuenta

import android.os.Bundle
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FragmentDocumentoBinding
import com.example.conjuntoresidencial.ui.comun.aparienciaDelTema
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado

/**
 * Un documento largo (términos, privacidad, licencias) desde res/raw. Formato sencillo:
 * "# " al inicio de línea = título de sección, "- " = viñeta, línea vacía = nuevo párrafo.
 */
class DocumentoFragment : Fragment() {
    private var _binding: FragmentDocumentoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDocumentoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val argumentos = requireArguments()
        configurarEncabezado(binding.encabezado, argumentos.getInt(ARG_TITULO))
        val texto = resources.openRawResource(argumentos.getInt(ARG_TEXTO)).bufferedReader().use { it.readText() }
        mostrar(texto)
    }

    private fun mostrar(texto: String) {
        val margen = (resources.displayMetrics.density * 8).toInt()
        texto.lines().map(String::trimEnd).filter(String::isNotEmpty).forEach { linea ->
            val vista = TextView(requireContext())
            when {
                linea.startsWith("# ") -> {
                    vista.aparienciaDelTema(com.google.android.material.R.attr.textAppearanceTitleMedium, com.google.android.material.R.attr.colorOnSurface)
                    vista.text = linea.removePrefix("# ")
                    ViewCompat.setAccessibilityHeading(vista, true)
                    vista.setPadding(0, margen * 2, 0, 0)
                }
                linea.startsWith("- ") -> {
                    estiloCuerpo(vista)
                    vista.text = getString(R.string.vineta, linea.removePrefix("- "))
                    vista.setPadding(margen, margen / 2, 0, 0)
                }
                else -> {
                    estiloCuerpo(vista)
                    vista.text = linea
                    vista.setPadding(0, margen, 0, 0)
                }
            }
            Linkify.addLinks(vista, Linkify.WEB_URLS)
            binding.contenido.addView(vista)
        }
    }

    private fun estiloCuerpo(vista: TextView) {
        vista.aparienciaDelTema(com.google.android.material.R.attr.textAppearanceBodyLarge, com.google.android.material.R.attr.colorOnSurfaceVariant)
        vista.setLineSpacing(0f, 1.15f)
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    companion object {
        const val ARG_TITULO = "titulo"
        const val ARG_TEXTO = "texto"
    }
}
