package com.example.conjuntoresidencial.model

/**
 * P11 bloques 6 y 7: la cartera del contador y sus solicitudes al administrador (con versiones,
 * historial y detalle completo). Ver ContabilidadService en el backend.
 */

/** Una solicitud del contador: FACTURACION, CUOTA_NUEVA, CUOTA_CAMBIO, CUOTA_ANULAR o PAGO_MANUAL. */
data class SolicitudContableDTO(
    val id: Long, val tipo: String, val estado: String, val version: Int = 1, val edicion: Boolean = false,
    val resumen: String? = null, val periodo: String? = null, val valor: Double? = null, val fechaLimite: String? = null,
    val cuotaId: Long? = null, val apartamentoId: Long? = null, val torre: String? = null, val apto: String? = null,
    val solicitadaPor: String? = null, val creadaEn: String? = null, val actualizadaEn: String? = null,
    val revisadaEn: String? = null, val revisadaPor: String? = null, val motivoRechazo: String? = null, val nota: String? = null
)

data class LineaDTO(val etiqueta: String, val valor: String?)
data class PasoHistorialDTO(val version: Int, val accion: String, val autor: String?, val cuando: String?, val motivo: String?,
                            val lineas: List<LineaDTO> = emptyList())
data class ValorAptoDTO(val apartamentoId: Long, val torre: String, val apto: String, val valor: Double)
data class VistaPreviaDTO(val apartamentos: Int, val total: Double, val minimo: Double?, val maximo: Double?, val yaTienen: Int,
                          val sinCoeficiente: Int, val valores: List<ValorAptoDTO> = emptyList(), val avisos: List<String> = emptyList())

/** detalle: lo que pide esta versión, tal cual (para volver a llenar el formulario al corregir o editar). */
data class DetalleSolicitudDTO(
    val solicitud: SolicitudContableDTO, val detalle: Map<String, Any?>?, val lineas: List<LineaDTO> = emptyList(),
    val lineasAprobadas: List<LineaDTO> = emptyList(), val vistaPrevia: VistaPreviaDTO? = null,
    val historial: List<PasoHistorialDTO> = emptyList(), val puedeEditar: Boolean = false, val puedeRevisar: Boolean = false
)

data class EnvioSolicitud(val tipo: String, val detalle: Map<String, Any?>, val nota: String? = null)
data class PropuestaSolicitud(val detalle: Map<String, Any?>, val nota: String? = null)

/** Facturación del mes: modo FIJO (valor por apartamento) o COEFICIENTE (total repartido). */
data class FacturacionBorrador(
    val periodo: String?, val concepto: String?, val modo: String, val valor: Double?, val total: Double?,
    val fechaLimite: String?, val interesMora: Double?, val descripcion: String?, val excluidos: List<Long> = emptyList()
)

data class AptoCarteraDTO(
    val apartamentoId: Long, val torre: String, val apto: String, val saldo: Double, val estadoCuenta: String?,
    val pendientes: Int, val vencidas: Int, val coeficiente: Double?, val solicitudesAbiertas: Int
)
data class CarteraApartamentoDTO(val resumen: AptoCarteraDTO, val cuotas: List<CuotaDTO>, val solicitudes: List<SolicitudContableDTO>)
data class CoeficienteDTO(val coeficiente: Double?)
data class RecaudoDTO(val periodo: String, val facturado: Double, val recaudado: Double)
data class PanelContadorDTO(
    val mes: EstadisticasCartera, val morosos: List<AptoCarteraDTO>, val misPendientes: Long, val misDevueltas: Long,
    val pendientes: Long, val recaudo: List<RecaudoDTO>
)
