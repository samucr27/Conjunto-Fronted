package com.example.conjuntoresidencial.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.model.PersonaRegistro
import com.example.conjuntoresidencial.model.RegistroRecibido
import com.example.conjuntoresidencial.model.RegistroRequest
import com.example.conjuntoresidencial.model.VerificarRegistroRequest
import com.example.conjuntoresidencial.util.Evento
import kotlinx.coroutines.launch

/**
 * Estado del registro en pasos (sobrevive a girar la pantalla): 1 torre y apto, 2 datos
 * personales, 3 personas del hogar, 4 visitantes frecuentes, 5 listo.
 */
class RegistroViewModel : ViewModel() {
    var paso = 1
    var torre = ""
    var apto = ""
    var rol = ""
    val personas = mutableListOf<PersonaRegistro>()
    val visitantes = mutableListOf<PersonaRegistro>()
    var datos: RegistroRequest? = null
    var recibido: RegistroRecibido? = null

    private val _cargando = MutableLiveData(false)
    val cargando: LiveData<Boolean> get() = _cargando

    // Avanzar al paso indicado (o el mensaje de error del backend)
    private val _resultado = MutableLiveData<Evento<Result<Int>>>()
    val resultado: LiveData<Evento<Result<Int>>> get() = _resultado

    fun verificar(torre: String, apto: String) {
        _cargando.value = true
        viewModelScope.launch {
            val r = runCatching {
                val respuesta = RetrofitClient.instance.verificarRegistro(VerificarRegistroRequest(torre, apto))
                val cuerpo = respuesta.body()
                if (!respuesta.isSuccessful || cuerpo == null) {
                    error(RetrofitClient.mensajeDeError(respuesta, "No se pudo verificar el apartamento"))
                }
                this@RegistroViewModel.torre = torre
                this@RegistroViewModel.apto = apto
                rol = cuerpo.rol
                2
            }
            _cargando.value = false
            _resultado.value = Evento(r.recoverCatching { e -> throw IllegalStateException(mensaje(e)) })
        }
    }

    fun enviar() {
        val base = datos ?: return
        _cargando.value = true
        viewModelScope.launch {
            val r = runCatching {
                val respuesta = RetrofitClient.instance.registrar(base.copy(personas = personas.toList(), visitantes = visitantes.toList()))
                val cuerpo = respuesta.body()
                if (!respuesta.isSuccessful || cuerpo == null) {
                    error(RetrofitClient.mensajeDeError(respuesta, "No se pudo enviar el registro"))
                }
                recibido = cuerpo
                5
            }
            _cargando.value = false
            _resultado.value = Evento(r.recoverCatching { e -> throw IllegalStateException(mensaje(e)) })
        }
    }

    private fun mensaje(e: Throwable): String =
        if (e is IllegalStateException) e.message.orEmpty() else RetrofitClient.mensajeDeExcepcion(e as? Exception ?: Exception(e))
}
