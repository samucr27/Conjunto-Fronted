package com.example.conjuntoresidencial.ui.comun

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.FragmentInicioBinding
import com.example.conjuntoresidencial.model.AdjuntoPublicacion
import com.example.conjuntoresidencial.model.EdicionPublicacion
import com.example.conjuntoresidencial.model.FijarPublicacion
import com.example.conjuntoresidencial.model.PublicacionDTO
import com.example.conjuntoresidencial.ui.feed.FeedViewModel
import com.example.conjuntoresidencial.ui.feed.PublicacionAdapter
import com.example.conjuntoresidencial.ui.feed.PublicarFragment
import com.example.conjuntoresidencial.util.SessionManager
import com.example.conjuntoresidencial.util.confirmarCerrarSesion

/**
 * Primera pestaña de las dos apps (P5): saludo con el nombre y el feed de publicaciones de la
 * administración. En la app de administración, además, el botón para publicar y las opciones
 * de cada publicación (editar el texto o eliminarla).
 */
class InicioFragment : Fragment() {
    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FeedViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInicioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sesion = SessionManager(requireContext())
        // "Hola, Laura" (residente) o "Hola, admin" (usuario de la administración)
        val nombre = if (sesion.esAdmin()) sesion.getUsuario() else sesion.getNombre().substringBefore(' ')
        val subtitulo = when {
            sesion.esContador() -> getString(R.string.panel_contador_subtitulo)
            sesion.esAdmin() -> getString(R.string.inicio_subtitulo_admin)
            else -> getString(R.string.panel_residente_titulo, sesion.getTorre(), sesion.getApto())
        }
        configurarEncabezado(
            binding.encabezado,
            if (nombre.isBlank()) getString(R.string.inicio_hola) else getString(R.string.inicio_hola_nombre, nombre),
            R.string.inicio_descripcion, atras = false, menu = R.menu.menu_panel
        ) { item ->
            (item == R.id.accion_cerrar_sesion).also { if (it) confirmarCerrarSesion() }
        }
        binding.encabezado.ponerInfo(subtitulo)

        // "Atrás" en la primera pestaña pregunta si se quiere cerrar sesión
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = confirmarCerrarSesion()
            })

        // Publica la administración (no el contador)
        val publica = sesion.esAdmin() && !sesion.esContador()
        val adaptador = PublicacionAdapter(
            onAbrir = ::abrir, onOpciones = if (publica) ::opciones else null,
            onLeer = { viewModel.marcarLeida(it.id) }
        )
        val manejador = LinearLayoutManager(requireContext())
        binding.rvPublicaciones.layoutManager = manejador
        binding.rvPublicaciones.adapter = adaptador
        binding.estado.vincular(binding.rvPublicaciones)
        binding.swipe.setOnRefreshListener { viewModel.recargar() }
        binding.rvPublicaciones.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                // Cerca del final: la siguiente página
                if (dy > 0 && manejador.findLastVisibleItemPosition() >= adaptador.itemCount - 3) viewModel.cargarMas()
            }
        })

        viewModel.publicaciones.observe(viewLifecycleOwner) { resource ->
            binding.estado.mostrarRecurso(
                resource, binding.swipe, R.drawable.ic_campaign, getString(R.string.inicio_sin_publicaciones),
                getString(if (publica) R.string.inicio_sin_publicaciones_admin else R.string.inicio_sin_publicaciones_residente),
                reintentar = { viewModel.recargar() }
            ) { lista -> adaptador.submitList(lista) }
        }

        if (publica) {
            controlesFlotantes?.mostrarBotonFlotante(R.string.publicar_titulo, R.drawable.ic_add) {
                findNavController().navigate(R.id.publicarFragment)
            }
        }
        // Al volver de publicar, el feed se recarga
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Boolean>(PublicarFragment.PUBLICADO)
            ?.observe(viewLifecycleOwner) { publicado ->
                if (publicado == true) {
                    findNavController().currentBackStackEntry?.savedStateHandle?.set(PublicarFragment.PUBLICADO, false)
                    aviso(R.string.publicar_listo).show()
                    viewModel.recargar()
                }
            }

        if (viewModel.publicaciones.value == null) viewModel.recargar()
    }

    /** La imagen o el archivo, con la app que el celular tenga para abrirlo (enlace de una hora). */
    private fun abrir(adjunto: AdjuntoPublicacion) {
        val url = adjunto.url ?: return
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            aviso(R.string.cuota_sin_visor).show()
        }
    }

    private fun opciones(p: PublicacionDTO) {
        elegirOpcion(getString(R.string.feed_publicacion), listOf(
            R.string.feed_editar_texto to {
                pedirTexto(getString(R.string.feed_editar_texto), R.string.publicar_texto, p.texto,
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES,
                    validar = { if (it.isBlank() && p.adjuntos.isEmpty()) getString(R.string.publicar_vacia) else null }) { texto ->
                    ejecutarAccion(getString(R.string.feed_editada_ok),
                        { RetrofitClient.comun.editarPublicacion(p.id, EdicionPublicacion(texto = texto)) }) { viewModel.recargar() }
                }
            },
            (if (p.fijado) R.string.feed_quitar_fijado else R.string.feed_fijar) to {
                ejecutarAccion(getString(if (p.fijado) R.string.feed_desfijada_ok else R.string.feed_fijada_ok),
                    { RetrofitClient.comun.fijarPublicacion(p.id, FijarPublicacion(!p.fijado)) }) { viewModel.recargar() }
            },
            R.string.feed_eliminar to {
                confirmar(getString(R.string.feed_eliminar_titulo), getString(R.string.feed_eliminar_mensaje), R.string.residente_eliminar_si) {
                    ejecutarAccion(getString(R.string.feed_eliminada), { RetrofitClient.comun.eliminarPublicacion(p.id) }) {
                        viewModel.recargar()
                    }
                }
            }
        ))
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
