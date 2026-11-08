package com.example.conjuntoresidencial.ui.propietario

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.api.instance
import com.example.conjuntoresidencial.databinding.FragmentVisitantesActivosBinding
import com.example.conjuntoresidencial.databinding.ItemTextoConAccionBinding
import com.example.conjuntoresidencial.model.VisitaDTO
import com.example.conjuntoresidencial.ui.comun.TiempoDentro
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.confirmar
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import kotlinx.coroutines.launch

/**
 * P12: Herramientas → Visitantes activos. Quién sigue dentro del conjunto visitando el
 * apartamento y desde hace cuánto; si la portería olvidó registrar la salida, el residente la da
 * aquí (a la portería le llega el aviso).
 */
class VisitantesActivosFragment : Fragment() {

    private var _binding: FragmentVisitantesActivosBinding? = null
    private val binding get() = _binding!!
    private lateinit var reloj: TiempoDentro.Reloj

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentVisitantesActivosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(binding.encabezado, R.string.activos_titulo, R.string.activos_descripcion)
        reloj = TiempoDentro.Reloj(viewLifecycleOwner)
        binding.swipe.setOnRefreshListener { cargar() }
    }

    /** P12: al volver a la pantalla o a la app se recarga (la portería pudo entregar o registrar algo). */
    override fun onResume() {
        super.onResume()
        cargar()
    }

    private fun cargar() {
        binding.swipe.isRefreshing = true
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.visitasActivas()
                if (_binding == null) return@launch
                if (r.isSuccessful) pintar(r.body().orEmpty())
                else aviso(RetrofitClient.mensajeDeError(r, getString(R.string.activos_error))).show()
            } catch (e: Exception) {
                if (_binding != null) aviso(RetrofitClient.mensajeDeExcepcion(e)).show()
            }
            _binding?.swipe?.isRefreshing = false
        }
    }

    private fun pintar(visitas: List<VisitaDTO>) {
        binding.encabezado.ponerInfo(resources.getQuantityString(R.plurals.activos_resumen, visitas.size, visitas.size))
        binding.tvVacio.isVisible = visitas.isEmpty()
        binding.lista.removeAllViews()
        reloj.limpiar()
        visitas.forEach { v ->
            val fila = ItemTextoConAccionBinding.inflate(layoutInflater, binding.lista, false)
            fila.tvEtiqueta.isVisible = true
            fila.tvEtiqueta.text = if (v.tipoIngreso == "VEHICULO") {
                listOfNotNull(v.placa, v.parqueadero?.let { getString(R.string.activos_parqueadero, it) }).joinToString(" · ")
            } else getString(R.string.activos_a_pie)
            fila.tvTitulo.text = v.visitante
            val desde = v.dentroDesde ?: v.creadaEn
            reloj.agregar(fila.tvDetalle) {
                getString(R.string.activos_detalle, desde.substringAfter('T', "").take(5), TiempoDentro.texto(requireContext(), desde))
            }
            fila.btnAccion.isVisible = true
            fila.btnAccion.setText(R.string.activos_dar_salida)
            fila.btnAccion.setOnClickListener {
                confirmar(getString(R.string.activos_confirmar, v.visitante), getString(R.string.activos_confirmar_detalle),
                    R.string.activos_dar_salida) { darSalida(v) }
            }
            binding.lista.addView(fila.root)
        }
    }

    private fun darSalida(v: VisitaDTO) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.registrarSalidaVisita(v.id)
                if (_binding == null) return@launch
                aviso(if (r.isSuccessful) getString(R.string.activos_salida_ok, v.visitante)
                      else RetrofitClient.mensajeDeError(r, getString(R.string.activos_error))).show()
                cargar()
            } catch (e: Exception) {
                if (_binding != null) aviso(RetrofitClient.mensajeDeExcepcion(e)).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
