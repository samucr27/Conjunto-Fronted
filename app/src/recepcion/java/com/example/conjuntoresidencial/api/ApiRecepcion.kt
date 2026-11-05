package com.example.conjuntoresidencial.api

import com.example.conjuntoresidencial.model.AbrirTurno
import com.example.conjuntoresidencial.model.AnotacionMinuta
import com.example.conjuntoresidencial.model.ApartamentoDirectorio
import com.example.conjuntoresidencial.model.NuevaAnotacion
import com.example.conjuntoresidencial.model.PaginaVisitas
import com.example.conjuntoresidencial.model.ResumenPorteria
import com.example.conjuntoresidencial.model.VisitaDTO
import retrofit2.http.Query
import com.example.conjuntoresidencial.model.GuardiaEnTurno
import com.example.conjuntoresidencial.model.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** P7 bloque 4: el turno de cada guardia en la recepción (GuardiaController del backend). */
interface ApiRecepcion : ApiComun {

    @GET("api/porteria/turno/guardias")
    suspend fun guardiasDelTurno(): Response<List<GuardiaEnTurno>>

    /** 400 si el PIN no coincide; 429 tras varios intentos. */
    @POST("api/porteria/turno")
    suspend fun abrirTurno(@Body pedido: AbrirTurno): Response<LoginResponse>

    // P8 bloque 5: el tablero de la portería
    @GET("api/porteria/resumen")
    suspend fun resumen(): Response<ResumenPorteria>

    @GET("api/porteria/visitas/dentro")
    suspend fun visitasDentro(): Response<List<VisitaDTO>>

    @GET("api/porteria/visitas/historial")
    suspend fun historial(@Query("q") q: String?, @Query("dias") dias: Int, @Query("pagina") pagina: Int): Response<PaginaVisitas>

    @GET("api/porteria/directorio")
    suspend fun directorio(@Query("q") q: String?): Response<List<ApartamentoDirectorio>>

    @GET("api/porteria/minuta")
    suspend fun minuta(@Query("dias") dias: Int): Response<List<AnotacionMinuta>>

    @POST("api/porteria/minuta")
    suspend fun anotar(@Body anotacion: NuevaAnotacion): Response<AnotacionMinuta>
}
