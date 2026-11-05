package com.example.conjuntoresidencial.ui.auth

import android.content.Context
import android.content.DialogInterface
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.DialogElegirConjuntoBinding
import com.example.conjuntoresidencial.model.ConjuntoDTO
import com.example.conjuntoresidencial.model.UbicacionConjuntos
import com.example.conjuntoresidencial.seguridad.AccesoBiometrico
import com.example.conjuntoresidencial.util.ConjuntoElegido
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * Guarda [nuevo]. Si es otro conjunto, la huella guardada era de una cuenta del anterior: se quita
 * (con sesión también en el servidor), para que no intente entrar con ella en el nuevo.
 */
fun cambiarConjunto(contexto: Context, nuevo: ConjuntoDTO) {
    val anterior = ConjuntoElegido.id(contexto)
    if (anterior != null && anterior != nuevo.id) AccesoBiometrico.desactivar(contexto)
    ConjuntoElegido.guardar(contexto, nuevo)
}

/**
 * Diálogo con los tres menús en cascada (lo usa Cuenta). [ubicaciones] evita pedirlas otra vez;
 * [soloConBoton]: tocar afuera no lo cierra (sí "Ahora no").
 */
fun Fragment.elegirConjunto(
    ubicaciones: List<UbicacionConjuntos>?,
    soloConBoton: Boolean = false,
    alElegir: (ConjuntoDTO) -> Unit
) {
    val vista = DialogElegirConjuntoBinding.inflate(layoutInflater)
    var elegido: ConjuntoDTO? = null
    val dialogo = MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.conjunto_titulo)
        .setMessage(R.string.conjunto_mensaje)
        .setView(vista.root)
        .setCancelable(!soloConBoton)
        .setPositiveButton(R.string.conjunto_continuar) { _, _ -> elegido?.let(alElegir) }
        .setNegativeButton(if (soloConBoton) R.string.ahora_no else R.string.btn_cancelar, null)
        .show()
    val continuar = dialogo.getButton(DialogInterface.BUTTON_POSITIVE)
    continuar.isEnabled = false
    controlarSelectorConjunto(vista, ubicaciones, activo = { dialogo.isShowing }) {
        elegido = it
        continuar.isEnabled = it != null
    }
}

/**
 * Los tres menús en cascada de [vista]: departamento, municipio y conjunto. Lo ya elegido sale
 * marcado, y si en un menú hay una sola opción se marca sola. [alMarcar] recibe el conjunto
 * elegido (null mientras falta). [activo] dice si la vista sigue en pantalla cuando llega una
 * respuesta. P8 bloque 2: lo usan el diálogo y la pantalla de elegir conjunto.
 */
fun Fragment.controlarSelectorConjunto(
    vista: DialogElegirConjuntoBinding,
    ubicaciones: List<UbicacionConjuntos>?,
    activo: () -> Boolean,
    alMarcar: (ConjuntoDTO?) -> Unit
) {
    val contexto = requireContext()
    val actual = ConjuntoElegido.actual(contexto)
    // Si se cambia de municipio antes de que llegue la lista anterior, esa se descarta
    var peticion = 0

    fun error(mensaje: String?) {
        vista.tvError.text = mensaje
        vista.tvError.isVisible = mensaje != null
    }
    fun opciones(campo: AutoCompleteTextView, items: List<String>) =
        campo.setAdapter(ArrayAdapter(contexto, android.R.layout.simple_list_item_1, items))
    fun marcar(c: ConjuntoDTO?) {
        alMarcar(c)
        vista.tvDireccion.text = c?.direccion
        vista.tvDireccion.isVisible = !c?.direccion.isNullOrBlank()
    }
    fun vaciarConjunto() {
        peticion++
        marcar(null)
        vista.etConjunto.setText("", false)
        vista.tilConjunto.isEnabled = false
    }

    fun cargarConjuntos(departamento: String, municipio: String) {
        vaciarConjunto()
        error(null)
        vista.progreso.isVisible = true
        val esta = ++peticion
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val respuesta = RetrofitClient.comun.conjuntos(departamento, municipio)
                val lista = respuesta.body()
                if (!activo() || esta != peticion) return@launch
                if (respuesta.isSuccessful && lista != null) {
                    opciones(vista.etConjunto, lista.map { it.nombre })
                    vista.tilConjunto.isEnabled = lista.isNotEmpty()
                    vista.etConjunto.setOnItemClickListener { _, _, i, _ -> marcar(lista[i]) }
                    (lista.firstOrNull { it.id == actual?.id } ?: lista.singleOrNull())?.let {
                        vista.etConjunto.setText(it.nombre, false)
                        marcar(it)
                    }
                    if (lista.isEmpty()) error(getString(R.string.conjunto_sin_conjuntos))
                } else {
                    error(RetrofitClient.mensajeDeError(respuesta, getString(R.string.conjunto_error)))
                }
            } catch (e: Exception) {
                if (activo() && esta == peticion) error(RetrofitClient.mensajeDeExcepcion(e))
            }
            if (activo() && esta == peticion) vista.progreso.isVisible = false
        }
    }

    fun elegirDepartamento(u: UbicacionConjuntos) {
        vaciarConjunto()
        opciones(vista.etMunicipio, u.municipios)
        vista.etMunicipio.setText("", false)
        vista.tilMunicipio.isEnabled = true
        vista.etMunicipio.setOnItemClickListener { _, _, i, _ -> cargarConjuntos(u.departamento, u.municipios[i]) }
        (u.municipios.firstOrNull { it == actual?.municipio } ?: u.municipios.singleOrNull())?.let {
            vista.etMunicipio.setText(it, false)
            cargarConjuntos(u.departamento, it)
        }
    }

    fun mostrar(lista: List<UbicacionConjuntos>) {
        if (lista.isEmpty()) {
            error(getString(R.string.conjunto_sin_conjuntos))
            return
        }
        opciones(vista.etDepartamento, lista.map { it.departamento })
        vista.etDepartamento.setOnItemClickListener { _, _, i, _ -> elegirDepartamento(lista[i]) }
        (lista.firstOrNull { it.departamento == actual?.departamento } ?: lista.singleOrNull())?.let {
            vista.etDepartamento.setText(it.departamento, false)
            elegirDepartamento(it)
        }
    }

    if (ubicaciones != null) {
        mostrar(ubicaciones)
        return
    }
    vista.progreso.isVisible = true
    viewLifecycleOwner.lifecycleScope.launch {
        try {
            val respuesta = RetrofitClient.comun.ubicacionesConjuntos()
            val lista = respuesta.body()
            if (!activo()) return@launch
            vista.progreso.isVisible = false
            when {
                respuesta.code() == 404 -> error(getString(R.string.conjunto_no_disponible))
                respuesta.isSuccessful && lista != null -> mostrar(lista)
                else -> error(RetrofitClient.mensajeDeError(respuesta, getString(R.string.conjunto_error)))
            }
        } catch (e: Exception) {
            if (activo()) {
                vista.progreso.isVisible = false
                error(RetrofitClient.mensajeDeExcepcion(e))
            }
        }
    }
}
