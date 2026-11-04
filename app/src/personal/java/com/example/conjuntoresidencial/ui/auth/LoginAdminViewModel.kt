package com.example.conjuntoresidencial.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conjuntoresidencial.api.RetrofitClient

import com.example.conjuntoresidencial.model.LoginAdminRequest
import com.example.conjuntoresidencial.model.LoginResponse
import com.example.conjuntoresidencial.util.Resource
import kotlinx.coroutines.launch

class LoginAdminViewModel : ViewModel() {

    private val _loginState = MutableLiveData<Resource<LoginResponse>>()
    val loginState: LiveData<Resource<LoginResponse>> get() = _loginState

    fun loginAdmin(usuario: String, contrasena: String) {
        if (usuario.isBlank() || contrasena.isBlank()) {
            _loginState.value = Resource.Error("Por favor, rellene todos los campos")
            return
        }
        // Un solo intento a la vez: "Listo" del teclado y el botón juntos no deben pedir dos códigos (P7 bloque 5)
        if (_loginState.value is Resource.Loading) return

        _loginState.value = Resource.Loading
        viewModelScope.launch {
            try {
                val response = RetrofitClient.comun.loginAdmin(LoginAdminRequest(usuario, contrasena))
                if (response.isSuccessful && response.body() != null) {
                    _loginState.value = Resource.Success(response.body())
                } else {
                    // 401 credenciales o 429 demasiados intentos: el backend explica cuál
                    _loginState.value = Resource.Error(
                        RetrofitClient.mensajeDeError(response, "Credenciales de Administrador inválidas")
                    )
                }
            } catch (e: Exception) {
                _loginState.value = Resource.Error(RetrofitClient.mensajeDeExcepcion(e))
            }
        }
    }
}
