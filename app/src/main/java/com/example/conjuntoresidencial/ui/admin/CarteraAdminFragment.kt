package com.example.conjuntoresidencial.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FragmentCarteraAdminBinding
import com.example.conjuntoresidencial.util.Resource
import com.example.conjuntoresidencial.ui.comun.accionesDeLista
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.ui.comun.resumen

class CarteraAdminFragment : Fragment() {

    private var _binding: FragmentCarteraAdminBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CarteraAdminViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCarteraAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(
            binding.encabezado, R.string.cartera_titulo, R.string.cartera_subtitulo, menu = R.menu.menu_lista,
            alElegir = accionesDeLista(binding.swipeCartera) { viewModel.fetchCartera() }
        )

        val adaptador = CarteraAdminAdapter()
        binding.rvCarteraAdmin.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCarteraAdmin.adapter = adaptador
        binding.estado.vincular(binding.rvCarteraAdmin)
        binding.swipeCartera.setOnRefreshListener { viewModel.fetchCartera() }

        viewModel.cartera.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading ->
                    // Con datos en pantalla, el gesto de recargar ya muestra su propio indicador
                    if (!binding.swipeCartera.isRefreshing) binding.estado.mostrarCargando()
                is Resource.Success -> {
                    binding.swipeCartera.isRefreshing = false
                    val lista = resource.data.orEmpty()
                    adaptador.submitList(lista)
                    binding.encabezado.ponerInfo(resumen(
                        R.plurals.resumen_apartamentos, lista.size,
                        R.plurals.resumen_en_mora to lista.count { (it.second?.saldoPendiente ?: 0.0) > 0 }
                    ))
                    if (lista.isEmpty()) {
                        binding.estado.mostrarVacio(R.drawable.ic_wallet, getString(R.string.cartera_vacia))
                    } else {
                        binding.estado.mostrarContenido()
                    }
                }
                is Resource.Error -> {
                    binding.swipeCartera.isRefreshing = false
                    binding.estado.mostrarError(resource.message) { viewModel.fetchCartera() }
                }
            }
        }

        // Al girar la pantalla se reutiliza lo ya cargado
        if (viewModel.cartera.value == null) viewModel.fetchCartera()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
