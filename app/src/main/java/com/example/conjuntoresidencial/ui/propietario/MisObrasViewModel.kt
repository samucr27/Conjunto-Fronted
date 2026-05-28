package com.example.conjuntoresidencial.ui.propietario

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.model.ObraDTO
import com.example.conjuntoresidencial.util.Resource
import kotlinx.coroutines.launch

class MisObrasViewModel : ViewModel() {
    private val _obras = MutableLiveData<Resource<List<ObraDTO>>>()
    val obras: LiveData<Resource<List<ObraDTO>>> get() = _obras

    private var aptoIdActual: Long = -1L

    fun fetchObras(aptoId: Long) {
        aptoIdActual = aptoId
        _obras.value = Resource.Loading
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getObrasPorApartamento(aptoId)
                if (response.isSuccessful && response.body() != null)
                    _obras.value = Resource.Success(response.body()!!)
                else _obras.value = Resource.Error("Error al cargar obras")
            } catch (e: Exception) {
                _obras.value = Resource.Error("Error de red: ${e.localizedMessage}")
            }
        }
    }

    fun finalizarObra(obraId: Long, obra: ObraDTO) {
        viewModelScope.launch {
            try {
                val obraActualizada = obra.copy(estadoObra = "FINALIZADA")
                val response = RetrofitClient.instance.updateObra(obraId, obraActualizada)
                if (response.isSuccessful) {
                    // Actualizar solo ese item en la lista sin recargar todo
                    val listaActual = (_obras.value as? Resource.Success)?.data?.toMutableList() ?: return@launch
                    val idx = listaActual.indexOfFirst { it.id == obraId }
                    if (idx != -1) {
                        listaActual[idx] = obraActualizada
                        _obras.value = Resource.Success(listaActual)
                    }
                }
            } catch (_: Exception) {}
        }
    }
}