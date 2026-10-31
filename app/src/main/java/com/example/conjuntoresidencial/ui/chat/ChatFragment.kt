package com.example.conjuntoresidencial.ui.chat

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.FragmentChatBinding
import com.example.conjuntoresidencial.databinding.ItemMensajeBinding
import com.example.conjuntoresidencial.model.AbrirChat
import com.example.conjuntoresidencial.model.ConversacionDTO
import com.example.conjuntoresidencial.model.MensajeChatDTO
import com.example.conjuntoresidencial.model.MensajeNuevo
import com.example.conjuntoresidencial.ui.comun.DiffPorId
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.controlesFlotantes
import com.example.conjuntoresidencial.ui.comun.ponerTitulo
import com.example.conjuntoresidencial.util.Formatos
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Abre (o crea) el chat de la administración con el propietario o el arrendatario de [aptoId]. */
fun Fragment.abrirChatCon(aptoId: Long, participante: String) {
    viewLifecycleOwner.lifecycleScope.launch {
        try {
            val r = RetrofitClient.comun.abrirChat(AbrirChat(aptoId, participante))
            val c = r.body()
            if (!r.isSuccessful || c == null) {
                aviso(RetrofitClient.mensajeDeError(r, getString(R.string.chat_error))).show()
                return@launch
            }
            findNavController().navigate(R.id.chatFragment, bundleOf(ChatFragment.ARG_CONVERSACION to c.id))
        } catch (e: Exception) {
            aviso(RetrofitClient.mensajeDeExcepcion(e)).show()
        }
    }
}

/**
 * Chat (P5): el residente con la administración, o la administración con un residente
 * ([ARG_CONVERSACION]). Mientras está en pantalla pide los mensajes nuevos cada pocos segundos.
 */
class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private val adaptador = MensajeAdapter()
    private val esAdmin get() = resources.getBoolean(R.bool.app_admin)
    private var conversacionId: Long? = null
    private var enviando = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(
            binding.encabezado, if (esAdmin) R.string.chat_titulo else R.string.chat_con_administracion,
            if (esAdmin) R.string.chat_admin_subtitulo else R.string.chat_residente_subtitulo
        )
        // El encabezado grande queda plegado y quieto: desplegado, la hoja con el campo para escribir
        // se saldría de la pantalla (sobre todo con el teclado abierto)
        binding.encabezado.root.setExpanded(false, false)
        (binding.encabezado.root.layoutParams as androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams).behavior =
            com.google.android.material.appbar.AppBarLayout.Behavior().apply {
                setDragCallback(object : com.google.android.material.appbar.AppBarLayout.Behavior.DragCallback() {
                    override fun canDrag(appBarLayout: com.google.android.material.appbar.AppBarLayout) = false
                })
            }
        binding.rvMensajes.isNestedScrollingEnabled = false
        binding.tvConQuien.setText(if (esAdmin) R.string.chat_titulo else R.string.chat_con_administracion)
        // Plegado, la hoja empieza arriba del todo: el título va debajo de la barra de estado y los botones
        // (MainActivity ya consume los insets: se leen de la ventana cuando la vista está puesta)
        binding.tvConQuien.post {
            val vista = _binding?.tvConQuien ?: return@post
            val arriba = ViewCompat.getRootWindowInsets(vista)?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
            vista.updatePadding(top = arriba + resources.getDimensionPixelSize(R.dimen.alto_botones_superiores))
        }
        binding.rvMensajes.layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        binding.rvMensajes.adapter = adaptador
        binding.estado.vincular(binding.rvMensajes)
        // El campo para escribir va pegado abajo: sin el difuminado de las demás pantallas
        controlesFlotantes?.sinDifuminadoAbajo()
        // El campo sube con el teclado (y no queda debajo de la barra de navegación)
        ViewCompat.setOnApplyWindowInsetsListener(binding.barraEscribir) { v, insets ->
            val abajo = insets.getInsets(WindowInsetsCompat.Type.ime() or WindowInsetsCompat.Type.navigationBars()).bottom
            v.updatePadding(bottom = abajo + (8 * resources.displayMetrics.density).toInt())
            insets
        }
        binding.etMensaje.doAfterTextChanged { binding.btnEnviar.isEnabled = !it.isNullOrBlank() && !enviando }
        binding.btnEnviar.setOnClickListener { enviar() }

        conversacionId = arguments?.getLong(ARG_CONVERSACION)?.takeIf { it > 0 }
        binding.estado.mostrarCargando()
        viewLifecycleOwner.lifecycleScope.launch {
            val c = conversacion() ?: return@launch
            conversacionId = c.id
            if (esAdmin) {
                binding.encabezado.ponerTitulo(titulo(c))
                binding.tvConQuien.text = titulo(c)
            }
            // Mientras se ve la pantalla: los nuevos cada 5 segundos
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    traerNuevos(c.id)
                    delay(INTERVALO)
                }
            }
        }
    }

    private fun titulo(c: ConversacionDTO) = getString(R.string.chat_titulo_apto, c.torre, c.apto,
        getString(if (c.participante == "ARRENDATARIO") R.string.chat_arrendatario else R.string.chat_propietario))

    /** La de los argumentos o, para el residente, la suya (se crea si no existe). */
    private suspend fun conversacion(): ConversacionDTO? {
        return try {
            val id = conversacionId
            // Con id: marcarla leída devuelve sus datos (torre, apto, con quién)
            val r = if (id == null) RetrofitClient.comun.abrirChat(AbrirChat())
            else RetrofitClient.comun.marcarChatLeido(id)
            val c = r.body()
            if (!r.isSuccessful || c == null) {
                binding.estado.mostrarError(RetrofitClient.mensajeDeError(r, getString(R.string.chat_error))) { reintentar() }
                null
            } else c
        } catch (e: Exception) {
            binding.estado.mostrarError(RetrofitClient.mensajeDeExcepcion(e)) { reintentar() }
            null
        }
    }

    private fun reintentar() {
        findNavController().navigate(R.id.chatFragment, arguments,
            androidx.navigation.NavOptions.Builder().setPopUpTo(R.id.chatFragment, true).build())
    }

    private suspend fun traerNuevos(id: Long) {
        try {
            val ultimo = adaptador.currentList.lastOrNull()?.id
            val r = RetrofitClient.comun.getMensajes(id, ultimo)
            if (!r.isSuccessful) {
                if (adaptador.currentList.isEmpty()) {
                    binding.estado.mostrarError(RetrofitClient.mensajeDeError(r, getString(R.string.chat_error))) { reintentar() }
                }
                return
            }
            val nuevos = r.body().orEmpty()
            if (nuevos.isNotEmpty()) {
                // Los que ya están (el propio recién enviado) no se repiten
                val ids = adaptador.currentList.map { it.id }.toSet()
                agregar(nuevos.filter { it.id !in ids })
                if (nuevos.any { !it.propio }) RetrofitClient.comun.marcarChatLeido(id)
            }
            if (adaptador.currentList.isEmpty()) {
                binding.estado.mostrarVacio(R.drawable.ic_chat, getString(R.string.chat_vacio),
                    if (esAdmin) null else getString(R.string.chat_vacio_residente))
            } else binding.estado.mostrarContenido()
        } catch (_: Exception) {
            // Sin conexión un momento: se intenta otra vez en la siguiente vuelta
        }
    }

    private fun agregar(mensajes: List<MensajeChatDTO>) {
        if (mensajes.isEmpty()) return
        adaptador.submitList(adaptador.currentList + mensajes) {
            _binding?.rvMensajes?.scrollToPosition(adaptador.itemCount - 1)
        }
    }

    private fun enviar() {
        val id = conversacionId ?: return
        val texto = binding.etMensaje.text?.toString()?.trim().orEmpty()
        if (texto.isEmpty() || enviando) return
        enviando = true
        binding.btnEnviar.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val r = RetrofitClient.comun.enviarMensaje(id, MensajeNuevo(texto))
                val m = r.body()
                if (r.isSuccessful && m != null) {
                    binding.etMensaje.text?.clear()
                    agregar(listOf(m).filter { nuevo -> adaptador.currentList.none { it.id == nuevo.id } })
                    binding.estado.mostrarContenido()
                } else {
                    aviso(RetrofitClient.mensajeDeError(r, getString(R.string.chat_error_enviar))).show()
                }
            } catch (e: Exception) {
                aviso(RetrofitClient.mensajeDeExcepcion(e)).show()
            } finally {
                enviando = false
                _binding?.let { it.btnEnviar.isEnabled = !it.etMensaje.text.isNullOrBlank() }
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    private class MensajeAdapter : ListAdapter<MensajeChatDTO, MensajeAdapter.VH>(DiffPorId { it.id }) {
        class VH(val b: ItemMensajeBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemMensajeBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(h: VH, position: Int) {
            val m = getItem(position)
            val b = h.b
            (b.burbuja.layoutParams as FrameLayout.LayoutParams).gravity = if (m.propio) Gravity.END else Gravity.START
            b.burbuja.requestLayout()
            val fondo = if (m.propio) com.google.android.material.R.attr.colorPrimaryContainer
            else com.google.android.material.R.attr.colorSurfaceContainerHighest
            val texto = if (m.propio) com.google.android.material.R.attr.colorOnPrimaryContainer
            else com.google.android.material.R.attr.colorOnSurface
            b.burbuja.setCardBackgroundColor(MaterialColors.getColor(b.burbuja, fondo))
            val color = MaterialColors.getColor(b.burbuja, texto)
            listOf(b.tvAutor, b.tvTexto, b.tvHora).forEach { it.setTextColor(color) }
            // El nombre solo en los del otro lado
            b.tvAutor.visibility = if (m.propio) View.GONE else View.VISIBLE
            b.tvAutor.text = m.autorNombre
            b.tvTexto.text = m.texto
            b.tvHora.text = Formatos.fechaHora(m.enviadoEn.take(10), m.enviadoEn.drop(11).take(5))
        }
    }

    companion object {
        const val ARG_CONVERSACION = "conversacionId"
        private const val INTERVALO = 5_000L
    }
}
