package com.example.conjuntoresidencial.ui.auth

import android.content.Context
import android.content.DialogInterface
import android.view.View
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * P7: en el login, [boton] muestra el conjunto elegido y permite cambiarlo. Si aún no hay
 * ninguno, el diálogo se abre solo. Con un servidor anterior a P7 (404: un solo conjunto) el botón
 * se oculta y todo sigue como antes.
 */
fun Fragment.prepararConjunto(boton: MaterialButton) {
    val contexto = requireContext()
    fun pintar() {
        val c = ConjuntoElegido.actual(contexto)
        boton.text = c?.let { if (it.municipio.isBlank()) it.nombre else getString(R.string.conjunto_actual, it.nombre, it.municipio) }
            ?: getString(R.string.conjunto_elegir)
        boton.contentDescription = c?.let { getString(R.string.conjunto_actual_descripcion, it.nombre) }
    }
    pintar()
    boton.setOnClickListener {
        elegirConjunto(null) { nuevo ->
            cambiarConjunto(contexto, nuevo)
            pintar()
        }
    }
    // Quien ya entraba con huella antes de P7 sigue en el conjunto 1 sin que se le pregunte: el
    // diálogo taparía la huella. Puede cambiarlo con el botón
    if (ConjuntoElegido.actual(contexto) != null || AccesoBiometrico.cuentaGuardada(contexto) != null) return
    viewLifecycleOwner.lifecycleScope.launch {
        // Sin conexión no se insiste: el botón queda para intentarlo a mano
        val respuesta = runCatching { RetrofitClient.comun.ubicacionesConjuntos() }.getOrNull() ?: return@launch
        if (view == null) return@launch
        val lista = respuesta.body()
        when {
            respuesta.code() == 404 -> boton.visibility = View.GONE
            respuesta.isSuccessful && !lista.isNullOrEmpty() -> elegirConjunto(lista, soloConBoton = true) { nuevo ->
                cambiarConjunto(contexto, nuevo)
                pintar()
            }
        }
    }
}

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
 * Diálogo con tres menús en cascada: departamento, municipio y conjunto. Lo ya elegido sale
 * marcado, y si en un menú hay una sola opción se marca sola. [ubicaciones] evita pedirlas otra
 * vez; [soloConBoton]: al abrirse solo, tocar afuera no lo cierra (sí "Ahora no").
 */
fun Fragment.elegirConjunto(
    ubicaciones: List<UbicacionConjuntos>?,
    soloConBoton: Boolean = false,
    alElegir: (ConjuntoDTO) -> Unit
) {
    val contexto = requireContext()
    val vista = DialogElegirConjuntoBinding.inflate(layoutInflater)
    val actual = ConjuntoElegido.actual(contexto)
    var elegido: ConjuntoDTO? = null
    // Si se cambia de municipio antes de que llegue la lista anterior, esa se descarta
    var peticion = 0
    val dialogo = MaterialAlertDialogBuilder(contexto)
        .setTitle(R.string.conjunto_titulo)
        .setMessage(R.string.conjunto_mensaje)
        .setView(vista.root)
        .setCancelable(!soloConBoton)
        .setPositiveButton(R.string.conjunto_continuar) { _, _ -> elegido?.let(alElegir) }
        .setNegativeButton(if (soloConBoton) R.string.ahora_no else R.string.btn_cancelar, null)
        .show()
    val continuar = dialogo.getButton(DialogInterface.BUTTON_POSITIVE)
    continuar.isEnabled = false

    fun error(mensaje: String?) {
        vista.tvError.text = mensaje
        vista.tvError.isVisible = mensaje != null
    }
    fun opciones(campo: AutoCompleteTextView, items: List<String>) =
        campo.setAdapter(ArrayAdapter(contexto, android.R.layout.simple_list_item_1, items))
    fun marcar(c: ConjuntoDTO?) {
        elegido = c
        continuar.isEnabled = c != null
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
                if (!dialogo.isShowing || esta != peticion) return@launch
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
                if (dialogo.isShowing && esta == peticion) error(RetrofitClient.mensajeDeExcepcion(e))
            }
            if (dialogo.isShowing && esta == peticion) vista.progreso.isVisible = false
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
            if (!dialogo.isShowing) return@launch
            vista.progreso.isVisible = false
            when {
                respuesta.code() == 404 -> error(getString(R.string.conjunto_no_disponible))
                respuesta.isSuccessful && lista != null -> mostrar(lista)
                else -> error(RetrofitClient.mensajeDeError(respuesta, getString(R.string.conjunto_error)))
            }
        } catch (e: Exception) {
            if (dialogo.isShowing) {
                vista.progreso.isVisible = false
                error(RetrofitClient.mensajeDeExcepcion(e))
            }
        }
    }
}
