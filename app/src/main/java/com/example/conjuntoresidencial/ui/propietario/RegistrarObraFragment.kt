package com.example.conjuntoresidencial.ui.propietario

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.FragmentRegistrarObraBinding
import com.example.conjuntoresidencial.model.PersonalObraDTO
import com.example.conjuntoresidencial.util.Resource
import com.example.conjuntoresidencial.util.SessionManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.example.conjuntoresidencial.R
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
                        mostrarDialogAgregarPersonal(obraId)
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

    private fun mostrarDialogAgregarPersonal(obraId: Long) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_agregar_personal_obra, null)
        AlertDialog.Builder(requireContext())
            .setTitle("Agregar Personal (opcional)")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = dialogView.findViewById<TextInputEditText>(R.id.etNombrePersonal).text.toString().trim()
                val cedula = dialogView.findViewById<TextInputEditText>(R.id.etCedulaPersonal).text.toString().trim()
                val arl = dialogView.findViewById<CheckBox>(R.id.cbArlPersonal).isChecked
                if (nombre.isNotBlank() && cedula.isNotBlank()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            RetrofitClient.instance.createPersonalObra(
                                PersonalObraDTO(null, nombre, cedula, arl, obraId)
                            )
                        } catch (_: Exception) {}
                        withContext(Dispatchers.Main) {
                            Snackbar.make(binding.root, "Obra y personal registrados", Snackbar.LENGTH_SHORT).show()
                            findNavController().popBackStack()
                        }
                    }
                } else {
                    Snackbar.make(binding.root, "Obra registrada exitosamente", Snackbar.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            }
            .setNegativeButton("Omitir") { _, _ ->
                Snackbar.make(binding.root, "Obra registrada exitosamente", Snackbar.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            .show()
    }

    private fun showDatePicker(onDate: (String) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, y, m, d ->
            onDate(String.format("%04d-%02d-%02d", y, m + 1, d))
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}