package com.example.conjuntoresidencial.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FragmentRegistroBinding
import com.example.conjuntoresidencial.model.PersonaRegistro
import com.example.conjuntoresidencial.model.RegistroRequest
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.confirmar
import com.example.conjuntoresidencial.ui.cuenta.DocumentoFragment
import com.example.conjuntoresidencial.util.exigirTexto
import com.example.conjuntoresidencial.util.limpiarErrorAlEscribir
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * Registro del residente en 4 pasos (P5): torre y apto (el backend dice si queda como
 * propietario o arrendatario), datos personales con la aceptación de datos y términos,
 * personas del hogar y visitantes frecuentes (estos dos se pueden omitir). Al final queda
 * pendiente de aprobación.
 */
class RegistroFragment : Fragment() {
    private var _binding: FragmentRegistroBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RegistroViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegistroBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(binding.encabezado, R.string.registro_titulo, R.string.registro_descripcion)

        listOf(binding.tilTorreReg, binding.tilAptoReg, binding.tilNombres, binding.tilApellidos, binding.tilCedula,
            binding.tilTelefonoReg, binding.tilCorreo, binding.tilContrasenaReg, binding.tilConfirmarReg)
            .forEach { it.limpiarErrorAlEscribir() }
        binding.btnLeerDatos.setOnClickListener { leer(R.string.legal_privacidad, R.raw.privacidad) }
        binding.btnLeerTerminos.setOnClickListener { leer(R.string.legal_terminos, R.raw.terminos) }
        binding.btnAgregarPersona.setOnClickListener { pedirPersona(R.string.registro_agregar_persona, viewModel.personas) }
        binding.btnAgregarVisitante.setOnClickListener { pedirPersona(R.string.registro_agregar_visitante, viewModel.visitantes) }
        binding.btnSiguiente.setOnClickListener { siguiente() }
        binding.btnOmitir.setOnClickListener { avanzarDesdeListas() }
        binding.btnAtrasPaso.setOnClickListener { volver() }
        binding.btnIrLogin.setOnClickListener { findNavController().popBackStack() }

        // "Atrás" del celular vuelve al paso anterior; en el primero o al terminar, sale
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.paso in 2..4) volver() else findNavController().popBackStack()
            }
        })

        viewModel.cargando.observe(viewLifecycleOwner) { cargando ->
            binding.progresoRegistro.visibility = if (cargando) View.VISIBLE else View.GONE
            binding.btnSiguiente.isEnabled = !cargando
            binding.btnOmitir.isEnabled = !cargando
        }
        viewModel.resultado.observe(viewLifecycleOwner) { evento ->
            evento.obtenerSiNoManejado()?.let { r ->
                r.onSuccess { paso -> mostrarPaso(paso) }
                    .onFailure { aviso(it.message ?: getString(R.string.error_red)).show() }
            }
        }
        mostrarPaso(viewModel.paso)
    }

    private fun mostrarPaso(paso: Int) {
        viewModel.paso = paso
        val vistas = listOf(binding.paso1, binding.paso2, binding.paso3, binding.paso4, binding.paso5)
        vistas.forEachIndexed { i, v -> v.visibility = if (i + 1 == paso) View.VISIBLE else View.GONE }
        binding.tvPaso.text = getString(R.string.registro_paso, paso.coerceAtMost(4))
        binding.progresoPasos.setProgressCompat(paso.coerceAtMost(4) * 25, true)
        binding.grupoProgreso.visibility = if (paso == 5) View.GONE else View.VISIBLE
        binding.barraBotones.visibility = if (paso == 5) View.GONE else View.VISIBLE
        binding.btnAtrasPaso.visibility = if (paso in 2..4) View.VISIBLE else View.GONE
        binding.btnOmitir.visibility = if (paso in 3..4) View.VISIBLE else View.GONE
        binding.btnSiguiente.setText(if (paso == 4) R.string.registro_finalizar else R.string.registro_siguiente)
        if (paso == 2) {
            binding.tvRolRegistro.text = getString(
                if (viewModel.rol == "ARRENDATARIO") R.string.registro_como_arrendatario else R.string.registro_como_propietario,
                viewModel.torre, viewModel.apto
            )
        }
        if (paso == 3) pintarLista(binding.listaPersonas, binding.tvSinPersonas, viewModel.personas)
        if (paso == 4) pintarLista(binding.listaVisitantes, binding.tvSinVisitantes, viewModel.visitantes)
        if (paso == 5) {
            binding.tvMensajeFinal.setText(
                if (viewModel.recibido?.requierePropietario == true) R.string.registro_listo_arrendatario
                else R.string.registro_listo_propietario
            )
        }
        binding.scrollRegistro.scrollTo(0, 0)
    }

    private fun siguiente() {
        when (viewModel.paso) {
            1 -> {
                val ok = listOf(
                    binding.tilTorreReg.exigirTexto(getString(R.string.login_falta_torre)),
                    binding.tilAptoReg.exigirTexto(getString(R.string.login_falta_apartamento))
                ).all { it }
                if (ok) viewModel.verificar(texto(binding.etTorreReg), texto(binding.etAptoReg))
            }
            2 -> if (validarDatos()) mostrarPaso(3)
            else -> avanzarDesdeListas()
        }
    }

    private fun avanzarDesdeListas() {
        if (viewModel.paso == 3) mostrarPaso(4) else viewModel.enviar()
    }

    private fun volver() {
        if (viewModel.paso > 1) mostrarPaso(viewModel.paso - 1)
    }

    private fun validarDatos(): Boolean {
        val obligatorios = listOf(
            binding.tilNombres to R.string.registro_falta_nombres,
            binding.tilApellidos to R.string.registro_falta_apellidos,
            binding.tilCedula to R.string.registro_falta_cedula,
            binding.tilTelefonoReg to R.string.registro_falta_telefono,
            binding.tilCorreo to R.string.registro_falta_correo
        ).map { (til, error) -> til.exigirTexto(getString(error)) }.all { it }
        val correo = texto(binding.etCorreo)
        val contrasena = binding.etContrasenaReg.text?.toString().orEmpty()
        var ok = obligatorios
        if (correo.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            binding.tilCorreo.error = getString(R.string.registro_correo_invalido); ok = false
        }
        if (contrasena.length < 6) { binding.tilContrasenaReg.error = getString(R.string.error_contrasena_corta); ok = false }
        if (contrasena != binding.etConfirmarReg.text?.toString()) {
            binding.tilConfirmarReg.error = getString(R.string.registro_no_coinciden); ok = false
        }
        if (!binding.cbDatos.isChecked || !binding.cbTerminos.isChecked) {
            aviso(R.string.registro_falta_aceptar).show(); ok = false
        }
        if (!ok) return false
        viewModel.datos = RegistroRequest(
            torre = viewModel.torre, apto = viewModel.apto, nombres = texto(binding.etNombres),
            apellidos = texto(binding.etApellidos), cedula = texto(binding.etCedula), telefono = texto(binding.etTelefonoReg),
            correo = correo, contrasena = contrasena, aceptaDatos = true, aceptaTerminos = true
        )
        return true
    }

    /** Persona del hogar o visitante: nombre (obligatorio), documento y teléfono. */
    private fun pedirPersona(@StringRes titulo: Int, lista: MutableList<PersonaRegistro>) {
        val vista = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_agregar_residente, null)
        vista.findViewById<View>(R.id.tilTipoResidente).visibility = View.GONE
        val tilNombre = vista.findViewById<TextInputLayout>(R.id.tilNombreResidente)
        val dialogo = MaterialAlertDialogBuilder(requireContext())
            .setTitle(titulo)
            .setView(vista)
            .setPositiveButton(R.string.btn_guardar, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            fun campo(id: Int) = vista.findViewById<TextInputEditText>(id).text?.toString()?.trim().orEmpty()
            val nombre = campo(R.id.etNombreResidente)
            if (nombre.isBlank()) {
                tilNombre.error = getString(R.string.residente_nombre_obligatorio)
                return@setOnClickListener
            }
            lista += PersonaRegistro(nombre, campo(R.id.etIdentificacionResidente).ifBlank { null }, campo(R.id.etTelefonoResidente).ifBlank { null })
            dialogo.dismiss()
            mostrarPaso(viewModel.paso)
        }
    }

    /** Cada persona en una fila; tocarla la quita. */
    private fun pintarLista(contenedor: LinearLayout, vacio: TextView, lista: MutableList<PersonaRegistro>) {
        contenedor.removeAllViews()
        vacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
        lista.forEach { p ->
            val fila = layoutInflater.inflate(android.R.layout.simple_list_item_2, contenedor, false)
            fila.findViewById<TextView>(android.R.id.text1).text = p.nombreCompleto
            fila.findViewById<TextView>(android.R.id.text2).text =
                listOfNotNull(p.identificacion?.let { getString(R.string.registro_doc, it) }, p.telefono).joinToString(" · ")
                    .ifBlank { getString(R.string.registro_tocar_quitar) }
            fila.setOnClickListener {
                confirmar(getString(R.string.residente_quitar_titulo, p.nombreCompleto), null, R.string.residente_quitar) {
                    lista.remove(p)
                    mostrarPaso(viewModel.paso)
                }
            }
            contenedor.addView(fila)
        }
    }

    private fun leer(@StringRes titulo: Int, @RawRes texto: Int) {
        findNavController().navigate(R.id.documentoFragment,
            bundleOf(DocumentoFragment.ARG_TITULO to titulo, DocumentoFragment.ARG_TEXTO to texto))
    }

    private fun texto(campo: TextInputEditText) = campo.text?.toString()?.trim().orEmpty()

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
