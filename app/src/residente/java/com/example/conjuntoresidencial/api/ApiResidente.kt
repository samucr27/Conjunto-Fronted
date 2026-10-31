package com.example.conjuntoresidencial.api

import com.example.conjuntoresidencial.model.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

/** Rutas que solo usa la app de residentes. */
interface ApiResidente : ApiComun {
    // Registro del residente (P5): sin sesión
    @POST("api/auth/registro/verificar")
    suspend fun verificarRegistro(@Body request: VerificarRegistroRequest): Response<RegistroVerificado>

    @PUT("api/apartamentos/{id}/citofonos")
    suspend fun cambiarCitofonos(@Path("id") id: Long, @Body citofonos: CitofonosRequest): Response<Apartamento>

    // P5: el arrendatario del apartamento (204 = no tiene) y la decisión de su propietario
    @GET("api/residentes/apartamento/{id}/arrendatario")
    suspend fun getArrendatario(@Path("id") apartamentoId: Long): Response<ResidenteDTO>

    @PUT("api/residentes/{id}/aprobacion-propietario")
    suspend fun aprobarArrendatario(@Path("id") id: Long, @Body aprobacion: AprobacionPropietario): Response<ResidenteDTO>

    // Visitantes frecuentes (Mi hogar)
    @GET("api/visitantes/apartamento/{id}")
    suspend fun getVisitantes(@Path("id") apartamentoId: Long): Response<List<VisitanteDTO>>

    @POST("api/visitantes")
    suspend fun createVisitante(@Body visitante: VisitanteDTO): Response<VisitanteDTO>

    @PUT("api/visitantes/{id}")
    suspend fun updateVisitante(@Path("id") id: Long, @Body visitante: VisitanteDTO): Response<VisitanteDTO>

    @DELETE("api/visitantes/{id}")
    suspend fun deleteVisitante(@Path("id") id: Long): Response<Void>

    @GET("api/cuotas/apartamento/{id}")
    suspend fun getCuotasPorApartamento(@Path("id") id: Long): Response<List<CuotaDTO>>

    @Multipart
    @POST("api/cuotas/{id}/comprobante")
    suspend fun subirComprobante(@Path("id") id: Long, @Part archivo: MultipartBody.Part): Response<CuotaDTO>

    @GET("api/reservas/apartamento/{id}")
    suspend fun getReservasPorApartamento(@Path("id") id: Long): Response<List<ReservaDTO>>

    @GET("api/pagos/apartamento/{id}")
    suspend fun getPagosPorApartamento(@Path("id") id: Long): Response<PagoDTO>

    @PUT("api/pagos/apartamento/{id}/simular")
    suspend fun simularPagoApartamento(@Path("id") id: Long): Response<Void>
}
