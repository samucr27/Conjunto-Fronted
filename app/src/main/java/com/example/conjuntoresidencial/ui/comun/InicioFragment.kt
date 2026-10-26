package com.example.conjuntoresidencial.ui.comun

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FragmentInicioBinding
import com.example.conjuntoresidencial.util.SessionManager
import com.example.conjuntoresidencial.util.confirmarCerrarSesion

/**
 * Primera pestaña de las dos apps (P5): saludo con el nombre y, debajo, el feed de
 * publicaciones de la administración (bloque 3). Las opciones que antes estaban aquí ahora
 * están en Herramientas.
 */
class InicioFragment : Fragment() {
    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInicioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sesion = SessionManager(requireContext())
        // "Hola, Laura" (entró con su cuenta) o "Hola" con el usuario de la administración
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

        binding.rvPublicaciones.layoutManager = LinearLayoutManager(requireContext())
        binding.estado.vincular(binding.rvPublicaciones)
        binding.swipe.isEnabled = false
        // El feed llega en el bloque 3; mientras tanto, el estado vacío
        binding.estado.mostrarVacio(
            R.drawable.ic_campaign, getString(R.string.inicio_sin_publicaciones),
            getString(if (sesion.esAdmin()) R.string.inicio_sin_publicaciones_admin else R.string.inicio_sin_publicaciones_residente)
        )
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
