package com.example.conjuntoresidencial.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.FragmentCarteraAdminBinding
import com.example.conjuntoresidencial.model.Apartamento
import com.example.conjuntoresidencial.model.PagoDTO
import kotlinx.coroutines.launch

class CarteraAdminFragment : Fragment() {

    private var _binding: FragmentCarteraAdminBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCarteraAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvCarteraAdmin.layoutManager = LinearLayoutManager(requireContext())

        obtenerDatosCartera()
    }

    private fun obtenerDatosCartera() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // CORRECCIÓN 1: Obtener la instancia del servicio usando el generador de la interfaz de tu proyecto
                val apiService = RetrofitClient.instance

                // Realizamos la petición HTTP inicial
                val respuestaAptos = apiService.getApartamentos()

                if (respuestaAptos.isSuccessful) {
                    // CORRECCIÓN 2: Asegurar la extracción correcta y segura de la lista del cuerpo de la respuesta
                    val listaApartamentos = respuestaAptos.body() ?: emptyList()
                    val listaCompletaCartera = mutableListOf<Pair<Apartamento, PagoDTO?>>()

                    // Iteración explícita sobre la colección limpia de datos devuelta por el servidor
                    for (apto in listaApartamentos) {
                        val respuestaPago = apiService.getPagosPorApartamento(apto.id)
                        val pago = if (respuestaPago.isSuccessful) respuestaPago.body() else null
                        listaCompletaCartera.add(Pair(apto, pago))
                    }

                    // Inicialización del adaptador con la lista emparejada
                    val adapter = CarteraAdminAdapter(listaCompletaCartera)
                    binding.rvCarteraAdmin.adapter = adapter
                } else {
                    Toast.makeText(requireContext(), "Error del servidor al obtener apartamentos", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Fallo de red o estructura: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}