package com.example.conjuntoresidencial.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FragmentElegirConjuntoBinding
import com.example.conjuntoresidencial.model.ConjuntoDTO
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.util.seguirPrimerosPasos

/**
 * P8 bloque 2: elegir el conjunto en pantalla completa. Se abre sola la primera vez (después de
 * la bienvenida) y desde "Cambiar conjunto" en el ⋮ del login. Al continuar sigue con el rol
 * (residentes) o vuelve al login.
 */
class ElegirConjuntoFragment : Fragment() {

    private var _binding: FragmentElegirConjuntoBinding? = null
    private val binding get() = _binding!!
    private var elegido: ConjuntoDTO? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentElegirConjuntoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(binding.encabezado, R.string.elegir_conjunto_titulo, R.string.elegir_conjunto_descripcion,
            atras = findNavController().previousBackStackEntry != null)
        controlarSelectorConjunto(binding.selector, null, activo = { _binding != null }) {
            elegido = it
            binding.btnContinuar.isEnabled = it != null
        }
        binding.btnContinuar.setOnClickListener {
            val nuevo = elegido ?: return@setOnClickListener
            cambiarConjunto(requireContext(), nuevo)
            seguirPrimerosPasos()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
