package com.example.conjuntoresidencial.ui.recepcion

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.ConfigApp
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.api.instance
import com.example.conjuntoresidencial.databinding.FragmentRecepcionInicioBinding
import com.example.conjuntoresidencial.databinding.ItemCifraPorteriaBinding
import com.example.conjuntoresidencial.databinding.ItemTextoConAccionBinding
import com.example.conjuntoresidencial.model.AnotacionMinuta
import com.example.conjuntoresidencial.model.ResumenPorteria
import com.example.conjuntoresidencial.model.VisitaDTO
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.confirmar
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.ui.comun.mostrarCodigoDelDia
import com.example.conjuntoresidencial.util.SessionManager
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * P8 bloque 5: lo primero que ve el guardia al abrir su turno. Cuatro cifras (visitas de hoy,
 * esperando respuesta, dentro ahora y parqueaderos de invitados libres), quién sigue dentro con
 * su botón de salida, y lo último de la minuta. Se actualiza cada 15 segundos.
 */
class RecepcionInicioFragment : Fragment() {

    private var _binding: FragmentRecepcionInicioBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRecepcionInicioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bloqueo = ConfigApp.destinoBloqueo
        configurarEncabezado(binding.encabezado, R.string.recepcion_inicio_titulo, R.string.recepcion_inicio_descripcion,
            atras = false, menu = if (bloqueo != null) R.menu.menu_porteria_recepcion else null) { id ->
            (id == R.id.accion_bloquear && bloqueo != null).also { tocado ->
                if (tocado) bloqueo?.let { findNavController().navigate(it, null, NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()) }
            }
        }
        SessionManager(requireContext()).getGuardia()?.let { binding.encabezado.ponerInfo(getString(R.string.porteria_turno_de, it)) }
        // Es la primera pantalla del turno: "atrás" deja la app abierta para no borrar la sesión del puesto
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { requireActivity().moveTaskToBack(true) }

        cifra(binding.cifraHoy, R.drawable.ic_people, R.string.recepcion_visitas_hoy)
        cifra(binding.cifraEsperando, R.drawable.ic_notifications, R.string.recepcion_esperando)
        cifra(binding.cifraDentro, R.drawable.ic_home, R.string.recepcion_dentro)
        cifra(binding.cifraParqueaderos, R.drawable.ic_car, R.string.recepcion_parqueaderos)

        mostrarCodigoDelDia(binding.codigoConjunto)
        binding.btnRegistrarVisita.setOnClickListener { irAPestana(R.id.porteriaFragment) }
        binding.btnCorrespondencia.setOnClickListener { findNavController().navigate(R.id.correspondenciaFragment) }
        binding.btnVerMinuta.setOnClickListener { findNavController().navigate(R.id.minutaFragment) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    cargar()
                    delay(INTERVALO_MS)
                }
            }
        }
    }

    /** Como tocar la pestaña en la barra de abajo: debajo queda solo este inicio. */
    private fun irAPestana(destino: Int) {
        findNavController().navigate(destino, null,
            NavOptions.Builder().setPopUpTo(R.id.recepcionInicioFragment, false).setLaunchSingleTop(true).build())
    }

    private fun cifra(c: ItemCifraPorteriaBinding, @DrawableRes icono: Int, @StringRes nombre: Int) {
        c.imgIcono.setImageResource(icono)
        c.tvNombre.setText(nombre)
    }

    private suspend fun cargar() = coroutineScope {
        val api = RetrofitClient.instance
        val resumen = async { runCatching { api.resumen().body() }.getOrNull() }
        val dentro = async { runCatching { api.visitasDentro().body() }.getOrNull() }
        val minuta = async { runCatching { api.minuta(1).body() }.getOrNull() }
        resumen.await()?.let { if (_binding != null) pintarResumen(it) }
        dentro.await()?.let { if (_binding != null) pintarDentro(it) }
        minuta.await()?.let { if (_binding != null) pintarMinuta(it) }
    }

    private fun pintarResumen(r: ResumenPorteria) {
        binding.cifraHoy.tvValor.text = r.visitasHoy.toString()
        binding.cifraEsperando.tvValor.text = r.esperandoRespuesta.toString()
        binding.cifraDentro.tvValor.text = r.dentro.toString()
        binding.cifraDentro.tvDetalle.isVisible = r.vehiculosDentro > 0
        binding.cifraDentro.tvDetalle.text = getString(R.string.recepcion_vehiculos_dentro, r.vehiculosDentro)
        binding.cifraParqueaderos.tvValor.text = if (r.parqueaderosTotal == 0) getString(R.string.recepcion_sin_parqueaderos)
            else getString(R.string.recepcion_parqueaderos_valor, r.parqueaderosLibres, r.parqueaderosTotal)
        binding.btnCorrespondencia.text = if (r.correspondenciaPendiente > 0)
            getString(R.string.corr_boton_inicio, r.correspondenciaPendiente) else getString(R.string.corr_titulo)
        r.guardia?.let { binding.encabezado.ponerInfo(getString(R.string.porteria_turno_de, it)) }
    }

    private fun pintarDentro(visitas: List<VisitaDTO>) {
        binding.tvDentroVacio.isVisible = visitas.isEmpty()
        binding.listaDentro.removeAllViews()
        visitas.forEach { v ->
            val fila = ItemTextoConAccionBinding.inflate(layoutInflater, binding.listaDentro, false)
            fila.tvEtiqueta.isVisible = true
            fila.tvEtiqueta.text = ingreso(v)
            fila.tvTitulo.text = v.visitante
            fila.tvDetalle.text = getString(R.string.recepcion_dentro_detalle, v.torre, v.apto, fechaCorta(v.creadaEn))
            fila.btnAccion.isVisible = true
            fila.btnAccion.setText(R.string.recepcion_salida)
            fila.btnAccion.setOnClickListener {
                confirmar(getString(R.string.recepcion_salida_confirmar, v.visitante), null, R.string.recepcion_salida) { salida(v) }
            }
            binding.listaDentro.addView(fila.root)
        }
    }

    private fun pintarMinuta(anotaciones: List<AnotacionMinuta>) {
        binding.tvMinutaVacia.isVisible = anotaciones.isEmpty()
        binding.listaMinuta.removeAllViews()
        anotaciones.take(3).forEach { m ->
            val fila = ItemTextoConAccionBinding.inflate(layoutInflater, binding.listaMinuta, false)
            fila.tvEtiqueta.isVisible = true
            fila.tvEtiqueta.setText(nombreCategoria(m.categoria))
            fila.tvTitulo.text = m.texto
            fila.tvDetalle.text = listOfNotNull(horaCorta(m.creadaEn), m.guardia).joinToString(" · ")
            binding.listaMinuta.addView(fila.root)
        }
    }

    private fun salida(v: VisitaDTO) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.registrarSalidaVisita(v.id)
                if (_binding == null) return@launch
                aviso(if (r.isSuccessful) {
                    v.parqueadero?.let { getString(R.string.ingreso_salida_ok, it) } ?: getString(R.string.recepcion_salida_ok)
                } else RetrofitClient.mensajeDeError(r, getString(R.string.porteria_error))).show()
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

    private companion object {
        const val INTERVALO_MS = 15_000L
    }
}
