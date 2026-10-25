package com.example.conjuntoresidencial.ui.cuenta

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FragmentLegalBinding
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado

/** Cuenta → Legal: cada fila abre el documento (DocumentoFragment) con su texto de res/raw. */
class LegalFragment : Fragment() {
    private var _binding: FragmentLegalBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLegalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(binding.encabezado, R.string.cuenta_legal, R.string.legal_descripcion)
        binding.filaTerminos.setOnClickListener { abrir(R.string.legal_terminos, R.raw.terminos) }
        binding.filaPrivacidad.setOnClickListener { abrir(R.string.legal_privacidad, R.raw.privacidad) }
        binding.filaLicencias.setOnClickListener { abrir(R.string.legal_licencias, R.raw.licencias) }
    }

    private fun abrir(@StringRes titulo: Int, @RawRes texto: Int) {
        findNavController().navigate(
            R.id.documentoFragment,
            bundleOf(DocumentoFragment.ARG_TITULO to titulo, DocumentoFragment.ARG_TEXTO to texto)
        )
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
