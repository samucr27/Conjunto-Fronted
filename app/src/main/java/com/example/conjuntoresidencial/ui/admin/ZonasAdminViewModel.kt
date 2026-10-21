package com.example.conjuntoresidencial.ui.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.model.ZonaComunDTO
import com.example.conjuntoresidencial.util.Evento
import com.example.conjuntoresidencial.util.Resource
import kotlinx.coroutines.launch

class ZonasAdminViewModel : ViewModel() {
    private val _zonas = MutableLiveData<Resource<List<ZonaComunDTO>>>()
    val zonas: LiveData<Resource<List<ZonaComunDTO>>> get() = _zonas

    /** Resultado de registrar una zona: null = éxito, texto = mensaje de error. */
    private val _registro = MutableLiveData<Evento<String?>>()
    val registro: LiveData<Evento<String?>> get() = _registro

    fun fetchZonas() {
        _zonas.value = Resource.Loading
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getZonasComunes()
                if (response.isSuccessful && response.body() != null)
                    _zonas.value = Resource.Success(response.body()!!)
                else _zonas.value = Resource.Error(RetrofitClient.mensajeDeError(response, "Error al cargar zonas"))
            } catch (e: Exception) {
                _zonas.value = Resource.Error(RetrofitClient.mensajeDeExcepcion(e))
            }
        }
    }

    fun registrarZona(nombre: String, estado: String, descripcion: String, esReservable: Boolean, errorPorDefecto: String) {
        viewModelScope.launch {
            try {
                val zona = ZonaComunDTO(null, nombre, estado, descripcion, esReservable)
                val response = RetrofitClient.instance.createZonaComun(zona)
                if (response.isSuccessful) {
                    _registro.value = Evento(null)
                    fetchZonas()
                } else {
                    // Antes solo se escribía en el log: el admin no se enteraba del error
                    _registro.value = Evento(RetrofitClient.mensajeDeError(response, errorPorDefecto))
                }
            } catch (e: Exception) {
                _registro.value = Evento(RetrofitClient.mensajeDeExcepcion(e))
            }
        }
    }
}
