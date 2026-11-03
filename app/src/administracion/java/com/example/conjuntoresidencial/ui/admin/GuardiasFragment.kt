package com.example.conjuntoresidencial.ui.admin

import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.api.instance
import com.example.conjuntoresidencial.databinding.FragmentGuardiasBinding
import com.example.conjuntoresidencial.model.DatosGuardia
import com.example.conjuntoresidencial.model.GuardiaDTO
import com.example.conjuntoresidencial.ui.comun.DiffPorId
import com.example.conjuntoresidencial.ui.comun.accionesDeLista
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.controlesFlotantes
import com.example.conjuntoresidencial.ui.comun.elegirOpcion
import com.example.conjuntoresidencial.ui.comun.ejecutarAccion
import com.example.conjuntoresidencial.ui.comun.mostrarRecurso
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.util.Resource
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

/**
 * P7 bloque 4: guardias de la recepción. El celular de la recepción entra con la cuenta de
 * portería (Mi equipo) y cada guardia abre su turno con su PIN: así cada visita dice quién la
 * registró. Aquí se crean, se les cambia el PIN y se dan de baja (su historial se conserva).
 */
class GuardiasFragment : Fragment() {

    private var _binding: FragmentGuardiasBinding? = null
    private val binding get() = _binding!!
    private val adaptador = GuardiaAdapter { opciones(it) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGuardiasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(binding.encabezado, R.string.menu_guardias, R.string.guardias_descripcion,
            menu = R.menu.menu_lista, alElegir = accionesDeLista(binding.swipe) { cargar() })
        binding.lista.layoutManager = LinearLayoutManager(requireContext())
        binding.lista.adapter = adaptador
        binding.estado.vincular(binding.lista)
        binding.swipe.setOnRefreshListener { cargar() }
        controlesFlotantes?.mostrarBotonFlotante(R.string.guardias_agregar, R.drawable.ic_add) { formulario(null) }
        cargar()
    }

    private fun cargar() {
        viewLifecycleOwner.lifecycleScope.launch {
            val resource = try {
                val r = RetrofitClient.instance.guardias()
                if (r.isSuccessful) Resource.Success(r.body().orEmpty())
                else Resource.Error(RetrofitClient.mensajeDeError(r, getString(R.string.guardias_error_cargar)))
            } catch (e: Exception) {
                Resource.Error(RetrofitClient.mensajeDeExcepcion(e))
            }
            if (_binding == null) return@launch
            binding.estado.mostrarRecurso(resource, binding.swipe, R.drawable.ic_key, getString(R.string.guardias_vacio),
                getString(R.string.guardias_vacio_mensaje), reintentar = { cargar() }) { lista ->
                adaptador.submitList(lista)
                binding.encabezado.ponerInfo(resources.getQuantityString(R.plurals.resumen_guardias,
                    lista.count { it.activo }, lista.count { it.activo }))
            }
        }
    }

    private fun opciones(g: GuardiaDTO) {
        elegirOpcion(g.nombre, listOf(
            R.string.guardias_editar to { formulario(g) },
            R.string.guardias_cambiar_pin to { cambiarPin(g) },
            (if (g.activo) R.string.guardias_dar_de_baja else R.string.guardias_reactivar) to {
                ejecutarAccion(getString(if (g.activo) R.string.guardias_dado_de_baja else R.string.guardias_reactivado), {
                    RetrofitClient.instance.editarGuardia(g.id, DatosGuardia(g.nombre, g.documento, activo = !g.activo))
                }) { cargar() }
            }
        ))
    }

    /** Nuevo guardia ([editando] null: también el PIN) o editar su nombre y documento. */
    private fun formulario(editando: GuardiaDTO?) {
        val contexto = requireContext()
        val nombre = campo(R.string.guardias_nombre, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS, 150)
        val documento = campo(R.string.guardias_documento, InputType.TYPE_CLASS_TEXT, 30)
        val pin = campo(R.string.guardias_pin, InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD, 6)
        editando?.let {
            nombre.editText?.setText(it.nombre)
            documento.editText?.setText(it.documento)
        }
        val campos = if (editando == null) listOf(nombre, documento, pin) else listOf(nombre, documento)
        val dialogo = MaterialAlertDialogBuilder(contexto)
            .setTitle(if (editando == null) R.string.guardias_agregar else R.string.guardias_editar)
            .setView(vista(campos))
            .setPositiveButton(R.string.btn_guardar, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            fun texto(t: TextInputLayout) = t.editText?.text?.toString()?.trim().orEmpty()
            val obligatorio = getString(R.string.equipo_dato_obligatorio)
            nombre.error = if (texto(nombre).isEmpty()) obligatorio else null
            documento.error = if (texto(documento).isEmpty()) obligatorio else null
            if (editando == null) pin.error = errorPin(texto(pin))
            if (campos.any { it.error != null }) return@setOnClickListener
            guardar(dialogo) {
                if (editando == null) RetrofitClient.instance.crearGuardia(DatosGuardia(texto(nombre), texto(documento), texto(pin)))
                else RetrofitClient.instance.editarGuardia(editando.id, DatosGuardia(texto(nombre), texto(documento)))
            }
        }
    }

    private fun cambiarPin(g: GuardiaDTO) {
        val pin = campo(R.string.guardias_pin_nuevo, InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD, 6)
        val dialogo = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.guardias_cambiar_pin_de, g.nombre))
            .setView(vista(listOf(pin)))
            .setPositiveButton(R.string.btn_guardar, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val nuevo = pin.editText?.text?.toString()?.trim().orEmpty()
            pin.error = errorPin(nuevo)
            if (pin.error != null) return@setOnClickListener
            guardar(dialogo) { RetrofitClient.instance.editarGuardia(g.id, DatosGuardia(g.nombre, g.documento, nuevo)) }
        }
    }

    /** Lo que también revisa el servidor: de 4 a 6 dígitos (él además rechaza 1111 o 1234). */
    private fun errorPin(pin: String): String? = if (!pin.matches(Regex("\\d{4,6}"))) getString(R.string.guardias_pin_invalido) else null

    private fun guardar(dialogo: AlertDialog, peticion: suspend () -> retrofit2.Response<GuardiaDTO>) {
        val boton = dialogo.getButton(AlertDialog.BUTTON_POSITIVE)
        boton.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            val error = try {
                val r = peticion()
                if (r.isSuccessful) null else RetrofitClient.mensajeDeError(r, getString(R.string.guardias_error_guardar))
            } catch (e: Exception) {
                RetrofitClient.mensajeDeExcepcion(e)
            }
            boton.isEnabled = true
            if (view == null) return@launch
            if (error == null) {
                dialogo.dismiss()
                aviso(R.string.guardias_guardado).show()
                cargar()
            } else {
                // Sobre el diálogo: el aviso de la pantalla quedaría detrás del fondo oscuro
                dialogo.window?.decorView?.let { Snackbar.make(it, error, Snackbar.LENGTH_LONG).show() } ?: aviso(error).show()
            }
        }
    }

    private fun campo(hint: Int, tipo: Int, largo: Int) = TextInputLayout(requireContext()).apply {
        this.hint = getString(hint)
        if (tipo and InputType.TYPE_NUMBER_VARIATION_PASSWORD != 0) endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
        addView(TextInputEditText(context).apply {
            inputType = tipo
            filters = arrayOf(InputFilter.LengthFilter(largo))
        })
    }

    private fun vista(campos: List<TextInputLayout>): View {
        val margen = (24 * resources.displayMetrics.density).toInt()
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(margen, margen / 2, margen, 0)
            campos.forEach { addView(it) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class GuardiaAdapter(private val onTocar: (GuardiaDTO) -> Unit) :
        ListAdapter<GuardiaDTO, GuardiaAdapter.VH>(DiffPorId { it.id }) {
        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val nombre: TextView = v.findViewById(android.R.id.text1)
            val detalle: TextView = v.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false))

        override fun onBindViewHolder(h: VH, position: Int) {
            val g = getItem(position)
            val contexto = h.itemView.context
            h.nombre.text = g.nombre
            h.detalle.text = if (g.activo) contexto.getString(R.string.guardias_fila, g.documento)
                else contexto.getString(R.string.guardias_fila_baja, g.documento)
            h.itemView.alpha = if (g.activo) 1f else 0.6f
            h.itemView.setOnClickListener { onTocar(g) }
        }
    }
}
