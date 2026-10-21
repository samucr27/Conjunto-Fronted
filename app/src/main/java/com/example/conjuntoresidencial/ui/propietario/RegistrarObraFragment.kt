package com.example.conjuntoresidencial.ui.propietario

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.FragmentRegistrarObraBinding
import com.example.conjuntoresidencial.model.PersonalObraDTO
import com.example.conjuntoresidencial.ui.comun.Estados
import com.example.conjuntoresidencial.util.Formatos
import com.example.conjuntoresidencial.util.Resource
import com.example.conjuntoresidencial.util.SessionManager
import com.example.conjuntoresidencial.util.configurarBarra
import com.example.conjuntoresidencial.util.limpiarErrorAlEscribir
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class RegistrarObraFragment : Fragment() {

    private var _binding: FragmentRegistrarObraBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RegistrarObraViewModel by viewModels()

    // Valores que acepta el backend; en pantalla se muestran con su etiqueta ("En proceso")
    private val estados = listOf("EN_PROCESO", "FINALIZADA")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegistrarObraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarBarra(binding.barra.toolbar, R.string.registrar_obra_titulo, R.string.registrar_obra_subtitulo)
        val aptoId = SessionManager(requireContext()).getApartamentoId()

        val etiquetas = estados.map { getString(Estados.obra(it).etiqueta) }
        binding.spinnerEstadoObra.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, etiquetas)
        )
        binding.spinnerEstadoObra.setText(etiquetas[estados.indexOf(viewModel.estado)], false)
        binding.spinnerEstadoObra.setOnItemClickListener { _, _, posicion, _ -> viewModel.estado = estados[posicion] }

        // Las fechas se guardan en el ViewModel (formato del backend) y se muestran como dd/MM/yyyy
        mostrarFechas()
        binding.etFechaInicioObra.setOnClickListener {
            elegirFecha(viewModel.fechaInicio, null) {
                viewModel.fechaInicio = it
                // Si el fin quedó antes del nuevo inicio, se borra para elegirlo otra vez
                if (viewModel.fechaFin.isNotEmpty() && viewModel.fechaFin < it) viewModel.fechaFin = ""
                mostrarFechas()
            }
        }
        binding.etFechaFinObra.setOnClickListener {
            elegirFecha(viewModel.fechaFin, viewModel.fechaInicio.ifEmpty { null }) {
                viewModel.fechaFin = it
                mostrarFechas()
            }
        }

        mostrarPersonal()
        binding.btnAgregarPersonal.setOnClickListener { agregarPersonalDesdeCampos() }
        listOf(binding.tilDescripcionObra, binding.tilNombrePersonalExterno, binding.tilCcPersonalExterno)
            .forEach { it.limpiarErrorAlEscribir() }

        binding.btnGuardarObra.setOnClickListener {
            val descripcion = binding.etDescripcionObra.text.toString().trim()
            binding.tilDescripcionObra.error =
                if (descripcion.isEmpty()) getString(R.string.registrar_obra_descripcion_obligatoria) else null
            binding.tilFechaInicioObra.error =
                if (viewModel.fechaInicio.isEmpty()) getString(R.string.registrar_obra_fecha_obligatoria) else null
            binding.tilFechaFinObra.error =
                if (viewModel.fechaFin.isEmpty()) getString(R.string.registrar_obra_fecha_obligatoria) else null
            if (descripcion.isEmpty() || viewModel.fechaInicio.isEmpty() || viewModel.fechaFin.isEmpty()) {
                return@setOnClickListener
            }
            viewModel.registrarObra(descripcion, aptoId, getString(R.string.registrar_obra_error))
        }

        viewModel.obraState.observe(viewLifecycleOwner) { resource ->
            val cargando = resource is Resource.Loading
            binding.progressRegistrarObra.visibility = if (cargando) View.VISIBLE else View.GONE
            binding.btnGuardarObra.isEnabled = !cargando
            when (resource) {
                is Resource.Loading -> Unit
                is Resource.Success -> {
                    val obraId = resource.data?.id
                    if (obraId != null && viewModel.personal.isNotEmpty()) {
                        guardarPersonalYSalir(obraId)
                    } else {
                        Toast.makeText(requireContext(), R.string.registrar_obra_exito, Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }
                }
                is Resource.Error -> Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun mostrarFechas() {
        binding.etFechaInicioObra.setText(Formatos.fecha(viewModel.fechaInicio))
        binding.etFechaFinObra.setText(Formatos.fecha(viewModel.fechaFin))
        if (viewModel.fechaInicio.isNotEmpty()) binding.tilFechaInicioObra.error = null
        if (viewModel.fechaFin.isNotEmpty()) binding.tilFechaFinObra.error = null
    }

    private fun agregarPersonalDesdeCampos() {
        val nombre = binding.etNombrePersonalExterno.text.toString().trim()
        val cc = binding.etCcPersonalExterno.text.toString().trim()
        binding.tilNombrePersonalExterno.error =
            if (nombre.isEmpty()) getString(R.string.registrar_obra_nombre_obligatorio) else null
        binding.tilCcPersonalExterno.error =
            if (cc.isEmpty()) getString(R.string.registrar_obra_cedula_obligatoria) else null
        if (nombre.isEmpty() || cc.isEmpty()) return

        viewModel.personal += PersonalObraDTO(null, nombre, cc, binding.switchArlPersonalExterno.isChecked, 0L)
        binding.etNombrePersonalExterno.text?.clear()
        binding.etCcPersonalExterno.text?.clear()
        binding.switchArlPersonalExterno.isChecked = false
        binding.etNombrePersonalExterno.requestFocus()
        mostrarPersonal()
    }

    /** Un chip por trabajador agregado; la "x" lo quita de la lista. */
    private fun mostrarPersonal() {
        binding.chipsPersonal.removeAllViews()
        viewModel.personal.forEach { p ->
            binding.chipsPersonal.addView(Chip(requireContext()).apply {
                text = getString(R.string.registrar_obra_trabajador_fila, p.nombreTrabajador, p.cedula)
                chipIcon = AppCompatResources.getDrawable(
                    requireContext(),
                    if (p.arlEstado) R.drawable.ic_check_circle else R.drawable.ic_error_outline
                )
                isChipIconVisible = true
                contentDescription = "$text, ${getString(Estados.arl(p.arlEstado).etiqueta)}"
                isCloseIconVisible = true
                closeIconContentDescription = getString(R.string.registrar_obra_quitar, p.nombreTrabajador)
                setOnCloseIconClickListener {
                    viewModel.personal.remove(p)
                    mostrarPersonal()
                }
            })
        }
        binding.chipsPersonal.visibility = if (viewModel.personal.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun guardarPersonalYSalir(obraId: Long) {
        binding.progressRegistrarObra.visibility = View.VISIBLE
        binding.btnGuardarObra.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            // "Nombre: motivo" de los trabajadores que no se pudieron guardar
            val fallidos = mutableListOf<String>()
            viewModel.personal.forEach { dto ->
                try {
                    val response = RetrofitClient.instance.createPersonalObra(dto.copy(obraId = obraId))
                    if (!response.isSuccessful) {
                        fallidos += "${dto.nombreTrabajador}: ${RetrofitClient.mensajeDeError(response, getString(R.string.registrar_obra_error))}"
                    }
                } catch (_: Exception) {
                    fallidos += "${dto.nombreTrabajador}: ${getString(R.string.registrar_obra_sin_conexion)}"
                }
            }
            binding.progressRegistrarObra.visibility = View.GONE

            if (fallidos.isEmpty()) {
                Toast.makeText(requireContext(), R.string.registrar_obra_exito_personal, Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.registrar_obra_avisos_titulo)
                    .setMessage(getString(R.string.registrar_obra_avisos_mensaje, fallidos.joinToString("\n")))
                    .setPositiveButton(R.string.btn_entendido) { _, _ -> findNavController().popBackStack() }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    /**
     * Calendario que no deja elegir días anteriores a hoy (ni a [minimo], "yyyy-MM-dd").
     * Devuelve la fecha en el formato del backend.
     */
    private fun elegirFecha(actual: String, minimo: String?, alElegir: (String) -> Unit) {
        val inicial = Calendar.getInstance()
        aCalendario(actual)?.let { inicial.time = it.time }
        val picker = DatePickerDialog(requireContext(), { _, y, m, d ->
            alElegir(String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d))
        }, inicial.get(Calendar.YEAR), inicial.get(Calendar.MONTH), inicial.get(Calendar.DAY_OF_MONTH))
        val hoy = Calendar.getInstance().timeInMillis - 1000
        picker.datePicker.minDate = maxOf(hoy, aCalendario(minimo)?.timeInMillis ?: 0L)
        picker.show()
    }

    private fun aCalendario(iso: String?): Calendar? {
        val partes = iso?.split("-")?.mapNotNull { it.toIntOrNull() } ?: return null
        if (partes.size != 3) return null
        return Calendar.getInstance().apply { set(partes[0], partes[1] - 1, partes[2], 0, 0, 0) }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
