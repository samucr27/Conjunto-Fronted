package com.example.conjuntoresidencial.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FragmentElegirRolBinding
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.util.PrimerosPasos
import com.example.conjuntoresidencial.util.seguirPrimerosPasos

/**
 * P8 bloque 2: propietario o arrendatario, después de elegir el conjunto (y desde "Cambiar rol"
 * en el ⋮ del login). El login entra con este rol; el elegido sale marcado.
 */
class ElegirRolFragment : Fragment() {

    private var _binding: FragmentElegirRolBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentElegirRolBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(binding.encabezado, R.string.rol_titulo, R.string.rol_descripcion,
            atras = findNavController().previousBackStackEntry != null)
        val actual = PrimerosPasos.rol(requireContext())
        binding.tarjetaPropietario.isChecked = actual == PrimerosPasos.PROPIETARIO
        binding.tarjetaArrendatario.isChecked = actual == PrimerosPasos.ARRENDATARIO
        binding.tarjetaPropietario.setOnClickListener { elegir(PrimerosPasos.PROPIETARIO) }
        binding.tarjetaArrendatario.setOnClickListener { elegir(PrimerosPasos.ARRENDATARIO) }
    }

    private fun elegir(rol: String) {
        PrimerosPasos.guardarRol(requireContext(), rol)
        seguirPrimerosPasos()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
