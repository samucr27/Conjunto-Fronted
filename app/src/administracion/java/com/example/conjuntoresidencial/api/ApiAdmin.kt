package com.example.conjuntoresidencial.api

import com.example.conjuntoresidencial.model.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

/** Rutas que solo usa la app de administración. */
interface ApiAdmin : ApiComun {
    // P6: ocupación del apartamento y banderas del arrendatario
    @PATCH("api/apartamentos/{id}/ocupacion-y-banderas")
    suspend fun cambiarOcupacionYBanderas(@Path("id") id: Long, @Body cambio: OcupacionYBanderas): Response<Apartamento>

    @GET("api/apartamentos")
    suspend fun getApartamentos(): Response<List<Apartamento>>

    @POST("api/apartamentos")
    suspend fun createApartamento(@Body apartamento: ApartamentoRequest): Response<Apartamento>

    @PUT("api/apartamentos/{id}")
    suspend fun updateApartamento(@Path("id") id: Long, @Body apartamento: ApartamentoRequest): Response<Apartamento>

    @DELETE("api/apartamentos/{id}")
    suspend fun deleteApartamento(@Path("id") id: Long): Response<Void>

    @PUT("api/apartamentos/{id}/contrasena")
    suspend fun restablecerContrasena(@Path("id") id: Long, @Body request: CambioContrasenaRequest): Response<Void>

    // P5 bloque 8: chat
    @GET("api/chat/conversaciones")
    suspend fun getConversaciones(): Response<List<ConversacionDTO>>

    // P5 bloque 7: cartera de la administración y cupos del equipo
    @GET("api/cartera/estadisticas")
    suspend fun getEstadisticasCartera(@Query("periodo") periodo: String?): Response<EstadisticasCartera>

    @GET("api/cartera/solicitudes")
    suspend fun getSolicitudesContables(@Query("estado") estado: String?): Response<List<SolicitudContable>>

    @PUT("api/cartera/solicitudes/{id}")
    suspend fun revisarSolicitudContable(@Path("id") id: Long, @Body revision: RevisionSolicitud): Response<SolicitudContable>

    @GET("api/administradores/cupos")
    suspend fun getCuposEquipo(): Response<List<CupoEquipo>>

    @PUT("api/administradores/cupos")
    suspend fun fijarCuposEquipo(@Body cupos: List<CupoEquipo>): Response<List<CupoEquipo>>

    // Cuentas de la administración (solo el administrador)
    @GET("api/administradores")
    suspend fun getCuentasAdministracion(): Response<List<CuentaAdministracion>>

    @POST("api/administradores")
    suspend fun crearCuentaAdministracion(@Body cuenta: CuentaAdministracion): Response<CuentaAdministracion>

    @PUT("api/administradores/{id}")
    suspend fun updateCuentaAdministracion(@Path("id") id: Long, @Body cuenta: CuentaAdministracion): Response<CuentaAdministracion>

    @DELETE("api/administradores/{id}")
    suspend fun eliminarCuentaAdministracion(@Path("id") id: Long): Response<Void>

    @GET("api/obras")
    suspend fun buscarObras(@Query("alcance") alcance: String, @Query("estado") estado: String): Response<List<ObraDTO>>

    @GET("api/personal-obra")
    suspend fun getTodoElPersonal(): Response<List<PersonalObraDTO>>

    @GET("api/zonas/{id}")
    suspend fun getZonaComunById(@Path("id") id: Long): Response<ZonaComunDTO>

    @POST("api/zonas")
    suspend fun createZonaComun(@Body zona: ZonaComunDTO): Response<ZonaComunDTO>

    @PUT("api/zonas/{id}")
    suspend fun updateZonaComun(@Path("id") id: Long, @Body zona: ZonaComunDTO): Response<ZonaComunDTO>

    // Calendario de reservas (P4): lo ocupado de la zona entre dos fechas (máximo 62 días)
    @DELETE("api/zonas/{id}")
    suspend fun deleteZona(@Path("id") id: Long): Response<Void>

    @GET("api/reservas")
    suspend fun getReservas(): Response<List<ReservaDTO>>

    // Horario semanal y fechas bloqueadas de una zona (P5)
    @GET("api/zonas/{id}/horario")
    suspend fun getHorarioZona(@Path("id") id: Long): Response<HorarioZona>

    @PUT("api/zonas/{id}/horario")
    suspend fun guardarHorarioZona(@Path("id") id: Long, @Body semanal: HorarioSemanal): Response<HorarioZona>

    @POST("api/zonas/{id}/bloqueos")
    suspend fun bloquearFecha(@Path("id") id: Long, @Body bloqueo: BloqueoZona): Response<BloqueoZona>

    @DELETE("api/zonas/{id}/bloqueos/{bloqueoId}")
    suspend fun desbloquearFecha(@Path("id") id: Long, @Path("bloqueoId") bloqueoId: Long): Response<Void>

    @PUT("api/reservas/{id}/estado")
    suspend fun updateEstadoReserva(@Path("id") id: Long, @Body estado: EstadoRequest): Response<ReservaDTO>

    // Cartera completa (solo admin): una petición en vez de una por apartamento
    @GET("api/pagos")
    suspend fun getPagos(): Response<List<PagoDTO>>

    // Vehículos (P4): por aprobar, aprobar o rechazar, y cupos por tipo
    @GET("api/vehiculos")
    suspend fun getVehiculosPorEstado(@Query("estado") estado: String): Response<List<VehiculoDTO>>

    @PUT("api/vehiculos/{id}/estado")
    suspend fun cambiarEstadoVehiculo(@Path("id") id: Long, @Body request: EstadoVehiculoRequest): Response<VehiculoDTO>

    @PUT("api/vehiculos/cupos/apartamento/{id}")
    suspend fun guardarCuposApartamento(@Path("id") apartamentoId: Long, @Body cupos: List<CupoApartamentoRequest>): Response<List<CupoVehiculoDTO>>

    // Parqueaderos (administrador, P5)
    @GET("api/parqueaderos")
    suspend fun getParqueaderos(@Query("tipo") tipo: String?, @Query("estado") estado: String?): Response<List<ParqueaderoDTO>>

    @GET("api/parqueaderos/resumen")
    suspend fun getResumenParqueaderos(): Response<ResumenParqueaderos>

    @POST("api/parqueaderos/generar")
    suspend fun generarParqueaderos(@Body pedido: GenerarParqueaderos): Response<List<ParqueaderoDTO>>

    @PUT("api/parqueaderos/{id}/asignacion")
    suspend fun asignarParqueadero(@Path("id") id: Long, @Body asignacion: AsignacionParqueadero): Response<ParqueaderoDTO>

    @DELETE("api/parqueaderos/{id}")
    suspend fun eliminarParqueadero(@Path("id") id: Long): Response<Void>

    @PUT("api/vehiculos/cupos")
    suspend fun guardarCuposVehiculo(@Body cupos: List<CupoVehiculoDTO>): Response<List<CupoVehiculoDTO>>
}
