package com.example.conjuntoresidencial.ui.propietario

import android.text.InputType
import com.example.conjuntoresidencial.ui.comun.ejecutarAccion
import com.example.conjuntoresidencial.ui.comun.pedirTexto
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.DialogDetalleObraBinding
import com.example.conjuntoresidencial.databinding.FragmentMisObrasBinding
import com.example.conjuntoresidencial.databinding.ItemPersonalFilaBinding
import com.example.conjuntoresidencial.model.ObraDTO
import com.example.conjuntoresidencial.model.PersonalObraDTO
import com.example.conjuntoresidencial.ui.comun.Estados
import com.example.conjuntoresidencial.ui.comun.mostrarEstado
import com.example.conjuntoresidencial.ui.comun.mostrarRecurso
import com.example.conjuntoresidencial.util.Formatos
import com.example.conjuntoresidencial.util.SessionManager
import com.example.conjuntoresidencial.ui.comun.accionesDeLista
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.ui.comun.resumen
import com.example.conjuntoresidencial.ui.comun.aviso
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MisObrasFragment : Fragment() {
    private var _binding: FragmentMisObrasBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MisObrasViewModel by viewModels()
    private var aptoId: Long = -1L

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMisObrasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(
            binding.encabezado, R.string.menu_mis_obras, R.string.mis_obras_subtitulo, menu = R.menu.menu_lista,
            alElegir = accionesDeLista(binding.swipe) { viewModel.fetchObras(aptoId) }
        )
        aptoId = SessionManager(requireContext()).getApartamentoId()

        val adaptador = MisObrasAdapter(
            onDetalle = { obra -> mostrarDetalleObra(obra) },
            onFinalizar = { obra -> confirmarFinalizar(obra) }
        )
        binding.rvMisObras.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMisObras.adapter = adaptador
        binding.estado.vincular(binding.rvMisObras)
        binding.swipe.setOnRefreshListener { viewModel.fetchObras(aptoId) }

        viewModel.obras.observe(viewLifecycleOwner) { resource ->
            binding.estado.mostrarRecurso(
                resource, binding.swipe, R.drawable.ic_build, getString(R.string.mis_obras_vacio),
                getString(R.string.mis_obras_vacio_mensaje),
                reintentar = { viewModel.fetchObras(aptoId) }
            ) { lista ->
                adaptador.submitList(lista)
                binding.encabezado.ponerInfo(resumen(
                    R.plurals.resumen_obras, lista.size,
                    R.plurals.resumen_en_proceso to lista.count { it.estadoObra == "EN_PROCESO" }
                ))
            }
        }

        viewModel.mensaje.observe(viewLifecycleOwner) { evento ->
            evento.obtenerSiNoManejado()?.let {
                aviso(it, Snackbar.LENGTH_LONG).show()
            }
        }

        if (viewModel.obras.value == null) viewModel.fetchObras(aptoId)
    }

    /** Carga el personal de la obra y muestra el detalle; se cancela si el usuario sale de la pantalla. */
    private fun mostrarDetalleObra(obra: ObraDTO) {
        viewLifecycleOwner.lifecycleScope.launch {
            val personal: List<PersonalObraDTO>? = try {
                val response = RetrofitClient.instance.getPersonalPorObra(obra.id!!)
                if (response.isSuccessful) response.body().orEmpty() else null
            } catch (_: Exception) {
                null
            }

            val vista = DialogDetalleObraBinding.inflate(layoutInflater)
            vista.tvEstadoDetalle.mostrarEstado(Estados.obra(obra.estadoObra))
            vista.tvFechasDetalle.text = Formatos.rango(obra.fechaInicio, obra.fechaFinEstimada)
            when {
                personal == null -> vista.tvSinPersonalDetalle.setText(R.string.obra_error_personal)
                personal.isEmpty() -> vista.tvSinPersonalDetalle.setText(R.string.obras_sin_personal)
                else -> personal.forEach { p ->
                    val fila = ItemPersonalFilaBinding.inflate(layoutInflater, vista.contenedorPersonal, true)
                    fila.tvNombreFila.text = p.nombreTrabajador
                    fila.tvCedulaFila.text = getString(R.string.personal_cedula, p.cedula)
                    fila.tvArlFila.mostrarEstado(Estados.arl(p.arlEstado))
                }
            }
            vista.tvSinPersonalDetalle.visibility = if (personal.isNullOrEmpty()) View.VISIBLE else View.GONE
            // Sin personal: solo el mensaje, sin el título de la sección
            vista.tvTituloPersonalDetalle.visibility = if (personal?.isEmpty() == true) View.GONE else View.VISIBLE

            val detalle = MaterialAlertDialogBuilder(requireContext())
                .setTitle(obra.descripcionAdecuacion)
                .setView(vista.root)
                .setPositiveButton(R.string.btn_cerrar, null)
            if (obra.estadoObra == "EN_PROCESO") detalle.setNeutralButton(R.string.obra_editar_descripcion) { _, _ ->
                pedirTexto(getString(R.string.obra_editar_descripcion), R.string.registrar_obra_descripcion, obra.descripcionAdecuacion,
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES,
                    validar = { if (it.isBlank()) getString(R.string.obra_descripcion_obligatoria) else null }) { nueva ->
                    ejecutarAccion(getString(R.string.obra_editada),
                        { RetrofitClient.instance.updateObra(obra.id!!, obra.copy(descripcionAdecuacion = nueva)) }) {
                        viewModel.fetchObras(aptoId)
                    }
                }
            }
            detalle.show()
        }
    }

    private fun confirmarFinalizar(obra: ObraDTO) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.obra_finalizar_titulo)
            .setMessage(getString(R.string.obra_finalizar_pregunta, obra.descripcionAdecuacion))
            .setPositiveButton(R.string.obra_finalizar_si) { _, _ ->
                viewModel.finalizarObra(obra.id!!, obra, getString(R.string.obra_error_finalizar))
            }
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
