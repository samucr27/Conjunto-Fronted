package com.example.conjuntoresidencial.model

data class LoginAdminRequest(val usuario: String, val contrasena: String)
data class LoginApartamentoRequest(val torre: String, val apto: String, val contrasena: String)
// El backend nunca devuelve la contraseña
data class Apartamento(val id: Long, val torre: String, val apto: String)
// Para crear/editar: la contraseña solo se envía (en edición es opcional)
data class ApartamentoRequest(val torre: String, val apto: String, val contrasena: String? = null)
data class CambioContrasenaRequest(val nuevaContrasena: String)
// El usuario cambia su propia contraseña (Cuenta → Perfil)
data class CambioPropiaContrasenaRequest(val contrasenaActual: String, val contrasenaNueva: String)
// Respuesta de los dos logins: usuario solo viene para el admin; torre y apto solo para el propietario
data class LoginResponse(val token: String, val expiraEn: String?, val rol: String, val id: Long, val usuario: String?, val torre: String?, val apto: String?)
// Cuerpo de los errores del backend: {"mensaje": "..."}
data class MensajeError(val mensaje: String?)
data class ResidenteDTO(val id: Long?, val nombreCompleto: String, val telefono: String, val identificacion: String, val tipoHabitante: String, val apartamentoId: Long)
// marca y color son opcionales; la placa solo la llevan carros y motos. estado (PENDIENTE,
// APROBADO o RECHAZADO), motivoRechazo, torre y apto los pone el backend (P4)
data class VehiculoDTO(
    val id: Long?, val placa: String?, val marca: String?, val color: String?, val tipo: String, val apartamentoId: Long,
    val estado: String? = null, val motivoRechazo: String? = null, val torre: String? = null, val apto: String? = null
)
// Cuántos vehículos de un tipo puede tener cada apartamento
data class CupoVehiculoDTO(val tipo: String, val maximo: Int, val requierePlaca: Boolean? = null) {
    val llevaPlaca: Boolean get() = requierePlaca ?: (tipo in com.example.conjuntoresidencial.util.Opciones.TIPOS_CON_PLACA)
}
// El administrador aprueba o rechaza un vehículo (motivo opcional, lo ve el residente)
data class EstadoVehiculoRequest(val estado: String, val motivo: String? = null)
data class ObraDTO(val id: Long?, val descripcionAdecuacion: String, val fechaInicio: String, val fechaFinEstimada: String, val estadoObra: String, val apartamentoId: Long)
data class PersonalObraDTO(val id: Long?, val nombreTrabajador: String, val cedula: String, val arlEstado: Boolean, val obraId: Long, val torrePiso: String? = null, val aptoNum: String? = null)
// aforo (personas por reserva) y horario para reservar ("08:00"); null = sin límite (P4)
data class ZonaComunDTO(
    val id: Long?, val nombre: String, val estadoRecibido: String, val descripcionEstado: String, val esReservable: Boolean? = null,
    val aforo: Int? = null, val horaApertura: String? = null, val horaCierre: String? = null
)
// hora = inicio; horaFin y asistentes desde P4 (al leer, horaFin siempre viene)
data class ReservaDTO(
    val id: Long?, val fecha: String, val hora: String, val estado: String, val zonaComunId: Long, val apartamentoId: Long,
    val horaFin: String? = null, val asistentes: Int? = null
)
// Lo ocupado de una zona día por día (calendario de reservas); las de otros no dicen de quién son
data class DisponibilidadDTO(
    val zonaId: Long, val aforo: Int?, val horaApertura: String?, val horaCierre: String?, val dias: List<DiaDisponibilidadDTO>
)
data class DiaDisponibilidadDTO(val fecha: String, val ocupadas: List<FranjaOcupadaDTO>)
data class FranjaOcupadaDTO(val inicio: String, val fin: String, val estado: String, val propia: Boolean?)
data class PagoDTO(val id: Long?, val saldoPendiente: Double, val estadoCuenta: String, val observaciones: String, val apartamentoId: Long)
data class EstadoRequest(val estado: String)
// tipo: "RESERVA" o "VEHICULO"; fecha en formato ISO, hora de Colombia ("2026-10-20T20:15:00").
// referenciaId (la reserva o el vehículo) y apartamentoId dicen a qué pantalla lleva; null en las viejas.
data class NotificacionDTO(
    val id: Long, val tipo: String, val mensaje: String, val fecha: String, val leida: Boolean,
    val referenciaId: Long? = null, val apartamentoId: Long? = null
)
// Token de Firebase del celular (avisos)
data class DispositivoRequest(val token: String)
// {"total": n}
data class ConteoDTO(val total: Long)