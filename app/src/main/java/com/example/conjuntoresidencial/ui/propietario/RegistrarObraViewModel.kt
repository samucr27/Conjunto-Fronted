package com.example.conjuntoresidencial.ui.propietario

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.model.ObraDTO
import com.example.conjuntoresidencial.model.PersonalObraDTO
import com.example.conjuntoresidencial.ui.obras.CertificadoArl
import com.example.conjuntoresidencial.util.Resource
import kotlinx.coroutines.launch

/** Trabajador agregado al registrar la obra, con su certificado ARL si lo adjuntaron. */
data class TrabajadorNuevo(val dto: PersonalObraDTO, val certificado: CertificadoArl?)

class RegistrarObraViewModel : ViewModel() {

    // Lo que el residente lleva escrito fuera de los campos de texto; sobrevive a girar la pantalla
    var fechaInicio = ""   // "yyyy-MM-dd"
    var fechaFin = ""
    var estado = "EN_PROCESO"
    val personal = mutableListOf<TrabajadorNuevo>()
    // Certificado elegido para el trabajador que se está escribiendo
    var certificadoPendiente: CertificadoArl? = null
    // Al editar (P5): la obra como venía del backend; null = obra nueva
    var original: ObraDTO? = null

    private val _obraState = MutableLiveData<Resource<ObraDTO>>()
    val obraState: LiveData<Resource<ObraDTO>> get() = _obraState

    /** Nueva (aptoId null = obra del conjunto) o, si hay [original], los cambios de esa obra. */
    fun registrarObra(descripcion: String, aptoId: Long?, errorPorDefecto: String) {
        _obraState.value = Resource.Loading
        viewModelScope.launch {
            try {
                val editada = original
                val response = if (editada?.id != null) {
                    RetrofitClient.comun.updateObra(editada.id, editada.copy(descripcionAdecuacion = descripcion,
                        fechaInicio = fechaInicio, fechaFinEstimada = fechaFin, estadoObra = estado))
                } else {
                    RetrofitClient.comun.createObra(ObraDTO(null, descripcion, fechaInicio, fechaFin, estado, aptoId))
                }
                if (response.isSuccessful && response.body() != null) {
                    _obraState.value = Resource.Success(response.body())
                } else {
                    _obraState.value = Resource.Error(RetrofitClient.mensajeDeError(response, errorPorDefecto))
                }
            } catch (e: Exception) {
                _obraState.value = Resource.Error(RetrofitClient.mensajeDeExcepcion(e))
            }
        }
    }
}
