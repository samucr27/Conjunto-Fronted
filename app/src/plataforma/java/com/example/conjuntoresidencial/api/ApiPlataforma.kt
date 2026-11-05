package com.example.conjuntoresidencial.api

import com.example.conjuntoresidencial.model.AccesoPrincipal
import com.example.conjuntoresidencial.model.CambioEstadoConjunto
import com.example.conjuntoresidencial.model.CambioPropiaContrasenaRequest
import com.example.conjuntoresidencial.model.ConjuntoPlataforma
import com.example.conjuntoresidencial.model.CuentaConjunto
import com.example.conjuntoresidencial.model.DatosConjunto
import com.example.conjuntoresidencial.model.DetalleConjunto
import com.example.conjuntoresidencial.model.PaginaEventos
import com.example.conjuntoresidencial.model.PanelPlataforma
import com.example.conjuntoresidencial.model.EventoPlataforma
import com.example.conjuntoresidencial.model.LoginPlataforma
import com.example.conjuntoresidencial.model.LoginResponse
import com.example.conjuntoresidencial.model.NuevoConjunto
import retrofit2.Response
import retrofit2.http.*

/** P7 bloque 3: /api/plataforma del backend (PlataformaController). */
interface ApiPlataforma {

    @POST("api/plataforma/login")
    suspend fun login(@Body datos: LoginPlataforma): Response<LoginResponse>

    @POST("api/plataforma/logout")
    suspend fun cerrarSesion(): Response<Void>

    @PUT("api/plataforma/contrasena")
    suspend fun cambiarContrasena(@Body datos: CambioPropiaContrasenaRequest): Response<LoginResponse>

    @GET("api/plataforma/departamentos")
    suspend fun departamentos(): Response<List<String>>

    @GET("api/plataforma/conjuntos")
    suspend fun conjuntos(): Response<List<ConjuntoPlataforma>>

    @POST("api/plataforma/conjuntos")
    suspend fun crear(@Body datos: NuevoConjunto): Response<AccesoPrincipal>

    @PUT("api/plataforma/conjuntos/{id}")
    suspend fun editar(@Path("id") id: Long, @Body datos: DatosConjunto): Response<ConjuntoPlataforma>

    @PUT("api/plataforma/conjuntos/{id}/estado")
    suspend fun cambiarEstado(@Path("id") id: Long, @Body cambio: CambioEstadoConjunto): Response<ConjuntoPlataforma>

    @GET("api/plataforma/conjuntos/{id}/administracion")
    suspend fun cuentas(@Path("id") id: Long): Response<List<CuentaConjunto>>

    @POST("api/plataforma/conjuntos/{id}/principal/restablecer")
    suspend fun restablecerPrincipal(@Path("id") id: Long): Response<AccesoPrincipal>

    @GET("api/plataforma/auditoria")
    suspend fun auditoria(@Query("conjunto") conjunto: Long?): Response<List<EventoPlataforma>>

    // P8 bloque 6
    @GET("api/plataforma/panel")
    suspend fun panel(): Response<PanelPlataforma>

    @GET("api/plataforma/conjuntos/{id}/detalle")
    suspend fun detalle(@Path("id") id: Long): Response<DetalleConjunto>

    /** desde y hasta: "2026-11-05" (incluidos). */
    @GET("api/plataforma/auditoria/buscar")
    suspend fun buscarAuditoria(
        @Query("conjunto") conjunto: Long?, @Query("accion") accion: String?, @Query("desde") desde: String?,
        @Query("hasta") hasta: String?, @Query("q") q: String?, @Query("pagina") pagina: Int
    ): Response<PaginaEventos>
}
