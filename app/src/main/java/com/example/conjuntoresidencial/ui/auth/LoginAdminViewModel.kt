package com.example.conjuntoresidencial.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.model.LoginAdminRequest
import com.example.conjuntoresidencial.util.Resource
import kotlinx.coroutines.launch

class LoginAdminViewModel : ViewModel() {

    private val _loginState = MutableLiveData<Resource<Unit>>()
    val loginState: LiveData<Resource<Unit>> get() = _loginState

    fun loginAdmin(usuario: String, contrasena: String) {
        if (usuario.isBlank() || contrasena.isBlank()) {
            _loginState.value = Resource.Error("Por favor, rellene todos los campos")
            return
        }

        _loginState.value = Resource.Loading
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.loginAdmin(LoginAdminRequest(usuario, contrasena))
                if (response.isSuccessful) {
                    _loginState.value = Resource.Success(Unit)
                } else {
                    _loginState.value = Resource.Error("Credenciales de Administrador inválidas")
                }
            } catch (e: Exception) {
                _loginState.value = Resource.Error("Error en conexión: ${e.localizedMessage}")
            }
        }
    }
}