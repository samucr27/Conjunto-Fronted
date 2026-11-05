package com.example.conjuntoresidencial.ui.auth

import android.graphics.drawable.Animatable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FragmentBienvenidaBinding
import com.example.conjuntoresidencial.databinding.ItemBienvenidaPuntoBinding
import com.example.conjuntoresidencial.ui.comun.controlesFlotantes
import com.example.conjuntoresidencial.util.PrimerosPasos
import com.example.conjuntoresidencial.util.seguirPrimerosPasos

/**
 * P8 bloque 2: lo primero que se ve al abrir la app por primera vez. Cada app pone su título,
 * su descripción y sus tres puntos (bienvenida_* en los strings de su flavor). "Comenzar" sigue
 * con el conjunto, el rol (residentes) y el login.
 */
class BienvenidaFragment : Fragment() {

    private var _binding: FragmentBienvenidaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBienvenidaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // El contenido empieza detrás de la barra de estado (MainActivity); abajo, el botón queda
        // nítido (sin el difuminado de las listas) y la franja de la barra de navegación del
        // sistema se pinta del mismo color de la pantalla
        binding.contenido.updatePadding(top = controlesFlotantes?.margenSuperior ?: 0)
        controlesFlotantes?.sinDifuminadoAbajo()
        requireActivity().window.decorView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.ic_launcher_background))

        val textos = resources.getStringArray(R.array.bienvenida_puntos)
        val iconos = resources.obtainTypedArray(R.array.bienvenida_iconos)
        try {
            textos.forEachIndexed { i, texto ->
                val punto = ItemBienvenidaPuntoBinding.inflate(layoutInflater, binding.listaPuntos, true)
                punto.tvTexto.text = texto
                punto.imgIcono.setImageResource(iconos.getResourceId(i, R.drawable.ic_check_circle))
            }
        } finally {
            iconos.recycle()
        }

        (binding.imgLogo.drawable as? Animatable)?.start()
        binding.btnComenzar.setOnClickListener {
            PrimerosPasos.marcarBienvenida(requireContext())
            seguirPrimerosPasos()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // El fondo de la ventana vuelve al del tema
        val fondo = android.util.TypedValue()
        requireActivity().theme.resolveAttribute(android.R.attr.windowBackground, fondo, true)
        requireActivity().window.setBackgroundDrawableResource(fondo.resourceId)
        _binding = null
    }
}
