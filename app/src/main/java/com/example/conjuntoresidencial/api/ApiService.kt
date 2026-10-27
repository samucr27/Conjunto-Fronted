package com.example.conjuntoresidencial.api

import com.example.conjuntoresidencial.model.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("api/auth/admin/login")
    suspend fun loginAdmin(@Body request: LoginAdminRequest): Response<LoginResponse>

    @POST("api/auth/apartamento/login")
    suspend fun loginApartamento(@Body request: LoginApartamentoRequest): Response<LoginResponse>

    @POST("api/auth/residente/login")
    suspend fun loginResidente(@Body request: LoginResidenteRequest): Response<LoginResponse>

    // Registro del residente (P5): sin sesión
    @POST("api/auth/registro/verificar")
    suspend fun verificarRegistro(@Body request: VerificarRegistroRequest): Response<RegistroVerificado>

    @POST("api/auth/registro")
    suspend fun registrar(@Body request: RegistroRequest): Response<RegistroRecibido>

    // Cuenta → Perfil: cambiar la propia contraseña (400 si la actual no coincide)
    @PUT("api/auth/contrasena")
    suspend fun cambiarContrasena(@Body request: CambioPropiaContrasenaRequest): Response<Void>

    @GET("api/apartamentos")
    suspend fun getApartamentos(): Response<List<Apartamento>>

    @GET("api/apartamentos/{id}")
    suspend fun getApartamentoById(@Path("id") id: Long): Response<Apartamento>

    @POST("api/apartamentos")
    suspend fun createApartamento(@Body apartamento: ApartamentoRequest): Response<Apartamento>

    @PUT("api/apartamentos/{id}")
    suspend fun updateApartamento(@Path("id") id: Long, @Body apartamento: ApartamentoRequest): Response<Apartamento>

    @DELETE("api/apartamentos/{id}")
    suspend fun deleteApartamento(@Path("id") id: Long): Response<Void>

    @PUT("api/apartamentos/{id}/citofonos")
    suspend fun cambiarCitofonos(@Path("id") id: Long, @Body citofonos: CitofonosRequest): Response<Apartamento>

    @PUT("api/apartamentos/{id}/contrasena")
    suspend fun restablecerContrasena(@Path("id") id: Long, @Body request: CambioContrasenaRequest): Response<Void>

    @GET("api/residentes/apartamento/{id}")
    suspend fun getResidentesPorApartamento(@Path("id") id: Long): Response<List<ResidenteDTO>>

    @POST("api/residentes")
    suspend fun createResidente(@Body residente: ResidenteDTO): Response<ResidenteDTO>

    @PUT("api/residentes/{id}")
    suspend fun updateResidente(@Path("id") id: Long, @Body residente: ResidenteDTO): Response<ResidenteDTO>

    @DELETE("api/residentes/{id}")
    suspend fun deleteResidente(@Path("id") id: Long): Response<Void>

    // P5: el arrendatario del apartamento (204 = no tiene) y la decisión de su propietario
    @GET("api/residentes/apartamento/{id}/arrendatario")
    suspend fun getArrendatario(@Path("id") apartamentoId: Long): Response<ResidenteDTO>

    @PUT("api/residentes/{id}/aprobacion-propietario")
    suspend fun aprobarArrendatario(@Path("id") id: Long, @Body aprobacion: AprobacionPropietario): Response<ResidenteDTO>

    // Feed de publicaciones (P5): lo ven todos; publica la administración
    @GET("api/publicaciones")
    suspend fun getPublicaciones(@Query("pagina") pagina: Int): Response<PaginaPublicaciones>

    @Multipart
    @POST("api/publicaciones")
    suspend fun publicar(@Part("texto") texto: okhttp3.RequestBody?, @Part archivos: List<MultipartBody.Part>): Response<PublicacionDTO>

    @PUT("api/publicaciones/{id}")
    suspend fun editarPublicacion(@Path("id") id: Long, @Body edicion: EdicionPublicacion): Response<PublicacionDTO>

    @DELETE("api/publicaciones/{id}")
    suspend fun eliminarPublicacion(@Path("id") id: Long): Response<Void>

    // Visitantes frecuentes (Mi hogar)
    @GET("api/visitantes/apartamento/{id}")
    suspend fun getVisitantes(@Path("id") apartamentoId: Long): Response<List<VisitanteDTO>>

    @POST("api/visitantes")
    suspend fun createVisitante(@Body visitante: VisitanteDTO): Response<VisitanteDTO>

    @PUT("api/visitantes/{id}")
    suspend fun updateVisitante(@Path("id") id: Long, @Body visitante: VisitanteDTO): Response<VisitanteDTO>

    @DELETE("api/visitantes/{id}")
    suspend fun deleteVisitante(@Path("id") id: Long): Response<Void>

    // Arrendatarios que agregaron los propietarios (administrador)
    @GET("api/residentes/pendientes")
    suspend fun getResidentesPendientes(): Response<List<ResidenteDTO>>

    @PUT("api/residentes/{id}/estado")
    suspend fun revisarResidente(@Path("id") id: Long, @Body revision: RevisionResidente): Response<ResidenteDTO>

    @PUT("api/residentes/{id}/acceso")
    suspend fun cambiarAccesoResidente(@Path("id") id: Long, @Body acceso: AccesoResidente): Response<ResidenteDTO>

    // Facturación mensual (P4 bloque 7). Contador: por mes y/o estado
    @GET("api/cuotas")
    suspend fun getCuotas(@Query("periodo") periodo: String?, @Query("estado") estado: String?): Response<List<CuotaDTO>>

    @GET("api/cuotas/apartamento/{id}")
    suspend fun getCuotasPorApartamento(@Path("id") id: Long): Response<List<CuotaDTO>>

    @POST("api/cuotas/generar")
    suspend fun generarCuotas(@Body solicitud: GenerarCuotasRequest): Response<CuotasGeneradas>

    @Multipart
    @POST("api/cuotas/{id}/comprobante")
    suspend fun subirComprobante(@Path("id") id: Long, @Part archivo: MultipartBody.Part): Response<CuotaDTO>

    @GET("api/cuotas/{id}/comprobante")
    suspend fun getComprobante(@Path("id") id: Long): Response<EnlaceComprobante>

    @PUT("api/cuotas/{id}")
    suspend fun updateCuota(@Path("id") id: Long, @Body cambio: CambioCuota): Response<CuotaDTO>

    @DELETE("api/cuotas/{id}")
    suspend fun deleteCuota(@Path("id") id: Long): Response<Void>

    @PUT("api/cuotas/{id}/estado")
    suspend fun revisarCuota(@Path("id") id: Long, @Body revision: RevisionCuota): Response<CuotaDTO>

    // Cuentas de la administración (solo el administrador)
    @GET("api/administradores")
    suspend fun getCuentasAdministracion(): Response<List<CuentaAdministracion>>

    @POST("api/administradores")
    suspend fun crearCuentaAdministracion(@Body cuenta: CuentaAdministracion): Response<CuentaAdministracion>

    @PUT("api/administradores/{id}")
    suspend fun updateCuentaAdministracion(@Path("id") id: Long, @Body cuenta: CuentaAdministracion): Response<CuentaAdministracion>

    @DELETE("api/administradores/{id}")
    suspend fun eliminarCuentaAdministracion(@Path("id") id: Long): Response<Void>

    @GET("api/vehiculos/apartamento/{id}")
    suspend fun getVehiculosPorApartamento(@Path("id") id: Long): Response<List<VehiculoDTO>>

    @POST("api/vehiculos")
    suspend fun createVehiculo(@Body vehiculo: VehiculoDTO): Response<VehiculoDTO>

    @PUT("api/vehiculos/{id}")
    suspend fun updateVehiculo(@Path("id") id: Long, @Body vehiculo: VehiculoDTO): Response<VehiculoDTO>

    @DELETE("api/vehiculos/{id}")
    suspend fun deleteVehiculo(@Path("id") id: Long): Response<Void>

    @GET("api/obras/apartamento/{id}")
    suspend fun getObrasPorApartamento(@Path("id") id: Long): Response<List<ObraDTO>>

    @GET("api/obras/en-proceso")
    suspend fun getObrasEnProceso(): Response<List<ObraDTO>>

    @POST("api/obras")
    suspend fun createObra(@Body obra: ObraDTO): Response<ObraDTO>

    @PUT("api/obras/{id}")
    suspend fun updateObra(@Path("id") id: Long, @Body obra: ObraDTO): Response<ObraDTO>

    @DELETE("api/obras/{id}")
    suspend fun deleteObra(@Path("id") id: Long): Response<Void>

    // P5 bloque 6: detalle, búsqueda y aprobación de obras; certificado ARL de cada trabajador
    @GET("api/obras/{id}")
    suspend fun getObra(@Path("id") id: Long): Response<ObraDTO>

    @GET("api/obras")
    suspend fun buscarObras(@Query("alcance") alcance: String, @Query("estado") estado: String): Response<List<ObraDTO>>

    @PUT("api/obras/{id}/aprobacion")
    suspend fun revisarObra(@Path("id") id: Long, @Body revision: RevisionObra): Response<ObraDTO>

    @PUT("api/personal-obra/{id}/aprobacion")
    suspend fun revisarPersonal(@Path("id") id: Long, @Body revision: RevisionPersonal): Response<PersonalObraDTO>

    @Multipart
    @POST("api/personal-obra/{id}/arl")
    suspend fun subirCertificadoArl(@Path("id") id: Long, @Part archivo: MultipartBody.Part): Response<PersonalObraDTO>

    @GET("api/personal-obra/{id}/arl")
    suspend fun getCertificadoArl(@Path("id") id: Long): Response<EnlaceComprobante>

    @GET("api/personal-obra")
    suspend fun getTodoElPersonal(): Response<List<PersonalObraDTO>>

    @GET("api/personal-obra/obra/{id}")
    suspend fun getPersonalPorObra(@Path("id") id: Long): Response<List<PersonalObraDTO>>

    @POST("api/personal-obra")
    suspend fun createPersonalObra(@Body personal: PersonalObraDTO): Response<PersonalObraDTO>

    @PUT("api/personal-obra/{id}")
    suspend fun updatePersonalObra(@Path("id") id: Long, @Body personal: PersonalObraDTO): Response<PersonalObraDTO>

    @DELETE("api/personal-obra/{id}")
    suspend fun deletePersonalObra(@Path("id") id: Long): Response<Void>

    @GET("api/zonas")
    suspend fun getZonasComunes(): Response<List<ZonaComunDTO>>

    @GET("api/zonas/{id}")
    suspend fun getZonaComunById(@Path("id") id: Long): Response<ZonaComunDTO>

    @POST("api/zonas")
    suspend fun createZonaComun(@Body zona: ZonaComunDTO): Response<ZonaComunDTO>

    @PUT("api/zonas/{id}")
    suspend fun updateZonaComun(@Path("id") id: Long, @Body zona: ZonaComunDTO): Response<ZonaComunDTO>

    // Calendario de reservas (P4): lo ocupado de la zona entre dos fechas (máximo 62 días)
    @DELETE("api/zonas/{id}")
    suspend fun deleteZona(@Path("id") id: Long): Response<Void>

    @GET("api/zonas/{id}/disponibilidad")
    suspend fun getDisponibilidad(
        @Path("id") zonaId: Long, @Query("desde") desde: String, @Query("hasta") hasta: String
    ): Response<DisponibilidadDTO>

    @GET("api/reservas")
    suspend fun getReservas(): Response<List<ReservaDTO>>

    @GET("api/reservas/apartamento/{id}")
    suspend fun getReservasPorApartamento(@Path("id") id: Long): Response<List<ReservaDTO>>

    @POST("api/reservas")
    suspend fun createReserva(@Body reserva: ReservaDTO): Response<ReservaDTO>

    // P5: el residente edita su reserva pendiente
    @PUT("api/reservas/{id}")
    suspend fun updateReserva(@Path("id") id: Long, @Body reserva: ReservaDTO): Response<ReservaDTO>

    // Horario semanal y fechas bloqueadas de una zona (P5)
    @GET("api/zonas/{id}/horario")
    suspend fun getHorarioZona(@Path("id") id: Long): Response<HorarioZona>

    @PUT("api/zonas/{id}/horario")
    suspend fun guardarHorarioZona(@Path("id") id: Long, @Body semanal: HorarioSemanal): Response<HorarioZona>

    @POST("api/zonas/{id}/bloqueos")
    suspend fun bloquearFecha(@Path("id") id: Long, @Body bloqueo: BloqueoZona): Response<BloqueoZona>

    @DELETE("api/zonas/{id}/bloqueos/{bloqueoId}")
    suspend fun desbloquearFecha(@Path("id") id: Long, @Path("bloqueoId") bloqueoId: Long): Response<Void>

    // El administrador cualquiera; el residente cancela la suya si aún no empieza
    @DELETE("api/reservas/{id}")
    suspend fun deleteReserva(@Path("id") id: Long): Response<Void>

    @PUT("api/reservas/{id}/estado")
    suspend fun updateEstadoReserva(@Path("id") id: Long, @Body estado: EstadoRequest): Response<ReservaDTO>

    // Cartera completa (solo admin): una petición en vez de una por apartamento
    @GET("api/pagos")
    suspend fun getPagos(): Response<List<PagoDTO>>

    @GET("api/pagos/apartamento/{id}")
    suspend fun getPagosPorApartamento(@Path("id") id: Long): Response<PagoDTO>

    @PUT("api/pagos/apartamento/{id}/simular")
    suspend fun simularPagoApartamento(@Path("id") id: Long): Response<Void>

    @POST("api/pagos")
    suspend fun createPago(@Body pago: PagoDTO): Response<PagoDTO>

    @PUT("api/pagos/{id}")
    suspend fun updatePago(@Path("id") id: Long, @Body pago: PagoDTO): Response<PagoDTO>

    @GET("api/obras")
    suspend fun getAllObras(): Response<List<ObraDTO>>

    @PUT("api/obras/{id}/estado")
    suspend fun cambiarEstadoObra(@Path("id") id: Long, @Body estado: EstadoRequest): Response<ObraDTO>

    // Notificaciones del administrador
    @GET("api/notificaciones")
    suspend fun getNotificaciones(): Response<List<NotificacionDTO>>

    // Solo el número, para el contador del panel
    @GET("api/notificaciones/no-leidas")
    suspend fun getNotificacionesNoLeidas(): Response<ConteoDTO>

    @PUT("api/notificaciones/leidas")
    suspend fun marcarNotificacionesLeidas(): Response<Void>

    @DELETE("api/notificaciones")
    suspend fun eliminarNotificaciones(): Response<Void>

    // Vehículos (P4): por aprobar, aprobar o rechazar, y cupos por tipo
    @GET("api/vehiculos")
    suspend fun getVehiculosPorEstado(@Query("estado") estado: String): Response<List<VehiculoDTO>>

    @PUT("api/vehiculos/{id}/estado")
    suspend fun cambiarEstadoVehiculo(@Path("id") id: Long, @Body request: EstadoVehiculoRequest): Response<VehiculoDTO>

    // Con apartamentoId: los cupos que rigen para ese apartamento (P5)
    @GET("api/vehiculos/cupos")
    suspend fun getCuposVehiculo(@Query("apartamentoId") apartamentoId: Long? = null): Response<List<CupoVehiculoDTO>>

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

    @PUT("api/notificaciones/{id}/leida")
    suspend fun marcarNotificacionLeida(@Path("id") id: Long): Response<Void>

    @DELETE("api/notificaciones/{id}")
    suspend fun eliminarNotificacion(@Path("id") id: Long): Response<Void>

    // Celular que recibe avisos (al iniciar sesión) y su baja (al cerrarla; no pide sesión)
    @POST("api/dispositivos")
    suspend fun registrarDispositivo(@Body dispositivo: DispositivoRequest): Response<Void>

    @POST("api/dispositivos/baja")
    suspend fun darDeBajaDispositivo(@Body dispositivo: DispositivoRequest): Response<Void>
}