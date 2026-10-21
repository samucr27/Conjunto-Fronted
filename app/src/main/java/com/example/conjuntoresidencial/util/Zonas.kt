package com.example.conjuntoresidencial.util

import com.example.conjuntoresidencial.model.ZonaComunDTO
import retrofit2.Response

/** zonaId -> nombre, para mostrar el nombre de la zona en las listas de reservas. Vacío si la petición falló. */
fun nombresDeZonas(response: Response<List<ZonaComunDTO>>): Map<Long, String> =
    if (response.isSuccessful) {
        response.body().orEmpty().mapNotNull { z -> z.id?.let { it to z.nombre } }.toMap()
    } else emptyMap()
