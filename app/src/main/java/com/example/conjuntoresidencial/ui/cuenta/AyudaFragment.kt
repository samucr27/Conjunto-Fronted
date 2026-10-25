package com.example.conjuntoresidencial.ui.cuenta

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FragmentAyudaBinding
import com.example.conjuntoresidencial.ui.comun.aparienciaDelTema
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.google.android.material.card.MaterialCardView

/** Cuenta → Ayuda: preguntas frecuentes (strings.xml: ayuda_preguntas y ayuda_respuestas). */
class AyudaFragment : Fragment() {
    private var _binding: FragmentAyudaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAyudaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(binding.encabezado, R.string.cuenta_ayuda, R.string.ayuda_descripcion)
        val preguntas = resources.getStringArray(R.array.ayuda_preguntas)
        val respuestas = resources.getStringArray(R.array.ayuda_respuestas)
        val dp = resources.displayMetrics.density
        preguntas.zip(respuestas).forEachIndexed { i, (pregunta, respuesta) ->
            val tarjeta = MaterialCardView(requireContext(), null, com.google.android.material.R.attr.materialCardViewFilledStyle)
            tarjeta.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { if (i > 0) topMargin = (12 * dp).toInt() }
            val columna = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                val relleno = (16 * dp).toInt()
                setPadding(relleno, relleno, relleno, relleno)
            }
            columna.addView(TextView(requireContext()).apply {
                aparienciaDelTema(com.google.android.material.R.attr.textAppearanceTitleMedium, com.google.android.material.R.attr.colorOnSurface)
                text = pregunta
                ViewCompat.setAccessibilityHeading(this, true)
            })
            columna.addView(TextView(requireContext()).apply {
                aparienciaDelTema(com.google.android.material.R.attr.textAppearanceBodyMedium, com.google.android.material.R.attr.colorOnSurfaceVariant)
                text = respuesta
                setPadding(0, (6 * dp).toInt(), 0, 0)
            })
            tarjeta.addView(columna)
            binding.contenido.addView(tarjeta)
        }
        binding.contenido.addView(TextView(requireContext()).apply {
            aparienciaDelTema(com.google.android.material.R.attr.textAppearanceBodyMedium, com.google.android.material.R.attr.colorOnSurfaceVariant)
            setText(R.string.ayuda_contacto)
            setPadding((16 * dp).toInt(), (20 * dp).toInt(), (16 * dp).toInt(), 0)
        })
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
