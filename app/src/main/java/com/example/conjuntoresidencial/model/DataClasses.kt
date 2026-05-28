package com.example.conjuntoresidencial.model

data class LoginAdminRequest(val usuario: String, val contrasena: String)
data class LoginApartamentoRequest(val torre: String, val apto: String, val contrasena: String)
data class Apartamento(val id: Long, val torre: String, val apto: String, val contrasena: String)
data class ResidenteDTO(val id: Long?, val nombreCompleto: String, val telefono: String, val identificacion: String, val tipoHabitante: String, val apartamentoId: Long)
data class VehiculoDTO(val id: Long?, val placa: String, val marca: String, val color: String, val tipo: String, val apartamentoId: Long)
data class ObraDTO(val id: Long?, val descripcionAdecuacion: String, val fechaInicio: String, val fechaFinEstimada: String, val estadoObra: String, val apartamentoId: Long)
data class PersonalObraDTO(val id: Long?, val nombreTrabajador: String, val cedula: String, val arlEstado: Boolean, val obraId: Long, val torrePiso: String? = null, val aptoNum: String? = null)
data class ZonaComunDTO(val id: Long?, val nombre: String, val estadoRecibido: String, val descripcionEstado: String, val esReservable: Boolean? = null)
data class ReservaDTO(val id: Long?, val fecha: String, val hora: String, val estado: String, val zonaComunId: Long, val apartamentoId: Long)
data class PagoDTO(val id: Long?, val saldoPendiente: Double, val estadoCuenta: String, val observaciones: String, val apartamentoId: Long)
data class EstadoRequest(val estado: String)