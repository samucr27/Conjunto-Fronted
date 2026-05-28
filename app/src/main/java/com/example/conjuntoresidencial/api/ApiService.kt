package com.example.conjuntoresidencial.api

import com.example.conjuntoresidencial.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("api/auth/admin/login")
    suspend fun loginAdmin(@Body request: LoginAdminRequest): Response<Void>

    @POST("api/auth/apartamento/login")
    suspend fun loginApartamento(@Body request: LoginApartamentoRequest): Response<Apartamento>

    @GET("api/apartamentos")
    suspend fun getApartamentos(): Response<List<Apartamento>>

    @GET("api/apartamentos/{id}")
    suspend fun getApartamentoById(@Path("id") id: Long): Response<Apartamento>

    @POST("api/apartamentos")
    suspend fun createApartamento(@Body apartamento: Apartamento): Response<Apartamento>

    @PUT("api/apartamentos/{id}")
    suspend fun updateApartamento(@Path("id") id: Long, @Body apartamento: Apartamento): Response<Apartamento>

    @GET("api/residentes/apartamento/{id}")
    suspend fun getResidentesPorApartamento(@Path("id") id: Long): Response<List<ResidenteDTO>>

    @POST("api/residentes")
    suspend fun createResidente(@Body residente: ResidenteDTO): Response<ResidenteDTO>

    @PUT("api/residentes/{id}")
    suspend fun updateResidente(@Path("id") id: Long, @Body residente: ResidenteDTO): Response<ResidenteDTO>

    @DELETE("api/residentes/{id}")
    suspend fun deleteResidente(@Path("id") id: Long): Response<Void>

    @GET("api/vehiculos/apartamento/{id}")
    suspend fun getVehiculosPorApartamento(@Path("id") id: Long): Response<List<VehiculoDTO>>

    @POST("api/vehiculos")
    suspend fun createVehiculo(@Body vehiculo: VehiculoDTO): Response<VehiculoDTO>

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

    @GET("api/personal-obra")
    suspend fun getTodoElPersonal(): Response<List<PersonalObraDTO>>

    @GET("api/personal-obra/obra/{id}")
    suspend fun getPersonalPorObra(@Path("id") id: Long): Response<List<PersonalObraDTO>>

    @POST("api/personal-obra")
    suspend fun createPersonalObra(@Body personal: PersonalObraDTO): Response<PersonalObraDTO>

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

    @GET("api/reservas")
    suspend fun getReservas(): Response<List<ReservaDTO>>

    @GET("api/reservas/apartamento/{id}")
    suspend fun getReservasPorApartamento(@Path("id") id: Long): Response<List<ReservaDTO>>

    @POST("api/reservas")
    suspend fun createReserva(@Body reserva: ReservaDTO): Response<ReservaDTO>

    @PUT("api/reservas/{id}/estado")
    suspend fun updateEstadoReserva(@Path("id") id: Long, @Body estado: EstadoRequest): Response<ReservaDTO>

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
}