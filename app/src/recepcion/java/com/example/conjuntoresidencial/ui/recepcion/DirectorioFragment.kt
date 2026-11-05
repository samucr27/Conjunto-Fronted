package com.example.conjuntoresidencial.ui.recepcion

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.api.instance
import com.example.conjuntoresidencial.databinding.FragmentDirectorioBinding
import com.example.conjuntoresidencial.databinding.ItemTextoConAccionBinding
import com.example.conjuntoresidencial.model.ApartamentoDirectorio
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.util.Locale

/**
 * P8 bloque 5: los apartamentos del conjunto con quién vive (propietario o arrendatario), sus
 * vehículos aprobados y el citófono. Se carga una vez y se filtra mientras se escribe; "Llamar"
 * abre el marcador con el teléfono del residente (si hay varios, pregunta a cuál).
 */
class DirectorioFragment : Fragment() {

    private var _binding: FragmentDirectorioBinding? = null
    private val binding get() = _binding!!
    private var todos: List<ApartamentoDirectorio> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDirectorioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(binding.encabezado, R.string.directorio_titulo, R.string.directorio_descripcion, atras = false)
        binding.etBuscar.doAfterTextChanged { pintar() }
        cargar()
    }

    private fun cargar() {
        binding.progreso.isVisible = true
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.directorio(null)
                if (_binding == null) return@launch
                if (r.isSuccessful) {
                    todos = r.body().orEmpty()
                    pintar()
                } else {
                    aviso(RetrofitClient.mensajeDeError(r, getString(R.string.porteria_error))).show()
                }
            } catch (e: Exception) {
                if (_binding != null) aviso(RetrofitClient.mensajeDeExcepcion(e)).show()
            }
            _binding?.progreso?.isVisible = false
        }
    }

    private fun pintar() {
        val q = normal(binding.etBuscar.text?.toString().orEmpty().trim())
        val lista = if (q.isEmpty()) todos else todos.filter { coincide(it, q) }
        binding.tvVacio.isVisible = lista.isEmpty() && todos.isNotEmpty()
        binding.lista.removeAllViews()
        // Muchos apartamentos: se muestran los primeros 100 y la búsqueda acota el resto
        lista.take(100).forEach { a -> binding.lista.addView(fila(a)) }
    }

    private fun fila(a: ApartamentoDirectorio): View {
        val fila = ItemTextoConAccionBinding.inflate(layoutInflater, binding.lista, false)
        fila.tvTitulo.text = getString(R.string.directorio_apto, a.torre, a.apto)
        if (a.ocupacion == "DESOCUPADO") {
            fila.tvEtiqueta.isVisible = true
            fila.tvEtiqueta.setText(R.string.directorio_desocupado)
        }
        val lineas = mutableListOf<String>()
        if (a.residentes.isEmpty()) lineas += getString(R.string.directorio_sin_residentes)
        a.residentes.forEach { r ->
            val tipo = getString(if (r.tipo == "ARRENDATARIO") R.string.directorio_arrendatario else R.string.directorio_propietario)
            lineas += getString(R.string.directorio_residente, r.nombre, tipo)
        }
        if (a.placas.isNotEmpty()) lineas += getString(R.string.directorio_placas, a.placas.joinToString(", "))
        listOfNotNull(a.citofono1, a.citofono2).takeIf { it.isNotEmpty() }
            ?.let { lineas += getString(R.string.directorio_citofono, it.joinToString(" / ")) }
        fila.tvDetalle.text = lineas.joinToString("\n")

        val telefonos = a.residentes.filter { !it.telefono.isNullOrBlank() }
        fila.btnAccion.isVisible = telefonos.isNotEmpty()
        fila.btnAccion.setText(R.string.directorio_llamar)
        fila.btnAccion.setOnClickListener {
            if (telefonos.size == 1) {
                llamar(telefonos[0].telefono!!)
            } else {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.directorio_llamar_a)
                    .setItems(telefonos.map { "${it.nombre} · ${it.telefono}" }.toTypedArray()) { _, i -> llamar(telefonos[i].telefono!!) }
                    .setNegativeButton(R.string.btn_cancelar, null)
                    .show()
            }
        }
        return fila.root
    }

    private fun coincide(a: ApartamentoDirectorio, q: String): Boolean {
        val sinEspacios = q.replace(" ", "")
        return normal("${a.torre}-${a.apto}").contains(q) || normal(a.apto) == q ||
            normal("torre ${a.torre} apto ${a.apto}").contains(q) ||
            a.placas.any { normal(it).contains(sinEspacios) } ||
            a.residentes.any { normal(it.nombre).contains(q) }
    }

    private fun normal(s: String) = Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase(Locale.ROOT)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
