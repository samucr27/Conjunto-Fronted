package com.example.conjuntoresidencial.ui.propietario

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.FragmentRegistrarObraBinding
import com.example.conjuntoresidencial.model.PersonalObraDTO
import com.example.conjuntoresidencial.util.Resource
import com.example.conjuntoresidencial.util.SessionManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RegistrarObraFragment : Fragment() {

    private var _binding: FragmentRegistrarObraBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RegistrarObraViewModel by viewModels()
    private val personalList = mutableListOf<PersonalObraDTO>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegistrarObraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val aptoId = SessionManager(requireContext()).getApartamentoId()

        val estados = listOf("EN_PROCESO", "FINALIZADA")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, estados)
        binding.spinnerEstadoObra.setAdapter(adapter)
        binding.spinnerEstadoObra.setText(estados[0], false)

        // Date pickers bloqueando fechas anteriores a hoy
        binding.etFechaInicioObra.setOnClickListener {
            showDatePicker { binding.etFechaInicioObra.setText(it) }
        }
        binding.etFechaFinObra.setOnClickListener {
            showDatePicker { binding.etFechaFinObra.setText(it) }
        }

        binding.btnAgregarPersonal.setOnClickListener { agregarPersonalDesdeCampos() }

        binding.btnGuardarObra.setOnClickListener {
            val descripcion = binding.etDescripcionObra.text.toString().trim()
            val fechaInicio = binding.etFechaInicioObra.text.toString().trim()
            val fechaFin = binding.etFechaFinObra.text.toString().trim()
            val estado = binding.spinnerEstadoObra.text.toString()

            if (descripcion.isEmpty() || fechaInicio.isEmpty() || fechaFin.isEmpty()) {
                Snackbar.make(binding.root, "Complete todos los campos de la obra", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dInicio = fmt.parse(fechaInicio)
            val dFin = fmt.parse(fechaFin)
            if (dFin != null && dInicio != null && dFin.before(dInicio)) {
                Snackbar.make(binding.root, "La fecha fin no puede ser anterior a la fecha de inicio", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            viewModel.registrarObra(descripcion, fechaInicio, fechaFin, estado, aptoId)
        }

        viewModel.obraState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressRegistrarObra.visibility = View.VISIBLE
                    binding.btnGuardarObra.isEnabled = false
                }
                is Resource.Success -> {
                    binding.progressRegistrarObra.visibility = View.GONE
                    val obraId = resource.data?.id
                    if (obraId != null && personalList.isNotEmpty()) {
                        guardarPersonalYSalir(obraId)
                    } else {
                        Snackbar.make(binding.root, "Obra registrada exitosamente", Snackbar.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }
                }
                is Resource.Error -> {
                    binding.progressRegistrarObra.visibility = View.GONE
                    binding.btnGuardarObra.isEnabled = true
                    Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun agregarPersonalDesdeCampos() {
        val nombre = binding.etNombrePersonalExterno.text.toString().trim()
        val cc = binding.etCcPersonalExterno.text.toString().trim()
        val arl = binding.switchArlPersonalExterno.isChecked

        if (nombre.isEmpty() || cc.isEmpty()) {
            Snackbar.make(binding.root, "Ingresa nombre y cédula del trabajador", Snackbar.LENGTH_SHORT).show()
            return
        }

        personalList.add(PersonalObraDTO(null, nombre, cc, arl, 0L))
        binding.etNombrePersonalExterno.text?.clear()
        binding.etCcPersonalExterno.text?.clear()
        binding.switchArlPersonalExterno.isChecked = false

        val count = personalList.size
        binding.tvContadorPersonal.text = "$count trabajador${if (count != 1) "es" else ""} agregado${if (count != 1) "s" else ""}"
        binding.tvContadorPersonal.visibility = View.VISIBLE
        Snackbar.make(binding.root, "Trabajador agregado a la lista", Snackbar.LENGTH_SHORT).show()
    }

    private fun guardarPersonalYSalir(obraId: Long) {
        binding.progressRegistrarObra.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            personalList.forEach { dto ->
                try { RetrofitClient.instance.createPersonalObra(dto.copy(obraId = obraId)) }
                catch (_: Exception) {}
            }
            withContext(Dispatchers.Main) {
                binding.progressRegistrarObra.visibility = View.GONE
                Snackbar.make(binding.root, "Obra y personal registrados exitosamente", Snackbar.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }
    }

    // Bloquea fechas anteriores a hoy
    private fun showDatePicker(onDate: (String) -> Unit) {
        val hoy = Calendar.getInstance()
        val picker = DatePickerDialog(
            requireContext(), { _, y, m, d ->
                onDate(String.format("%04d-%02d-%02d", y, m + 1, d))
            },
            hoy.get(Calendar.YEAR), hoy.get(Calendar.MONTH), hoy.get(Calendar.DAY_OF_MONTH)
        )
        picker.datePicker.minDate = hoy.timeInMillis
        picker.show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}