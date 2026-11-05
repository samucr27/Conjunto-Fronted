package com.example.conjuntoresidencial.ui.recepcion

import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.api.instance
import com.example.conjuntoresidencial.databinding.FragmentTurnoBinding
import com.example.conjuntoresidencial.model.AbrirTurno
import com.example.conjuntoresidencial.model.GuardiaEnTurno
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.util.SessionManager
import com.example.conjuntoresidencial.util.confirmarCerrarSesion
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

/**
 * P7 bloque 4: turno de la recepción. Después del login de la cuenta de portería, y cada vez que
 * la app se bloquea por inactividad, el guardia toca su nombre y escribe su PIN; el servidor
 * devuelve un token con su turno y las visitas que registre llevan su nombre.
 */
class TurnoFragment : Fragment() {

    private var _binding: FragmentTurnoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTurnoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(binding.encabezado, R.string.turno_titulo, R.string.turno_descripcion, atras = false)
        SessionManager(requireContext()).getGuardia()?.let { binding.encabezado.ponerInfo(getString(R.string.turno_anterior, it)) }
        binding.btnCerrarSesion.setOnClickListener { confirmarCerrarSesion() }
        // "Atrás" no cierra la app: al cerrarla se borra la sesión y habría que volver a escribir la contraseña del puesto
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { requireActivity().moveTaskToBack(true) }
        cargar()
    }

    private fun cargar() {
        binding.progreso.visibility = View.VISIBLE
        binding.tvMensaje.visibility = View.GONE
        binding.btnReintentar.visibility = View.GONE
        binding.listaGuardias.removeAllViews()
        viewLifecycleOwner.lifecycleScope.launch {
            val resultado = try {
                val r = RetrofitClient.instance.guardiasDelTurno()
                if (r.isSuccessful) Result.success(r.body().orEmpty())
                else Result.failure(Exception(RetrofitClient.mensajeDeError(r, getString(R.string.turno_error_cargar))))
            } catch (e: Exception) {
                Result.failure(Exception(RetrofitClient.mensajeDeExcepcion(e)))
            }
            if (_binding == null) return@launch
            binding.progreso.visibility = View.GONE
            resultado.onSuccess(::mostrar).onFailure { e ->
                mensaje(e.message ?: getString(R.string.turno_error_cargar), R.string.reintentar) { cargar() }
            }
        }
    }

    private fun mostrar(guardias: List<GuardiaEnTurno>) {
        if (guardias.isEmpty()) {
            // Sin guardias registrados la recepción funciona como antes, sin turno ni PIN
            binding.tvPregunta.visibility = View.GONE
            mensaje(getString(R.string.turno_sin_guardias), R.string.turno_continuar_sin_turno) { irAPorteria() }
            return
        }
        binding.tvPregunta.visibility = View.VISIBLE
        val margen = (8 * resources.displayMetrics.density).toInt()
        guardias.forEach { g ->
            val boton = MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = g.nombre
                minHeight = (56 * resources.displayMetrics.density).toInt()
                setIconResource(R.drawable.ic_person)
                setOnClickListener { pedirPin(g) }
            }
            binding.listaGuardias.addView(boton, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = margen })
        }
    }

    private fun mensaje(texto: String, boton: Int, accion: () -> Unit) {
        binding.tvMensaje.text = texto
        binding.tvMensaje.visibility = View.VISIBLE
        binding.btnReintentar.setText(boton)
        binding.btnReintentar.setOnClickListener { accion() }
        binding.btnReintentar.visibility = View.VISIBLE
    }

    /** El PIN no se muestra; el diálogo sigue abierto si no coincide. */
    private fun pedirPin(g: GuardiaEnTurno) {
        val contexto = requireContext()
        val campo = TextInputEditText(contexto).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            filters = arrayOf(InputFilter.LengthFilter(6))
            imeOptions = EditorInfo.IME_ACTION_DONE
        }
        val til = TextInputLayout(contexto).apply {
            hint = getString(R.string.turno_pin)
            endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
            addView(campo)
            val margen = (24 * resources.displayMetrics.density).toInt()
            setPadding(margen, margen / 2, margen, 0)
        }
        val dialogo = MaterialAlertDialogBuilder(contexto)
            .setTitle(g.nombre)
            .setView(til)
            .setPositiveButton(R.string.turno_abrir, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
        val abrir = dialogo.getButton(AlertDialog.BUTTON_POSITIVE)
        campo.setOnEditorActionListener { _, accion, _ -> (accion == EditorInfo.IME_ACTION_DONE).also { if (it) abrir.performClick() } }
        campo.requestFocus()
        abrir.setOnClickListener {
            val pin = campo.text?.toString().orEmpty()
            if (pin.length < 4) {
                til.error = getString(R.string.turno_pin_corto)
                return@setOnClickListener
            }
            abrir.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                val error = try {
                    val r = RetrofitClient.instance.abrirTurno(AbrirTurno(g.id, pin))
                    val sesion = r.body()
                    if (r.isSuccessful && sesion != null) {
                        SessionManager(contexto).guardar(sesion)
                        null
                    } else RetrofitClient.mensajeDeError(r, getString(R.string.turno_error_abrir))
                } catch (e: Exception) {
                    RetrofitClient.mensajeDeExcepcion(e)
                }
                abrir.isEnabled = true
                if (error == null) {
                    dialogo.dismiss()
                    if (_binding != null) irAPorteria()
                } else {
                    til.error = error
                    campo.setText("")
                }
            }
        }
    }

    private fun irAPorteria() {
        // P8 bloque 5: el turno empieza en el inicio de la recepción
        findNavController().navigate(R.id.recepcionInicioFragment, null, NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
