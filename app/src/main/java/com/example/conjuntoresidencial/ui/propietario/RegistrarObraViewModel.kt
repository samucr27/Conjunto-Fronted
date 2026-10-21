package com.example.conjuntoresidencial.ui.propietario

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.model.ObraDTO
import com.example.conjuntoresidencial.model.PersonalObraDTO
import com.example.conjuntoresidencial.util.Resource
import kotlinx.coroutines.launch

class RegistrarObraViewModel : ViewModel() {

    // Lo que el residente lleva escrito fuera de los campos de texto; sobrevive a girar la pantalla
    var fechaInicio = ""   // "yyyy-MM-dd"
    var fechaFin = ""
    var estado = "EN_PROCESO"
    val personal = mutableListOf<PersonalObraDTO>()

    private val _obraState = MutableLiveData<Resource<ObraDTO>>()
    val obraState: LiveData<Resource<ObraDTO>> get() = _obraState

    fun registrarObra(descripcion: String, aptoId: Long, errorPorDefecto: String) {
        _obraState.value = Resource.Loading
        viewModelScope.launch {
            try {
                val obra = ObraDTO(null, descripcion, fechaInicio, fechaFin, estado, aptoId)
                val response = RetrofitClient.instance.createObra(obra)
                if (response.isSuccessful && response.body() != null) {
                    _obraState.value = Resource.Success(response.body())
                } else {
                    _obraState.value = Resource.Error(RetrofitClient.mensajeDeError(response, errorPorDefecto))
                }
            } catch (e: Exception) {
                _obraState.value = Resource.Error("Error de red: ${e.localizedMessage}")
            }
        }
    }
}
