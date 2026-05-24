package com.example.conjuntoresidencial.ui.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.model.ObraDTO
import com.example.conjuntoresidencial.util.Resource
import kotlinx.coroutines.launch

class ControlObrasViewModel : ViewModel() {
    private val _obras = MutableLiveData<Resource<List<ObraDTO>>>()
    val obras: LiveData<Resource<List<ObraDTO>>> get() = _obras

    fun fetchObras() {
        _obras.value = Resource.Loading
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getObrasEnProceso()
                if (response.isSuccessful && response.body() != null)
                    _obras.value = Resource.Success(response.body()!!)
                else _obras.value = Resource.Error("Error al cargar obras")
            } catch (e: Exception) {
                _obras.value = Resource.Error("Error de red: ${e.localizedMessage}")
            }
        }
    }
}