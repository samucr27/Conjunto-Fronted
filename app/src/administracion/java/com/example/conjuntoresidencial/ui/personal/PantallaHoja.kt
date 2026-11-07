package com.example.conjuntoresidencial.ui.personal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.FragmentPantallaContenidoBinding
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import kotlinx.coroutines.launch

/**
 * P11 bloque 8: base de las pantallas de personal y presupuesto (layout/fragment_pantalla_contenido):
 * encabezado grande, jalar para actualizar, cargando / error con "Reintentar", y [pintar] arma la hoja.
 */
abstract class PantallaHoja : Fragment() {

    private var _binding: FragmentPantallaContenidoBinding? = null
    protected val binding get() = _binding!!
    protected val vivo: Boolean get() = _binding != null
    protected val hoja: LinearLayout get() = binding.contenido
    private var cargada = false

    protected abstract val titulo: Int
    protected abstract val descripcion: Int

    /** Trae los datos; devuelve un mensaje de error o null. */
    protected abstract suspend fun traer(): String?

    /** Arma la hoja con lo que trajo [traer]. */
    protected abstract fun pintar()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPantallaContenidoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(binding.encabezado, titulo, descripcion)
        binding.swipe.setOnRefreshListener { recargar() }
        binding.estado.vincular(binding.desplazar)
        recargar()
    }

    override fun onResume() {
        super.onResume()
        // Al volver de una ficha o un formulario, los datos pudieron cambiar
        if (cargada) recargar()
    }

    fun recargar() {
        if (!vivo) return
        if (!binding.swipe.isRefreshing && !cargada) binding.estado.mostrarCargando()
        viewLifecycleOwner.lifecycleScope.launch {
            val error = try { traer() } catch (e: Exception) { RetrofitClient.mensajeDeExcepcion(e) }
            if (!vivo) return@launch
            binding.swipe.isRefreshing = false
            if (error != null && !cargada) { binding.estado.mostrarError(error) { recargar() }; return@launch }
            cargada = true
            hoja.removeAllViews()
            pintar()
            binding.estado.mostrarContenido()
            if (error != null) aviso(error).show()
        }
    }

    protected fun mensajeDe(r: retrofit2.Response<*>): String? =
        if (r.isSuccessful) null else RetrofitClient.mensajeDeError(r, getString(R.string.pers_error))

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
