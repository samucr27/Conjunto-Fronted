package com.example.conjuntoresidencial.api

import com.example.conjuntoresidencial.model.AbrirTurno
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
}
