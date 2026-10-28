package com.example.conjuntoresidencial.model

data class LoginAdminRequest(val usuario: String, val contrasena: String)
data class LoginApartamentoRequest(val torre: String, val apto: String, val contrasena: String)
// Cada residente con su documento y su contraseña personal (P4)
// P5: el titular entra con torre, apto y rol; el documento (P4) sigue funcionando
data class LoginResidenteRequest(
    val documento: String? = null, val contrasena: String,
    val torre: String? = null, val apto: String? = null, val rol: String? = null
)
// Registro desde la app (P5): paso 1 dice si queda como PROPIETARIO o ARRENDATARIO
data class VerificarRegistroRequest(val torre: String, val apto: String)
data class RegistroVerificado(val rol: String)
data class PersonaRegistro(val nombreCompleto: String, val identificacion: String?, val telefono: String?)
data class RegistroRequest(
    val torre: String, val apto: String, val nombres: String, val apellidos: String, val cedula: String,
    val telefono: String, val correo: String, val contrasena: String, val aceptaDatos: Boolean, val aceptaTerminos: Boolean,
    val personas: List<PersonaRegistro> = emptyList(), val visitantes: List<PersonaRegistro> = emptyList()
)
data class RegistroRecibido(val rol: String, val estado: String, val requierePropietario: Boolean)
// El backend nunca devuelve la contraseña
// citófonos y arrendado (arrendatario activo) los pone el backend (P5)
data class Apartamento(
    val id: Long, val torre: String, val apto: String,
    val citofono1: String? = null, val citofono2: String? = null, val arrendado: Boolean? = null,
    // P6: cómo está ocupado y qué le deja ver la administración al arrendatario
    val tipoOcupacion: String? = null, val arrendatarioVeCartera: Boolean? = null,
    val arrendatarioPuedePagar: Boolean? = null, val arrendatarioVeObras: Boolean? = null
)
// PATCH /api/apartamentos/{id}/ocupacion-y-banderas: lo que no se envía no cambia
data class OcupacionYBanderas(
    val tipoOcupacion: String? = null, val arrendatarioVeCartera: Boolean? = null,
    val arrendatarioPuedePagar: Boolean? = null, val arrendatarioVeObras: Boolean? = null
)
data class CitofonosRequest(val citofono1: String?, val citofono2: String?)
// Feed de publicaciones (P5): cada url es un enlace firmado que vence en una hora
data class AdjuntoPublicacion(val id: Long, val tipo: String, val nombre: String, val mime: String, val tamano: Long, val url: String?)
data class PublicacionDTO(
    val id: Long, val texto: String, val autor: String, val creadaEn: String, val editadaEn: String?,
    val adjuntos: List<AdjuntoPublicacion>,
    // P6: título opcional, categoría, a quién va dirigida, torre (null = todas), fijada arriba y si ya la abrí
    val titulo: String? = null, val categoria: String = "GENERAL", val audiencia: String = "TODOS",
    val torre: String? = null, val fijado: Boolean = false, val leida: Boolean = true
)
data class PaginaPublicaciones(val publicaciones: List<PublicacionDTO>, val hayMas: Boolean)
// Lo que va null no se cambia
data class EdicionPublicacion(
    val texto: String? = null, val titulo: String? = null, val categoria: String? = null,
    val audiencia: String? = null, val torre: String? = null, val fijado: Boolean? = null
)
data class FijarPublicacion(val fijado: Boolean)
// Visitante frecuente del apartamento (P5)
data class VisitanteDTO(val id: Long?, val nombreCompleto: String, val identificacion: String?, val telefono: String?, val apartamentoId: Long)
// El propietario acepta o rechaza a su arrendatario
data class AprobacionPropietario(val aprobado: Boolean, val motivo: String? = null)
// Para crear/editar: la contraseña solo se envía (en edición es opcional)
data class ApartamentoRequest(val torre: String, val apto: String, val contrasena: String? = null)
data class CambioContrasenaRequest(val nuevaContrasena: String)
// El usuario cambia su propia contraseña (Cuenta → Perfil)
data class CambioPropiaContrasenaRequest(val contrasenaActual: String, val contrasenaNueva: String)
// Respuesta de los dos logins: usuario solo viene para el admin; torre y apto solo para el propietario
// rol: ADMIN, PROPIETARIO o ARRENDATARIO. residenteId y nombre solo si entró con su documento
data class LoginResponse(
    val token: String, val expiraEn: String?, val rol: String, val id: Long, val usuario: String?, val torre: String?, val apto: String?,
    val residenteId: Long? = null, val nombre: String? = null,
    // P5: solo administradores; el principal es el único que maneja el equipo
    val principal: Boolean? = null,
    // P6: solo residentes. Ocupación del apartamento y lo que la administración le deja ver al arrendatario
    val tipoOcupacion: String? = null,
    val arrendatarioVeCartera: Boolean? = null,
    val arrendatarioPuedePagar: Boolean? = null,
    val arrendatarioVeObras: Boolean? = null,
    // Si solo la administración registra vehículos
    val vehiculosAltaSoloAdmin: Boolean? = null,
    // Hay una versión de los términos o la política por aceptar
    val consentimientosPendientes: Boolean? = null
)
// Cuerpo de los errores del backend: {"mensaje": "..."}
data class MensajeError(val mensaje: String?)
// estado (PENDIENTE, APROBADO o RECHAZADO), tieneAcceso, torre y apto los pone el backend;
// contrasena solo se envía al crearlo (con ella entra con su documento)
data class ResidenteDTO(
    val id: Long?, val nombreCompleto: String, val telefono: String?, val identificacion: String, val tipoHabitante: String, val apartamentoId: Long,
    val estado: String? = null, val tieneAcceso: Boolean? = null, val torre: String? = null, val apto: String? = null,
    val contrasena: String? = null,
    val correo: String? = null, val titular: Boolean? = null, val aprobadoPropietario: Boolean? = null
)
data class RevisionResidente(val estado: String, val motivo: String? = null)
// Cuota de administración de un mes (P4 bloque 7): periodo "2026-11"; estado PENDIENTE,
// EN_REVISION (subió el comprobante), PAGADA o RECHAZADA
data class CuotaDTO(
    val id: Long, val apartamentoId: Long, val torre: String?, val apto: String?, val periodo: String,
    val valor: Double, val fechaLimite: String, val estado: String, val vencida: Boolean? = null,
    val tieneComprobante: Boolean? = null, val comprobanteTipo: String? = null, val comprobanteSubidoEn: String? = null,
    val motivoRechazo: String? = null, val pagadaEn: String? = null,
    // P5: el contador registró el pago sin comprobante y espera al administrador
    val pagoPorAprobar: Boolean? = null
)
data class GenerarCuotasRequest(val periodo: String, val valor: Double, val fechaLimite: String?)
// enAprobacion (P5): la mandó el contador y espera al administrador (creadas = 0)
data class CuotasGeneradas(val periodo: String, val creadas: Int, val existentes: Int, val enAprobacion: Boolean? = null)
// P5: lo que manda el contador a aprobar: FACTURACION (periodo, valor, fecha límite) o PAGO_MANUAL (de una cuota)
data class SolicitudContable(
    val id: Long, val tipo: String, val estado: String, val periodo: String?, val valor: Double?, val fechaLimite: String?,
    val cuotaId: Long?, val torre: String?, val apto: String?, val solicitadaPor: String, val creadaEn: String,
    val revisadaEn: String?, val motivoRechazo: String?
)
data class RevisionSolicitud(val aprobada: Boolean, val motivo: String? = null)
// P5: resumen de la cartera de un mes
data class EstadisticasCartera(
    val periodo: String, val cuotas: Int, val pagadas: Int, val enRevision: Int, val pendientes: Int, val vencidas: Int,
    val rechazadas: Int, val facturado: Double, val recaudado: Double, val porCobrar: Double, val porcentajeRecaudo: Int,
    val apartamentos: Int, val apartamentosAlDia: Int, val apartamentosEnMora: Int, val carteraTotal: Double,
    val solicitudesPendientes: Long
)
data class RevisionCuota(val estado: String, val motivo: String? = null)
// Corregir una cuota sin pagar: los dos opcionales
data class CambioCuota(val valor: Double? = null, val fechaLimite: String? = null)
// Enlace firmado que vence a los 10 minutos
data class EnlaceComprobante(val url: String, val tipo: String?, val expiraEn: String?)
// Cuenta de la administración: rol ADMIN o CONTADOR; contrasena solo al crearla
// P5: datos personales (obligatorios al crearla) y principal (el que maneja el equipo, lo pone el backend)
data class CuentaAdministracion(
    val id: Long?, val usuario: String, val rol: String, val contrasena: String? = null,
    val nombreCompleto: String? = null, val documento: String? = null, val telefono: String? = null, val correo: String? = null,
    val principal: Boolean? = null
)
// Cuántas cuentas de un rol puede crear el principal; maximo null = sin límite
data class CupoEquipo(val rol: String, val maximo: Int?, val usados: Long? = null)
// Contraseña vacía = quitarle el acceso con documento
data class AccesoResidente(val contrasena: String)
// marca y color son opcionales; la placa solo la llevan carros y motos. estado (PENDIENTE,
// APROBADO o RECHAZADO), motivoRechazo, torre y apto los pone el backend (P4)
data class VehiculoDTO(
    val id: Long?, val placa: String?, val marca: String?, val color: String?, val tipo: String, val apartamentoId: Long,
    val estado: String? = null, val motivoRechazo: String? = null, val torre: String? = null, val apto: String? = null,
    // P5: el parqueadero asignado (null = sin parqueadero)
    val parqueaderoId: Long? = null, val parqueadero: String? = null
)
// Cuántos vehículos de un tipo puede tener cada apartamento
// personalizado (P5): el cupo propio del apartamento y no el general
data class CupoVehiculoDTO(val tipo: String, val maximo: Int, val requierePlaca: Boolean? = null, val personalizado: Boolean? = null) {
    val llevaPlaca: Boolean get() = requierePlaca ?: (tipo in com.example.conjuntoresidencial.util.Opciones.TIPOS_CON_PLACA)
}
// El administrador aprueba o rechaza un vehículo (motivo opcional, lo ve el residente)
data class EstadoVehiculoRequest(val estado: String, val motivo: String? = null, val parqueaderoId: Long? = null)
// Parqueaderos (P5): estado LIBRE o ASIGNADO; tipo de vehículo o "Invitados"
data class ParqueaderoDTO(
    val id: Long, val numero: String, val tipo: String, val estado: String, val vehiculoId: Long?, val placa: String?,
    val vehiculo: String?, val apartamentoId: Long?, val torre: String?, val apto: String?
)
data class ResumenParqueaderos(val total: Int, val asignados: Int, val libres: Int, val invitados: Int)
data class GenerarParqueaderos(val tipo: String, val cantidad: Int, val prefijo: String?)
data class AsignacionParqueadero(val vehiculoId: Long?)
// Cupo propio de un apartamento; maximo null = vuelve al cupo general
data class CupoApartamentoRequest(val tipo: String, val maximo: Int?)
// P5: apartamentoId null = obra del conjunto. aprobacion (PENDIENTE, APROBADA o RECHAZADA), motivoRechazo,
// torre, apto y personalPendiente (trabajadores por aprobar) los pone el backend
data class ObraDTO(
    val id: Long?, val descripcionAdecuacion: String, val fechaInicio: String, val fechaFinEstimada: String, val estadoObra: String,
    val apartamentoId: Long?, val aprobacion: String? = null, val motivoRechazo: String? = null,
    val torre: String? = null, val apto: String? = null, val personalPendiente: Int? = null
)
// P5: aprobacion (PENDIENTE, APROBADO o RECHAZADO) y si tiene certificado ARL adjunto
data class PersonalObraDTO(
    val id: Long?, val nombreTrabajador: String, val cedula: String, val arlEstado: Boolean, val obraId: Long,
    val torrePiso: String? = null, val aptoNum: String? = null, val aprobacion: String? = null, val motivoRechazo: String? = null,
    val tieneCertificado: Boolean? = null, val certificadoSubidoEn: String? = null
)
// P5 bloque 8: chat administración ⇄ residente. participante: PROPIETARIO o ARRENDATARIO;
// noLeidos: los del otro lado que no he visto; propio: lo escribí yo (burbuja a la derecha)
data class ConversacionDTO(
    val id: Long, val apartamentoId: Long, val torre: String, val apto: String, val participante: String,
    val ultimoMensaje: String?, val ultimoMensajeEn: String?, val noLeidos: Int
)
data class MensajeChatDTO(
    val id: Long, val autor: String, val autorNombre: String, val texto: String, val enviadoEn: String, val propio: Boolean
)
data class AbrirChat(val apartamentoId: Long? = null, val participante: String? = null)
data class MensajeNuevo(val texto: String)
// Revisión de la administración; el motivo solo al rechazar
data class RevisionObra(val aprobada: Boolean, val motivo: String? = null)
data class RevisionPersonal(val aprobado: Boolean, val motivo: String? = null)
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
// abierta/apertura/cierre/motivo (P5): el horario de ese día (semanal o general) o por qué está cerrada
data class DiaDisponibilidadDTO(
    val fecha: String, val ocupadas: List<FranjaOcupadaDTO>,
    val abierta: Boolean? = null, val apertura: String? = null, val cierre: String? = null, val motivo: String? = null
)
// Horario de una zona por día de la semana (1 = lunes) y fechas bloqueadas (P5)
data class DiaHorarioZona(val diaSemana: Int, val apertura: String, val cierre: String)
data class BloqueoZona(val id: Long?, val fecha: String, val motivo: String?)
data class HorarioZona(val semanal: List<DiaHorarioZona>, val bloqueos: List<BloqueoZona>)
data class HorarioSemanal(val semanal: List<DiaHorarioZona>)
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
data class DispositivoRequest(val token: String, val tipoApp: String? = null, val applicationId: String? = null)
// P6 bloque 6: pago en línea con Wompi (el servidor firma la dirección; la app solo la abre)
data class CheckoutRequest(val cuotaId: Long)
data class CheckoutDTO(val url: String, val referencia: String, val montoCentavos: Long)
// P6 bloque 5: portería. telefono solo lo ve la portería
data class VisitaDTO(
    val id: Long, val apartamentoId: Long, val torre: String, val apto: String, val visitante: String,
    val documento: String?, val placa: String?, val motivo: String?, val estado: String,
    val creadaEn: String, val venceEn: String, val respondidaEn: String?, val canal: String?, val telefono: String?
)
data class NuevaVisita(
    val torre: String, val apto: String, val visitante: String,
    val documento: String? = null, val placa: String? = null, val motivo: String? = null
)
data class RespuestaVisita(val decision: String)
// P6: huella con credencial opaca por celular (nunca la contraseña)
data class ActivarBiometria(val dispositivoId: String)
data class BiometriaActivada(val credencial: String)
data class LoginBiometria(val dispositivoId: String, val credencial: String)
// P6: textos legales con versión, consentimientos y solicitudes de datos personales
data class TextoLegalDTO(val id: Long, val tipo: String, val version: Int, val titulo: String, val contenido: String)
data class AceptarTextos(val textoIds: List<Long>)
data class NuevaSolicitudDatos(val tipo: String, val detalle: String?)
data class SolicitudDatosDTO(
    val id: Long, val nombre: String?, val tipo: String, val estado: String, val detalle: String?,
    val respuesta: String?, val creadaEn: String, val resueltaEn: String?
)
data class ResolverSolicitudDatos(val estado: String, val respuesta: String?)
// {"total": n}
data class ConteoDTO(val total: Long)