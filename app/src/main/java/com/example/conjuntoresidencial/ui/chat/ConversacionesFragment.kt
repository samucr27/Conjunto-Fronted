package com.example.conjuntoresidencial.ui.chat

import com.example.conjuntoresidencial.ui.comun.BarraBusqueda
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.FragmentEquipoBinding
import com.example.conjuntoresidencial.model.ConversacionDTO
import com.example.conjuntoresidencial.ui.comun.DiffPorId
import com.example.conjuntoresidencial.ui.comun.accionesDeLista
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.controlesFlotantes
import com.example.conjuntoresidencial.ui.comun.mostrarRecurso
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.util.Formatos
import com.example.conjuntoresidencial.util.Resource
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

/** Bandeja de mensajes de la administración (P5): las conversaciones con residentes, la más reciente primero. */
class ConversacionesFragment : Fragment() {
    // Misma estructura que Mi equipo: encabezado, lista y sus estados
    private var _binding: FragmentEquipoBinding? = null
    private val binding get() = _binding!!
    private var conversaciones: List<ConversacionDTO>? = null
    private lateinit var barra: BarraBusqueda
    private val adaptador = ConversacionAdapter { c ->
        findNavController().navigate(R.id.chatFragment, bundleOf(ChatFragment.ARG_CONVERSACION to c.id))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEquipoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(
            binding.encabezado, R.string.chat_mensajes, R.string.chat_conversaciones_subtitulo, menu = R.menu.menu_lista,
            alElegir = accionesDeLista(binding.swipe) { cargar() }
        )
        binding.rvEquipo.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEquipo.adapter = adaptador
        binding.estado.vincular(binding.rvEquipo)
        binding.swipe.setOnRefreshListener { cargar() }
        barra = BarraBusqueda(this, binding.busqueda, R.string.busqueda_conversaciones, listOf(
            R.string.busqueda_todos to null, R.string.busqueda_sin_leer to "SIN_LEER"
        )) { conversaciones?.let { mostrar(Resource.Success(it)) } }
        barra.vincular(binding.swipe, binding.rvEquipo)
        controlesFlotantes?.mostrarBotonFlotante(R.string.chat_nuevo, R.drawable.ic_chat) { nuevoChat() }
        // También al volver de un chat: cambian el último mensaje y los no leídos
        cargar()
    }

    private fun cargar() {
        viewLifecycleOwner.lifecycleScope.launch {
            val resource = try {
                val r = RetrofitClient.instance.getConversaciones()
                if (r.isSuccessful) Resource.Success(r.body().orEmpty())
                else Resource.Error(RetrofitClient.mensajeDeError(r, getString(R.string.chat_error)))
            } catch (e: Exception) {
                Resource.Error(RetrofitClient.mensajeDeExcepcion(e))
            }
            if (_binding == null) return@launch
            if (resource is Resource.Success) conversaciones = resource.data
            mostrar(resource)
        }
    }

    private fun mostrar(resource: Resource<List<ConversacionDTO>>) {
        val filtrado = if (resource is Resource.Success) Resource.Success(resource.data.orEmpty().filter { c ->
            barra.coincide(c.torre, c.apto, "torre ${c.torre} apto ${c.apto}", c.ultimoMensaje) &&
                (barra.filtro == null || c.noLeidos > 0)
        }) else resource
        run {
            binding.estado.mostrarRecurso(filtrado, binding.swipe, R.drawable.ic_chat,
                if (barra.activa || barra.filtro != null) getString(R.string.busqueda_sin_resultados) else getString(R.string.chat_conversaciones_vacio),
                reintentar = { cargar() }) { lista ->
                adaptador.submitList(lista)
                val noLeidos = lista.sumOf { it.noLeidos }
                binding.encabezado.ponerInfo(
                    if (noLeidos == 0) null else resources.getQuantityString(R.plurals.chat_no_leidos, noLeidos, noLeidos)
                )
            }
        }
    }

    /** Torre, apto y con quién (propietario o arrendatario). */
    private fun nuevoChat() {
        val margen = (24 * resources.displayMetrics.density).toInt()
        fun campo(hint: Int) = TextInputLayout(requireContext()).apply {
            this.hint = getString(hint)
            addView(TextInputEditText(requireContext()).apply { inputType = InputType.TYPE_CLASS_TEXT })
            setPadding(0, margen / 3, 0, 0)
        }
        val torre = campo(R.string.login_torre)
        val apto = campo(R.string.login_apartamento)
        val quien = MaterialButtonToggleGroup(requireContext()).apply {
            isSingleSelection = true
            isSelectionRequired = true
            listOf(R.string.chat_propietario, R.string.chat_arrendatario).forEachIndexed { i, texto ->
                addView(com.google.android.material.button.MaterialButton(requireContext(), null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                    id = View.generateViewId()
                    setText(texto)
                    if (i == 0) post { check(id) }
                })
            }
        }
        val contenedor = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(margen, margen / 2, margen, 0)
            addView(torre)
            addView(apto)
            addView(TextView(requireContext()).apply { setText(R.string.chat_con); setPadding(0, margen / 2, 0, margen / 4) })
            addView(quien)
        }
        val dialogo = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.chat_nuevo)
            .setView(contenedor)
            .setPositiveButton(R.string.chat_enviar, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val t = torre.editText?.text?.toString()?.trim().orEmpty()
            val a = apto.editText?.text?.toString()?.trim().orEmpty()
            torre.error = if (t.isEmpty()) getString(R.string.equipo_dato_obligatorio) else null
            apto.error = if (a.isEmpty()) getString(R.string.equipo_dato_obligatorio) else null
            if (t.isEmpty() || a.isEmpty()) return@setOnClickListener
            val participante = if (quien.checkedButtonId == quien.getChildAt(1).id) "ARRENDATARIO" else "PROPIETARIO"
            viewLifecycleOwner.lifecycleScope.launch {
                val encontrado = try {
                    RetrofitClient.instance.getApartamentos().body().orEmpty()
                        .firstOrNull { it.torre.equals(t, true) && it.apto.equals(a, true) }
                } catch (e: Exception) {
                    aviso(RetrofitClient.mensajeDeExcepcion(e)).show()
                    return@launch
                }
                val id = encontrado?.id
                if (id == null) {
                    apto.error = getString(R.string.chat_apto_no_existe)
                    return@launch
                }
                dialogo.dismiss()
                abrirChatCon(id, participante)
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    private class ConversacionAdapter(private val onTocar: (ConversacionDTO) -> Unit) :
        ListAdapter<ConversacionDTO, ConversacionAdapter.VH>(DiffPorId { it.id }) {
        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val titulo: TextView = v.findViewById(android.R.id.text1)
            val detalle: TextView = v.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false))

        override fun onBindViewHolder(h: VH, position: Int) {
            val c = getItem(position)
            val contexto = h.itemView.context
            val quien = contexto.getString(if (c.participante == "ARRENDATARIO") R.string.chat_arrendatario else R.string.chat_propietario)
            val titulo = contexto.getString(R.string.chat_titulo_apto, c.torre, c.apto, quien)
            h.titulo.text = if (c.noLeidos > 0) "$titulo · ${contexto.resources.getQuantityString(R.plurals.chat_no_leidos, c.noLeidos, c.noLeidos)}"
            else titulo
            h.titulo.setTypeface(null, if (c.noLeidos > 0) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            val cuando = c.ultimoMensajeEn?.let { Formatos.fechaHora(it.take(10), it.drop(11).take(5)) }.orEmpty()
            h.detalle.text = listOfNotNull(c.ultimoMensaje, cuando.ifEmpty { null }).joinToString("\n")
            h.itemView.setOnClickListener { onTocar(c) }
        }
    }
}
