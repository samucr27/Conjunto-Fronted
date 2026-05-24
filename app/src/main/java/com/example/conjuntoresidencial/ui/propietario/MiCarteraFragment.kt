package com.example.conjuntoresidencial.ui.propietario

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.Snackbar
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FragmentMiCarteraBinding
import com.example.conjuntoresidencial.util.Resource
import com.example.conjuntoresidencial.util.SessionManager

class MiCarteraFragment : Fragment() {

    private var _binding: FragmentMiCarteraBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MiCarteraViewModel by viewModels()
    private var aptoId: Long = -1L

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMiCarteraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        aptoId = SessionManager(requireContext()).getApartamentoId()
        viewModel.fetchPagos(aptoId)

        binding.btnSimularPago.setOnClickListener { viewModel.simularPago(aptoId) }

        viewModel.pagoState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> binding.progressCartera.visibility = View.VISIBLE
                is Resource.Success -> {
                    binding.progressCartera.visibility = View.GONE
                    val pago = resource.data
                    if (pago != null) {
                        binding.tvNoRegistrosCartera.visibility = View.GONE
                        binding.layoutInfoCartera.visibility = View.VISIBLE
                        binding.tvSaldoPendiente.text = getString(R.string.saldo_pendiente_format, pago.saldoPendiente.toString())
                        binding.tvEstadoCuenta.text = pago.estadoCuenta
                        binding.tvObservacionesCartera.text = pago.observaciones
                        if (pago.saldoPendiente > 0) {
                            binding.btnSimularPago.isEnabled = true
                            binding.btnSimularPago.text = getString(R.string.btn_simular_pago)
                            binding.btnSimularPago.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green_pay))
                        } else {
                            binding.btnSimularPago.isEnabled = false
                            binding.btnSimularPago.text = getString(R.string.btn_sin_deuda)
                            binding.btnSimularPago.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.gray_disabled))
                        }
                    } else {
                        binding.tvNoRegistrosCartera.visibility = View.VISIBLE
                        binding.layoutInfoCartera.visibility = View.GONE
                    }
                }
                is Resource.Error -> {
                    binding.progressCartera.visibility = View.GONE
                    Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }

        viewModel.simulacionState.observe(viewLifecycleOwner) { resource ->
            if (resource is Resource.Success) {
                Snackbar.make(binding.root, getString(R.string.simulacion_exitosa), Snackbar.LENGTH_SHORT).show()
                viewModel.fetchPagos(aptoId)
            } else if (resource is Resource.Error) {
                Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}