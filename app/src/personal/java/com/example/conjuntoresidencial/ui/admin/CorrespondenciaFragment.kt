package com.example.conjuntoresidencial.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.DialogoCorrespondenciaBinding
import com.example.conjuntoresidencial.databinding.FragmentCorrespondenciaBinding
import com.example.conjuntoresidencial.databinding.ItemCorrespondenciaBinding
import com.example.conjuntoresidencial.model.CorrespondenciaDTO
import com.example.conjuntoresidencial.model.EntregaCorrespondencia
import com.example.conjuntoresidencial.model.NuevaCorrespondencia
import com.example.conjuntoresidencial.model.ReciboParaTodos
import com.example.conjuntoresidencial.ui.comun.TIPOS_CORRESPONDENCIA
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.nombreTipoCorrespondencia
import com.example.conjuntoresidencial.ui.comun.pintarCorrespondencia
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import retrofit2.Response

/**
 * P9 bloque 3: la correspondencia en la portería (y en la administración). Se registra lo que
 * llega para un apartamento, o un recibo público para todos; al apartamento le llega el aviso.
 * Cuando lo recogen, Entregar pide el nombre de quien recoge.
 */
class CorrespondenciaFragment : Fragment() {

    private var _binding: FragmentCorrespondenciaBinding? = null
    private val binding get() = _binding!!

    private var todas: List<CorrespondenciaDTO> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCorrespondenciaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(binding.encabezado, R.string.corr_titulo, R.string.corr_descripcion)
        binding.btnRegistrar.setOnClickListener { registrar() }
        binding.btnReciboTodos.setOnClickListener { reciboParaTodos() }
        binding.chipsEstado.setOnCheckedStateChangeListener { _, _ -> cargar() }
        binding.etBuscar.doAfterTextChanged { pintar() }
        binding.swipe.setOnRefreshListener { cargar() }
        cargar()
    }

    private val entregadas get() = binding.chipEntregadas.isChecked

    private fun cargar() {
        val estado = if (entregadas) "ENTREGADA" else "PENDIENTE"
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val r = RetrofitClient.comun.correspondencia(estado, null, 7)
                if (_binding == null) return@launch
                if (r.isSuccessful) {
                    todas = r.body().orEmpty()
                    pintar()
                } else {
                    aviso(RetrofitClient.mensajeDeError(r, getString(R.string.corr_error))).show()
                }
            } catch (e: Exception) {
                if (_binding != null) aviso(RetrofitClient.mensajeDeExcepcion(e)).show()
            }
            _binding?.swipe?.isRefreshing = false
        }
    }

    /** La búsqueda filtra lo cargado: apartamento ("5-502", "502"), remitente o quién recogió. */
    private fun pintar() {
        val q = binding.etBuscar.text?.toString()?.trim()?.lowercase().orEmpty()
        val lista = todas.filter { c ->
            q.isEmpty() || "${c.torre}-${c.apto}".lowercase().contains(q) || c.apto.lowercase() == q ||
                c.remitente.orEmpty().lowercase().contains(q) || c.entregadaA.orEmpty().lowercase().contains(q)
        }
        if (!entregadas) binding.chipPendientes.text = getString(R.string.corr_por_entregar_n, todas.size)
        binding.tvVacio.isVisible = lista.isEmpty()
        binding.tvVacio.setText(if (entregadas) R.string.corr_vacio_entregadas else R.string.corr_vacio_pendientes)
        binding.lista.removeAllViews()
        lista.forEach { c ->
            val fila = ItemCorrespondenciaBinding.inflate(layoutInflater, binding.lista, false)
            pintarCorrespondencia(fila, c, conApartamento = true) { entregar(it) }
            binding.lista.addView(fila.root)
        }
    }

    private fun registrar() {
        val d = DialogoCorrespondenciaBinding.inflate(layoutInflater)
        TIPOS_CORRESPONDENCIA.forEachIndexed { i, (codigo, nombre) ->
            d.chipsTipo.addView(Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle).apply {
                id = View.generateViewId()
                tag = codigo
                setText(nombre)
                isCheckable = true
                isChecked = i == 0
            })
        }
        dialogo(R.string.corr_nueva_titulo, d, R.string.corr_guardar) { cerrar ->
            val torre = d.etTorre.text?.toString()?.trim().orEmpty()
            val apto = d.etApto.text?.toString()?.trim().orEmpty()
            val falta = torre.isEmpty() || apto.isEmpty()
            d.tilApto.error = if (falta) getString(R.string.corr_falta_apto) else null
            if (falta) return@dialogo
            val tipo = d.chipsTipo.findViewById<Chip>(d.chipsTipo.checkedChipId)?.tag as? String ?: "PAQUETE"
            enviar({ RetrofitClient.comun.registrarCorrespondencia(
                NuevaCorrespondencia(torre, apto, tipo, texto(d.etCampo1.text), texto(d.etCampo2.text))) }) {
                cerrar()
                aviso(getString(R.string.corr_registrada_ok, torre, apto)).show()
            }
        }
    }

    private fun reciboParaTodos() {
        val d = DialogoCorrespondenciaBinding.inflate(layoutInflater)
        d.chipsTipo.isVisible = false
        d.filaApartamento.isVisible = false
        d.tvMensaje.isVisible = true
        d.tvMensaje.setText(R.string.corr_todos_mensaje)
        d.tilCampo1.setHint(R.string.corr_empresa)
        d.tilCampo2.setHint(R.string.corr_mes)
        dialogo(R.string.corr_todos_titulo, d, R.string.corr_avisar_todos) { cerrar ->
            val empresa = texto(d.etCampo1.text)
            d.tilCampo1.error = if (empresa == null) getString(R.string.corr_falta_empresa) else null
            if (empresa == null) return@dialogo
            enviar({ RetrofitClient.comun.reciboParaTodos(ReciboParaTodos(empresa, texto(d.etCampo2.text))) }) { lote ->
                cerrar()
                aviso(getString(R.string.corr_todos_ok, lote?.creadas ?: 0)).show()
            }
        }
    }

    private fun entregar(c: CorrespondenciaDTO) {
        val d = DialogoCorrespondenciaBinding.inflate(layoutInflater)
        d.chipsTipo.isVisible = false
        d.filaApartamento.isVisible = false
        d.tilCampo2.isVisible = false
        d.tvMensaje.isVisible = true
        d.tvMensaje.text = getString(R.string.corr_entregar_mensaje,
            listOfNotNull(getString(nombreTipoCorrespondencia(c.tipo)), c.remitente).joinToString(" · "), c.torre, c.apto)
        d.tilCampo1.setHint(R.string.corr_nombre_recoge)
        dialogo(R.string.corr_entregar_titulo, d, R.string.corr_entregar) { cerrar ->
            val nombre = texto(d.etCampo1.text)
            d.tilCampo1.error = if (nombre == null) getString(R.string.corr_falta_nombre) else null
            if (nombre == null) return@dialogo
            enviar({ RetrofitClient.comun.entregarCorrespondencia(c.id, EntregaCorrespondencia(nombre)) }) {
                cerrar()
                aviso(getString(R.string.corr_entregada_ok, nombre)).show()
            }
        }
    }

    /** Un diálogo que no se cierra solo al tocar el botón: [alAceptar] valida y lo cierra si sale bien. */
    private fun dialogo(titulo: Int, d: DialogoCorrespondenciaBinding, boton: Int, alAceptar: (cerrar: () -> Unit) -> Unit) {
        val dialogo = MaterialAlertDialogBuilder(requireContext())
            .setTitle(titulo)
            .setView(d.root)
            .setPositiveButton(boton, null)
            .setNegativeButton(R.string.cancelar_corr, null)
            .show()
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            alAceptar { dialogo.dismiss() }
        }
    }

    private fun <T> enviar(llamada: suspend () -> Response<T>, alTerminar: (T?) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val r = llamada()
                if (_binding == null) return@launch
                if (r.isSuccessful) {
                    alTerminar(r.body())
                    cargar()
                } else {
                    aviso(RetrofitClient.mensajeDeError(r, getString(R.string.corr_error))).show()
                }
            } catch (e: Exception) {
                if (_binding != null) aviso(RetrofitClient.mensajeDeExcepcion(e)).show()
            }
        }
    }

    private fun texto(e: CharSequence?): String? = e?.toString()?.trim()?.takeIf { it.isNotEmpty() }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
