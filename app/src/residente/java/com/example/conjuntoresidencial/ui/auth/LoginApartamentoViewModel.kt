package com.example.conjuntoresidencial.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.api.instance
import com.example.conjuntoresidencial.model.LoginApartamentoRequest
import com.example.conjuntoresidencial.model.LoginResidenteRequest
import com.example.conjuntoresidencial.model.LoginResponse
import com.example.conjuntoresidencial.util.Resource
import kotlinx.coroutines.launch

class LoginApartamentoViewModel : ViewModel() {

    private val _loginState = MutableLiveData<Resource<LoginResponse>>()
    val loginState: LiveData<Resource<LoginResponse>> get() = _loginState

    fun loginApartamento(torre: String, apto: String, contrasena: String) {
        if (torre.isBlank() || apto.isBlank() || contrasena.isBlank()) {
            _loginState.value = Resource.Error("Llene todos los datos del apartamento")
            return
        }
        entrar { RetrofitClient.instance.loginApartamento(LoginApartamentoRequest(torre, apto, contrasena)) }
    }

    /** Con su documento y su contraseña personal (P4). */
    /** El titular (P5): torre, apto, PROPIETARIO o ARRENDATARIO y su contraseña. */
    fun loginTitular(torre: String, apto: String, rol: String, contrasena: String) {
        entrar { RetrofitClient.instance.loginResidente(LoginResidenteRequest(contrasena = contrasena, torre = torre, apto = apto, rol = rol)) }
    }

    private fun entrar(peticion: suspend () -> retrofit2.Response<LoginResponse>) {
        _loginState.value = Resource.Loading
        viewModelScope.launch {
            try {
                val response = peticion()
                if (response.isSuccessful && response.body() != null) {
                    _loginState.value = Resource.Success(response.body())
                } else {
                    // 401 datos incorrectos o 429 demasiados intentos: el backend explica cuál
                    _loginState.value = Resource.Error(
                        RetrofitClient.mensajeDeError(response, "Datos de Apartamento inválidos")
                    )
                }
            } catch (e: Exception) {
                _loginState.value = Resource.Error(RetrofitClient.mensajeDeExcepcion(e))
            }
        }
    }
}
