package com.example.conjuntoresidencial.api

import com.example.conjuntoresidencial.model.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

/** Rutas que usan las dos apps (P6). Las firmas y rutas son las de antes de separarlas. */
interface ApiComun {
    @POST("api/auth/admin/login")
    suspend fun loginAdmin(@Body request: LoginAdminRequest): Response<LoginResponse>

    @POST("api/auth/apartamento/login")
    suspend fun loginApartamento(@Body request: LoginApartamentoRequest): Response<LoginResponse>

    @POST("api/auth/residente/login")
    suspend fun loginResidente(@Body request: LoginResidenteRequest): Response<LoginResponse>

    @POST("api/auth/registro")
    suspend fun registrar(@Body request: RegistroRequest): Response<RegistroRecibido>

    // Cuenta → Perfil: cambiar la propia contraseña (400 si la actual no coincide)
    @PUT("api/auth/contrasena")
    suspend fun cambiarContrasena(@Body request: CambioPropiaContrasenaRequest): Response<Void>

    @GET("api/apartamentos/{id}")
    suspend fun getApartamentoById(@Path("id") id: Long): Response<Apartamento>

    @GET("api/residentes/apartamento/{id}")
    suspend fun getResidentesPorApartamento(@Path("id") id: Long): Response<List<ResidenteDTO>>

    @POST("api/residentes")
    suspend fun createResidente(@Body residente: ResidenteDTO): Response<ResidenteDTO>

    @PUT("api/residentes/{id}")
    suspend fun updateResidente(@Path("id") id: Long, @Body residente: ResidenteDTO): Response<ResidenteDTO>

    @DELETE("api/residentes/{id}")
    suspend fun deleteResidente(@Path("id") id: Long): Response<Void>

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

    @POST("api/chat/conversaciones")
    suspend fun abrirChat(@Body pedido: AbrirChat): Response<ConversacionDTO>

    @GET("api/chat/conversaciones/{id}/mensajes")
    suspend fun getMensajes(@Path("id") id: Long, @Query("despuesDe") despuesDe: Long?): Response<List<MensajeChatDTO>>

    @POST("api/chat/conversaciones/{id}/mensajes")
    suspend fun enviarMensaje(@Path("id") id: Long, @Body mensaje: MensajeNuevo): Response<MensajeChatDTO>

    @PUT("api/chat/conversaciones/{id}/leido")
    suspend fun marcarChatLeido(@Path("id") id: Long): Response<ConversacionDTO>

    @POST("api/cuotas/generar")
    suspend fun generarCuotas(@Body solicitud: GenerarCuotasRequest): Response<CuotasGeneradas>

    @GET("api/cuotas/{id}/comprobante")
    suspend fun getComprobante(@Path("id") id: Long): Response<EnlaceComprobante>

    @PUT("api/cuotas/{id}")
    suspend fun updateCuota(@Path("id") id: Long, @Body cambio: CambioCuota): Response<CuotaDTO>

    @DELETE("api/cuotas/{id}")
    suspend fun deleteCuota(@Path("id") id: Long): Response<Void>

    @PUT("api/cuotas/{id}/estado")
    suspend fun revisarCuota(@Path("id") id: Long, @Body revision: RevisionCuota): Response<CuotaDTO>

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

    @PUT("api/obras/{id}/aprobacion")
    suspend fun revisarObra(@Path("id") id: Long, @Body revision: RevisionObra): Response<ObraDTO>

    @PUT("api/personal-obra/{id}/aprobacion")
    suspend fun revisarPersonal(@Path("id") id: Long, @Body revision: RevisionPersonal): Response<PersonalObraDTO>

    @Multipart
    @POST("api/personal-obra/{id}/arl")
    suspend fun subirCertificadoArl(@Path("id") id: Long, @Part archivo: MultipartBody.Part): Response<PersonalObraDTO>

    @GET("api/personal-obra/{id}/arl")
    suspend fun getCertificadoArl(@Path("id") id: Long): Response<EnlaceComprobante>

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

    @GET("api/zonas/{id}/disponibilidad")
    suspend fun getDisponibilidad(
        @Path("id") zonaId: Long, @Query("desde") desde: String, @Query("hasta") hasta: String
    ): Response<DisponibilidadDTO>

    @POST("api/reservas")
    suspend fun createReserva(@Body reserva: ReservaDTO): Response<ReservaDTO>

    // P5: el residente edita su reserva pendiente
    @PUT("api/reservas/{id}")
    suspend fun updateReserva(@Path("id") id: Long, @Body reserva: ReservaDTO): Response<ReservaDTO>

    // El administrador cualquiera; el residente cancela la suya si aún no empieza
    @DELETE("api/reservas/{id}")
    suspend fun deleteReserva(@Path("id") id: Long): Response<Void>

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

    // Con apartamentoId: los cupos que rigen para ese apartamento (P5)
    @GET("api/vehiculos/cupos")
    suspend fun getCuposVehiculo(@Query("apartamentoId") apartamentoId: Long? = null): Response<List<CupoVehiculoDTO>>

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
