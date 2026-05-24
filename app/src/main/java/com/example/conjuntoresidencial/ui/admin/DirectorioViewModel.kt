package com.example.conjuntoresidencial.ui.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.model.Apartamento
import com.example.conjuntoresidencial.util.Resource
import kotlinx.coroutines.launch

class DirectorioViewModel : ViewModel() {

    private val _aptos = MutableLiveData<Resource<List<Apartamento>>>()
    val aptos: LiveData<Resource<List<Apartamento>>> get() = _aptos

    fun fetchApartamentos() {
        _aptos.value = Resource.Loading
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getApartamentos()
                if (response.isSuccessful && response.body() != null) {
                    _aptos.value = Resource.Success(response.body()!!)
                } else {
                    _aptos.value = Resource.Error("Error al cargar apartamentos")
                }
            } catch (e: Exception) {
                _aptos.value = Resource.Error("Error de red: ${e.localizedMessage}")
            }
        }
    }
}