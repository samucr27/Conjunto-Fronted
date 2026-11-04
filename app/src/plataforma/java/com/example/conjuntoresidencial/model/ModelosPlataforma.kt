package com.example.conjuntoresidencial.model

// P7 bloque 3: lo que maneja la app de plataforma (PlataformaDTO del backend)

data class LoginPlataforma(val usuario: String, val contrasena: String)

data class ConjuntoPlataforma(
    val id: Long, val nombre: String, val departamento: String, val municipio: String, val direccion: String?,
    val activo: Boolean, val creadoEn: String?, val apartamentos: Long, val cuentasAdministracion: Long,
    // Usuario del administrador principal; null si el conjunto no tiene
    val principal: String?,
    // P7 bloque 5: celulares con sesión a la vez por cuenta (null: servidor anterior)
    val maxDispositivos: Int? = null
)

data class DatosConjunto(
    val nombre: String, val departamento: String, val municipio: String, val direccion: String?, val maxDispositivos: Int?
)

data class NuevoConjunto(
    val nombre: String, val departamento: String, val municipio: String, val direccion: String?,
    val usuarioPrincipal: String, val nombrePrincipal: String, val correoPrincipal: String?, val maxDispositivos: Int?
)

data class CambioEstadoConjunto(val activo: Boolean, val motivo: String)

data class CuentaConjunto(
    val id: Long, val usuario: String, val nombreCompleto: String?, val rol: String, val principal: Boolean, val activo: Boolean
)

/** La contraseña temporal solo llega esta vez. */
data class AccesoPrincipal(val conjuntoId: Long, val usuario: String, val contrasenaTemporal: String)

data class EventoPlataforma(
    val id: Long, val operador: String, val accion: String, val conjuntoId: Long?, val detalle: String?, val fecha: String
)
