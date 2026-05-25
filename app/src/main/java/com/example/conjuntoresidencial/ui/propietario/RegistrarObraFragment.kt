package com.example.conjuntoresidencial.ui.propietario

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.R
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

        binding.etFechaInicioObra.setOnClickListener { showDatePicker { binding.etFechaInicioObra.setText(it) } }
        binding.etFechaFinObra.setOnClickListener { showDatePicker { binding.etFechaFinObra.setText(it) } }

        binding.btnGuardarObra.setOnClickListener {
            val descripcion = binding.etDescripcionObra.text.toString().trim()
            val fechaInicio = binding.etFechaInicioObra.text.toString().trim()
            val fechaFin = binding.etFechaFinObra.text.toString().trim()
            val estado = binding.spinnerEstadoObra.text.toString()

            if (descripcion.isEmpty() || fechaInicio.isEmpty() || fechaFin.isEmpty()) {
                Snackbar.make(binding.root, "Complete todos los campos", Snackbar.LENGTH_SHORT).show()
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
                    if (obraId != null) {
                        cargarYMostrarPersonal(obraId)
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

    private fun cargarYMostrarPersonal(obraId: Long) {
        binding.progressRegistrarObra.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getTodoElPersonal()
                withContext(Dispatchers.Main) {
                    binding.progressRegistrarObra.visibility = View.GONE
                    if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                        mostrarDialogSeleccionarPersonal(obraId, response.body()!!)
                    } else {
                        // No hay personal registrado, terminar directamente
                        Snackbar.make(binding.root, "Obra registrada. No hay personal disponible.", Snackbar.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressRegistrarObra.visibility = View.GONE
                    Snackbar.make(binding.root, "Obra registrada exitosamente", Snackbar.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            }
        }
    }

    private fun mostrarDialogSeleccionarPersonal(obraId: Long, listaPersonal: List<PersonalObraDTO>) {
        val nombres = listaPersonal.map { it.nombreTrabajador }.toTypedArray()
        val seleccionados = BooleanArray(listaPersonal.size) { false }

        AlertDialog.Builder(requireContext())
            .setTitle("Seleccionar Personal de Obra")
            .setMultiChoiceItems(nombres, seleccionados) { _, index, isChecked ->
                seleccionados[index] = isChecked
            }
            .setPositiveButton("Asignar") { _, _ ->
                val personalElegido = listaPersonal.filterIndexed { index, _ -> seleccionados[index] }
                if (personalElegido.isEmpty()) {
                    Snackbar.make(binding.root, "Obra registrada. Sin personal asignado.", Snackbar.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                } else {
                    asignarPersonalAObra(obraId, personalElegido)
                }
            }
            .setNegativeButton("Omitir") { _, _ ->
                Snackbar.make(binding.root, "Obra registrada exitosamente", Snackbar.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            .show()
    }

    private fun asignarPersonalAObra(obraId: Long, personalElegido: List<PersonalObraDTO>) {
        binding.progressRegistrarObra.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            var exito = true
            for (persona in personalElegido) {
                try {
                    // Se crea una copia vinculada a esta obra
                    RetrofitClient.instance.createPersonalObra(
                        PersonalObraDTO(
                            id = null,
                            nombreTrabajador = persona.nombreTrabajador,
                            cedula = persona.cedula,
                            arlEstado = persona.arlEstado,
                            obraId = obraId
                        )
                    )
                } catch (e: Exception) {
                    exito = false
                }
            }
            withContext(Dispatchers.Main) {
                binding.progressRegistrarObra.visibility = View.GONE
                val msg = if (exito) "Obra y personal asignados correctamente"
                else "Obra registrada, algunos errores al asignar personal"
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }
    }

    private fun showDatePicker(onDate: (String) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, y, m, d ->
            onDate(String.format("%04d-%02d-%02d", y, m + 1, d))
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}