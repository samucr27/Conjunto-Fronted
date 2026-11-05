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
    val id: Long, val operador: String, val accion: String, val conjuntoId: Long?, val detalle: String?, val fecha: String,
    // P8 bloque 6: el nombre del conjunto (solo en /auditoria/buscar, /panel y /detalle)
    val conjunto: String? = null
)

// P8 bloque 6: panel, detalle del conjunto y auditoría con filtros (PlataformaPanelService del backend)

data class PorDepartamento(val departamento: String, val conjuntos: Long, val activos: Long)

data class PendientePlataforma(val conjuntoId: Long, val nombre: String, val municipio: String, val motivo: String)

data class PanelPlataforma(
    val conjuntosActivos: Long, val conjuntosSuspendidos: Long, val conjuntosNuevosMes: Long, val apartamentos: Long,
    val residentes: Long, val cuentasAdministracion: Long, val celularesConSesion: Long, val visitasSemana: Long,
    val porDepartamento: List<PorDepartamento>, val pendientes: List<PendientePlataforma>,
    val ultimosEventos: List<EventoPlataforma>
)

data class CifrasConjunto(
    val apartamentos: Long, val propietarios: Long, val arrendatarios: Long, val residentesPendientes: Long,
    val vehiculos: Long, val guardias: Long, val celularesConSesion: Long, val visitasMes: Long, val parqueaderos: Long
)

data class DetalleConjunto(
    val conjunto: ConjuntoPlataforma, val cifras: CifrasConjunto, val cuentas: List<CuentaConjunto>,
    val ultimaSesion: String?, val eventos: List<EventoPlataforma>,
    // P9 bloque 2: el código de hoy (null: servidor anterior)
    val codigoDelDia: String? = null, val codigoCambiaEn: String? = null
)

data class PaginaEventos(val eventos: List<EventoPlataforma>, val hayMas: Boolean)
