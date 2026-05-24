package com.example.conjuntoresidencial.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.model.Apartamento
import com.example.conjuntoresidencial.model.LoginApartamentoRequest
import com.example.conjuntoresidencial.util.Resource
import kotlinx.coroutines.launch

class LoginApartamentoViewModel : ViewModel() {

    private val _loginState = MutableLiveData<Resource<Apartamento>>()
    val loginState: LiveData<Resource<Apartamento>> get() = _loginState

    fun loginApartamento(torre: String, apto: String, contrasena: String) {
        if (torre.isBlank() || apto.isBlank() || contrasena.isBlank()) {
            _loginState.value = Resource.Error("Llene todos los datos del apartamento")
            return
        }

        _loginState.value = Resource.Loading
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.loginApartamento(LoginApartamentoRequest(torre, apto, contrasena))
                if (response.isSuccessful && response.body() != null) {
                    _loginState.value = Resource.Success(response.body())
                } else {
                    _loginState.value = Resource.Error("Datos de Apartamento inválidos")
                }
            } catch (e: Exception) {
                _loginState.value = Resource.Error("Error en red: ${e.localizedMessage}")
            }
        }
    }
}