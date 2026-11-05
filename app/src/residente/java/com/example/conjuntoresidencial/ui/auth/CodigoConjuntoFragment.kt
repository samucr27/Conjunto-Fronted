package com.example.conjuntoresidencial.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.FragmentCodigoConjuntoBinding
import com.example.conjuntoresidencial.model.CodigoConjuntoRequest
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.util.ConjuntoElegido
import com.example.conjuntoresidencial.util.seguirPrimerosPasos
import kotlinx.coroutines.launch

/**
 * P9 bloque 2: el código de 6 dígitos del conjunto, que se pide en persona en la portería o la
 * administración y cambia cada día. Con el correcto el servidor da un pase que este celular
 * guarda (ConjuntoElegido) y que el login y el registro exigen; no se vuelve a pedir hasta
 * cambiar de conjunto o reinstalar la app.
 */
class CodigoConjuntoFragment : Fragment() {

    private var _binding: FragmentCodigoConjuntoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCodigoConjuntoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(binding.encabezado, R.string.codigo_titulo, R.string.codigo_descripcion,
            atras = findNavController().previousBackStackEntry != null)
        ConjuntoElegido.actual(requireContext())?.let { c ->
            binding.encabezado.ponerInfo(if (c.municipio.isBlank()) c.nombre else getString(R.string.conjunto_actual, c.nombre, c.municipio))
        }
        binding.etCodigo.doAfterTextChanged {
            binding.tilCodigo.error = null
            // Con el sexto dígito se revisa solo
            if (it?.length == 6) enviar()
        }
        binding.etCodigo.setOnEditorActionListener { _, accion, _ -> (accion == EditorInfo.IME_ACTION_DONE).also { if (it) enviar() } }
        binding.btnContinuar.setOnClickListener { enviar() }
        binding.btnOtroConjunto.setOnClickListener { findNavController().navigate(R.id.elegirConjuntoFragment) }
    }

    private fun enviar() {
        val codigo = binding.etCodigo.text?.toString()?.filter { it.isDigit() }.orEmpty()
        if (codigo.length != 6) {
            binding.tilCodigo.error = getString(R.string.codigo_falta)
            return
        }
        val conjunto = ConjuntoElegido.id(requireContext()) ?: run { findNavController().navigate(R.id.elegirConjuntoFragment); return }
        if (!binding.btnContinuar.isEnabled) return
        binding.btnContinuar.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val r = RetrofitClient.comun.verificarCodigoConjunto(conjunto, CodigoConjuntoRequest(codigo))
                val pase = r.body()
                if (_binding == null) return@launch
                if (r.isSuccessful && pase != null) {
                    ConjuntoElegido.guardarPase(requireContext(), pase.conjuntoId, pase.pase)
                    aviso(R.string.codigo_listo).show()
                    seguirPrimerosPasos()
                    return@launch
                }
                // Primero se borra (eso quita el error en doAfterTextChanged) y después se muestra
                binding.etCodigo.setText("")
                binding.tilCodigo.error = RetrofitClient.mensajeDeError(r, getString(R.string.codigo_error))
            } catch (e: Exception) {
                if (_binding != null) binding.tilCodigo.error = RetrofitClient.mensajeDeExcepcion(e)
            }
            _binding?.btnContinuar?.isEnabled = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
