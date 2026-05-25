package com.example.conjuntoresidencial.ui.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.model.PersonalObraDTO
import com.example.conjuntoresidencial.util.Resource
import kotlinx.coroutines.launch

class PersonalObraViewModel : ViewModel() {
    private val _personal = MutableLiveData<Resource<List<PersonalObraDTO>>>()
    val personal: LiveData<Resource<List<PersonalObraDTO>>> get() = _personal

    fun fetchPersonal() {
        _personal.value = Resource.Loading
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getTodoElPersonal()
                if (response.isSuccessful && response.body() != null)
                    _personal.value = Resource.Success(response.body()!!)
                else _personal.value = Resource.Error("Error al cargar personal")
            } catch (e: Exception) {
                _personal.value = Resource.Error("Error de red: ${e.localizedMessage}")
            }
        }
    }
}