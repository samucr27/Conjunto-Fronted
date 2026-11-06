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
import com.example.conjuntoresidencial.ui.comun.FiltroTorreApto
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
    private lateinit var filtro: FiltroTorreApto

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDirectorioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(binding.encabezado, R.string.directorio_titulo, R.string.directorio_descripcion, atras = false)
        filtro = FiltroTorreApto(binding.filtro) { _, _ -> pintar() }
        filtro.ponerPista(R.string.directorio_buscar_apto)
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
                    filtro.ponerTorres(todos.map { it.torre })
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

    /**
     * P10: sin torre ni texto se ven las torres (tocar una la elige); con torre, sus apartamentos;
     * el texto busca apartamento, nombre o placa dentro de la torre elegida (o en todas).
     */
    private fun pintar() {
        val q = filtro.texto
        val torre = filtro.torre
        binding.lista.removeAllViews()
        if (torre == null && q.isEmpty()) {
            binding.tvVacio.isVisible = false
            todos.groupBy { it.torre }.toSortedMap(compareBy<String>({ it.toIntOrNull() ?: Int.MAX_VALUE }, { it }))
                .forEach { (t, aptos) -> binding.lista.addView(filaTorre(t, aptos.size)) }
            return
        }
        val lista = todos.filter { (torre == null || it.torre == torre) && (q.isEmpty() || coincide(it, q)) }
        binding.tvVacio.isVisible = lista.isEmpty() && todos.isNotEmpty()
        // Muchos apartamentos: se muestran los primeros 100 y la búsqueda acota el resto
        lista.take(100).forEach { a -> binding.lista.addView(fila(a)) }
    }

    private fun filaTorre(torre: String, aptos: Int): View {
        val fila = ItemTextoConAccionBinding.inflate(layoutInflater, binding.lista, false)
        fila.tvTitulo.text = getString(R.string.filtro_torre_n, torre)
        fila.tvDetalle.text = resources.getQuantityString(R.plurals.directorio_aptos_torre, aptos, aptos)
        fila.root.setOnClickListener { filtro.elegirTorre(torre) }
        return fila.root
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
        // P10: los vehículos sin placa (bicicletas) llegan como null y no se muestran
        val placas = (a.placas as List<String?>).filterNot { it.isNullOrBlank() }.map { it!! }
        if (placas.isNotEmpty()) lineas += getString(R.string.directorio_placas, placas.joinToString(", "))
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
        if (FiltroTorreApto.coincideApto(a.apto, q)) return true
        val sinEspacios = q.replace(" ", "")
        return normal("${a.torre}-${a.apto}").contains(q) || normal(a.apto) == q ||
            normal("torre ${a.torre} apto ${a.apto}").contains(q) ||
            (a.placas as List<String?>).any { it != null && normal(it).contains(sinEspacios) } ||
            a.residentes.any { normal(it.nombre).contains(q) }
    }

    private fun normal(s: String) = Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase(Locale.ROOT)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
