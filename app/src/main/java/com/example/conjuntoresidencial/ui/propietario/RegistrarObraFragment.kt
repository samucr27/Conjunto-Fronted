package com.example.conjuntoresidencial.ui.propietario

import android.app.DatePickerDialog
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import com.example.conjuntoresidencial.ui.comun.ponerTitulo
import com.example.conjuntoresidencial.ui.obras.CertificadoArl
import com.example.conjuntoresidencial.ui.obras.leerCertificado
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
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.controlesFlotantes
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.util.limpiarErrorAlEscribir
import com.example.conjuntoresidencial.ui.comun.aviso
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class RegistrarObraFragment : Fragment() {

    private var _binding: FragmentRegistrarObraBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RegistrarObraViewModel by viewModels()
    // Botón de guardar: flota en la capa de MainActivity, encima del difuminado
    private var btnGuardar: ExtendedFloatingActionButton? = null

    // Valores que acepta el backend; en pantalla se muestran con su etiqueta ("En proceso")
    private val estados = listOf("EN_PROCESO", "FINALIZADA")

    // P5: editar una obra (sin la sección de personal, que se maneja en el detalle) o registrar una del conjunto
    private val obraEditada get() = arguments?.getLong(ARG_OBRA)?.takeIf { it > 0 }
    private val delConjunto get() = arguments?.getBoolean(ARG_CONJUNTO) == true
    private val esAdmin get() = resources.getBoolean(R.bool.app_admin)

    private val elegirCertificado = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val certificado = uri?.let { leerCertificado(it) } ?: return@registerForActivityResult
        viewModel.certificadoPendiente = certificado
        mostrarCertificadoPendiente()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegistrarObraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        when {
            obraEditada != null -> configurarEncabezado(binding.encabezado, R.string.obra_editar_titulo, R.string.obra_editar_subtitulo)
            delConjunto -> configurarEncabezado(binding.encabezado, R.string.obra_conjunto_titulo, R.string.obra_conjunto_subtitulo)
            else -> configurarEncabezado(binding.encabezado, R.string.registrar_obra_titulo, R.string.registrar_obra_subtitulo)
        }
        val aptoId: Long? = if (delConjunto) null else SessionManager(requireContext()).getApartamentoId()
        binding.cardPersonalObra.isVisible = obraEditada == null
        obraEditada?.let { if (viewModel.original == null) cargarObra(it) }

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
        binding.btnArlPersonalExterno.setOnClickListener { elegirCertificado.launch(CertificadoArl.TIPOS) }
        mostrarCertificadoPendiente()
        listOf(binding.tilDescripcionObra, binding.tilNombrePersonalExterno, binding.tilCcPersonalExterno)
            .forEach { it.limpiarErrorAlEscribir() }

        // Botón de guardar flotante, nítido encima del difuminado de abajo
        btnGuardar = controlesFlotantes?.mostrarBotonFlotante(R.string.registrar_obra_guardar, R.drawable.ic_check_circle) {
            val descripcion = binding.etDescripcionObra.text.toString().trim()
            binding.tilDescripcionObra.error =
                if (descripcion.isEmpty()) getString(R.string.registrar_obra_descripcion_obligatoria) else null
            binding.tilFechaInicioObra.error =
                if (viewModel.fechaInicio.isEmpty()) getString(R.string.registrar_obra_fecha_obligatoria) else null
            binding.tilFechaFinObra.error =
                if (viewModel.fechaFin.isEmpty()) getString(R.string.registrar_obra_fecha_obligatoria) else null
            if (descripcion.isEmpty() || viewModel.fechaInicio.isEmpty() || viewModel.fechaFin.isEmpty()) {
                return@mostrarBotonFlotante
            }
            viewModel.registrarObra(descripcion, aptoId, getString(R.string.registrar_obra_error))
        }

        viewModel.obraState.observe(viewLifecycleOwner) { resource ->
            val cargando = resource is Resource.Loading
            binding.progressRegistrarObra.visibility = if (cargando) View.VISIBLE else View.GONE
            btnGuardar?.isEnabled = !cargando
            when (resource) {
                is Resource.Loading -> Unit
                is Resource.Success -> {
                    val obraId = resource.data?.id
                    if (obraId != null && viewModel.personal.isNotEmpty() && obraEditada == null) {
                        guardarPersonalYSalir(obraId)
                    } else {
                        Toast.makeText(requireContext(), when {
                            obraEditada != null -> R.string.obra_guardada
                            esAdmin -> R.string.registrar_obra_exito
                            else -> R.string.registrar_obra_exito_pendiente
                        }, Toast.LENGTH_LONG).show()
                        findNavController().popBackStack()
                    }
                }
                is Resource.Error -> aviso(resource.message, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    /** Editar: trae la obra y llena el formulario (una sola vez; al girar la pantalla queda en el ViewModel). */
    private fun cargarObra(id: Long) {
        binding.progressRegistrarObra.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val r = RetrofitClient.comun.getObra(id)
                val obra = r.body()
                if (!r.isSuccessful || obra == null) {
                    aviso(RetrofitClient.mensajeDeError(r, getString(R.string.obra_error_cargar)), Snackbar.LENGTH_LONG).show()
                    return@launch
                }
                viewModel.original = obra
                viewModel.fechaInicio = obra.fechaInicio
                viewModel.fechaFin = obra.fechaFinEstimada
                viewModel.estado = obra.estadoObra
                binding.etDescripcionObra.setText(obra.descripcionAdecuacion)
                binding.spinnerEstadoObra.setText(getString(Estados.obra(obra.estadoObra).etiqueta), false)
                mostrarFechas()
            } catch (e: Exception) {
                aviso(RetrofitClient.mensajeDeExcepcion(e), Snackbar.LENGTH_LONG).show()
            } finally {
                binding.progressRegistrarObra.visibility = View.GONE
            }
        }
    }

    private fun mostrarCertificadoPendiente() {
        val listo = viewModel.certificadoPendiente != null
        binding.btnArlPersonalExterno.setText(if (listo) R.string.arl_elegido else R.string.arl_adjuntar)
        binding.btnArlPersonalExterno.setIconResource(if (listo) R.drawable.ic_check_circle else R.drawable.ic_add)
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

        val certificado = viewModel.certificadoPendiente
        viewModel.personal += TrabajadorNuevo(PersonalObraDTO(null, nombre, cc, certificado != null, 0L), certificado)
        viewModel.certificadoPendiente = null
        mostrarCertificadoPendiente()
        binding.etNombrePersonalExterno.text?.clear()
        binding.etCcPersonalExterno.text?.clear()
        binding.etNombrePersonalExterno.requestFocus()
        mostrarPersonal()
    }

    /** Un chip por trabajador agregado; la "x" lo quita de la lista. */
    private fun mostrarPersonal() {
        binding.chipsPersonal.removeAllViews()
        viewModel.personal.forEach { t ->
            val p = t.dto
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
                    viewModel.personal.remove(t)
                    mostrarPersonal()
                }
            })
        }
        binding.chipsPersonal.visibility = if (viewModel.personal.isEmpty()) View.GONE else View.VISIBLE
        // Resumen del encabezado: "2 trabajadores agregados"
        val n = viewModel.personal.size
        binding.encabezado.ponerInfo(
            if (n == 0) null else resources.getQuantityString(R.plurals.resumen_trabajadores_agregados, n, n)
        )
    }

    private fun guardarPersonalYSalir(obraId: Long) {
        binding.progressRegistrarObra.visibility = View.VISIBLE
        btnGuardar?.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            // "Nombre: motivo" de los trabajadores que no se pudieron guardar
            val fallidos = mutableListOf<String>()
            viewModel.personal.forEach { (dto, certificado) ->
                try {
                    val response = RetrofitClient.comun.createPersonalObra(dto.copy(obraId = obraId))
                    val id = response.body()?.id
                    if (!response.isSuccessful || id == null) {
                        fallidos += "${dto.nombreTrabajador}: ${RetrofitClient.mensajeDeError(response, getString(R.string.registrar_obra_error))}"
                    } else if (certificado != null &&
                        !RetrofitClient.comun.subirCertificadoArl(id, certificado.parte()).isSuccessful) {
                        fallidos += "${dto.nombreTrabajador}: ${getString(R.string.registrar_obra_arl_fallo)}"
                    }
                } catch (_: Exception) {
                    fallidos += "${dto.nombreTrabajador}: ${getString(R.string.registrar_obra_sin_conexion)}"
                }
            }
            binding.progressRegistrarObra.visibility = View.GONE

            if (fallidos.isEmpty()) {
                Toast.makeText(requireContext(),
                    if (esAdmin) R.string.registrar_obra_exito_personal else R.string.registrar_obra_exito_pendiente,
                    Toast.LENGTH_LONG).show()
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

    override fun onDestroyView() { super.onDestroyView(); _binding = null; btnGuardar = null }

    companion object {
        const val ARG_OBRA = "obraId"
        const val ARG_CONJUNTO = "conjunto"
    }
}
