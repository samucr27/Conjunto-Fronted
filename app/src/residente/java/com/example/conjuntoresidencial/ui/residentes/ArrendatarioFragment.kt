package com.example.conjuntoresidencial.ui.residentes

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.api.instance
import com.example.conjuntoresidencial.databinding.FragmentArrendatarioBinding
import com.example.conjuntoresidencial.model.AccesoResidente
import com.example.conjuntoresidencial.model.AprobacionPropietario
import com.example.conjuntoresidencial.model.ResidenteDTO
import com.example.conjuntoresidencial.ui.comun.EstadoVisual
import com.example.conjuntoresidencial.ui.comun.TonoEstado
import com.example.conjuntoresidencial.ui.comun.accionesDeLista
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.confirmar
import com.example.conjuntoresidencial.ui.comun.ejecutarAccion
import com.example.conjuntoresidencial.ui.comun.mostrarEstado
import com.example.conjuntoresidencial.ui.comun.pedirTexto
import com.example.conjuntoresidencial.util.SessionManager
import kotlinx.coroutines.launch

/**
 * Administrar arrendatario (solo el propietario, P5): ver su solicitud y aceptarla o
 * rechazarla, cambiarle la contraseña o quitarlo. Mientras haya un arrendatario activo, el
 * propietario no reserva, no registra vehículos ni gestiona Mi hogar; al quitarlo los recupera.
 */
class ArrendatarioFragment : Fragment() {
    private var _binding: FragmentArrendatarioBinding? = null
    private val binding get() = _binding!!
    private val aptoId by lazy { SessionManager(requireContext()).getApartamentoId() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentArrendatarioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(binding.encabezado, R.string.arrendatario_titulo, R.string.arrendatario_descripcion,
            menu = R.menu.menu_lista, alElegir = accionesDeLista(binding.swipe) { cargar() })
        binding.estado.vincular(binding.contenido)
        binding.swipe.setOnRefreshListener { cargar() }
        cargar()
    }

    private fun cargar() {
        if (!binding.swipe.isRefreshing) binding.estado.mostrarCargando()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.getArrendatario(aptoId)
                if (_binding == null) return@launch
                binding.swipe.isRefreshing = false
                val arrendatario = r.body()
                when {
                    !r.isSuccessful -> binding.estado.mostrarError(
                        RetrofitClient.mensajeDeError(r, getString(R.string.residentes_error_cargar))) { cargar() }
                    arrendatario == null -> binding.estado.mostrarVacio(R.drawable.ic_people,
                        getString(R.string.arrendatario_vacio), getString(R.string.arrendatario_vacio_mensaje))
                    else -> { mostrar(arrendatario); binding.estado.mostrarContenido() }
                }
            } catch (e: Exception) {
                if (_binding == null) return@launch
                binding.swipe.isRefreshing = false
                binding.estado.mostrarError(RetrofitClient.mensajeDeExcepcion(e)) { cargar() }
            }
        }
    }

    private fun mostrar(a: ResidenteDTO) {
        val esperaPropietario = a.aprobadoPropietario == false
        val activo = a.estado == "APROBADO" && !esperaPropietario
        binding.tvNombreArrendatario.text = a.nombreCompleto
        binding.tvDetalleArrendatario.text = listOfNotNull(
            getString(R.string.registro_doc, a.identificacion), a.telefono, a.correo
        ).joinToString("\n")
        binding.tvEstadoArrendatario.mostrarEstado(when {
            esperaPropietario -> EstadoVisual(R.string.arrendatario_espera_tu_aprobacion, TonoEstado.ADVERTENCIA)
            !activo -> EstadoVisual(R.string.arrendatario_espera_administracion, TonoEstado.ADVERTENCIA)
            else -> EstadoVisual(R.string.arrendatario_activo, TonoEstado.EXITO)
        })
        binding.tvNotaArrendatario.setText(if (activo) R.string.arrendatario_nota_activo else R.string.arrendatario_nota_pendiente)
        binding.grupoDecision.visibility = if (esperaPropietario) View.VISIBLE else View.GONE
        binding.btnCambiarContrasenaArr.visibility = if (a.tieneAcceso == true && !esperaPropietario) View.VISIBLE else View.GONE

        binding.btnAceptarArr.setOnClickListener {
            ejecutarAccion(getString(R.string.arrendatario_aceptado),
                { RetrofitClient.instance.aprobarArrendatario(a.id!!, AprobacionPropietario(true)) }) { cargar() }
        }
        binding.btnRechazarArr.setOnClickListener {
            pedirTexto(getString(R.string.arrendatario_rechazar_titulo), R.string.residente_motivo, null,
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, validar = { null }) { motivo ->
                ejecutarAccion(getString(R.string.arrendatario_rechazado),
                    { RetrofitClient.instance.aprobarArrendatario(a.id!!, AprobacionPropietario(false, motivo.ifBlank { null })) }) { cargar() }
            }
        }
        binding.btnCambiarContrasenaArr.setOnClickListener {
            pedirTexto(getString(R.string.residente_acceso_titulo, a.nombreCompleto), R.string.login_contrasena, null,
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
                validar = { if (it.length < 6) getString(R.string.error_contrasena_corta) else null }) { nueva ->
                ejecutarAccion(getString(R.string.contrasena_restablecida),
                    { RetrofitClient.instance.cambiarAccesoResidente(a.id!!, AccesoResidente(nueva)) }) { cargar() }
            }
        }
        binding.btnQuitarArr.setOnClickListener {
            confirmar(getString(R.string.arrendatario_quitar_titulo, a.nombreCompleto), getString(R.string.arrendatario_quitar_mensaje),
                R.string.residente_quitar) {
                ejecutarAccion(getString(R.string.arrendatario_quitado), { RetrofitClient.instance.deleteResidente(a.id!!) }) { cargar() }
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
