package com.example.conjuntoresidencial.ui.propietario

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.api.instance
import com.example.conjuntoresidencial.model.ZonaComunDTO
import com.example.conjuntoresidencial.util.Resource
import kotlinx.coroutines.launch

class ZonasPropietarioViewModel : ViewModel() {

    private val _zonas = MutableLiveData<Resource<List<ZonaComunDTO>>>()
    val zonas: LiveData<Resource<List<ZonaComunDTO>>> get() = _zonas

    fun fetchZonas() {
        _zonas.value = Resource.Loading
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getZonasComunes()
                if (response.isSuccessful && response.body() != null) {
                    _zonas.value = Resource.Success(response.body()!!)
                } else _zonas.value = Resource.Error("Error al cargar zonas")
            } catch (e: Exception) {
                _zonas.value = Resource.Error(RetrofitClient.mensajeDeExcepcion(e))
            }
        }
    }
}
