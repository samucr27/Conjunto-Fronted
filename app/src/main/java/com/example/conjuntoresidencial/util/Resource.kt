package com.example.conjuntoresidencial.util

sealed class Resource<out T> {
    object Loading : Resource<Nothing>()
    data class Success<out T>(val data: T?) : Resource<T>()
    data class Error(val message: String) : Resource<Nothing>()
}

/** Convierte los datos de un resultado correcto; carga y error pasan igual. */
fun <T, R> Resource<T>.map(transformar: (T) -> R): Resource<R> = when (this) {
    is Resource.Success -> Resource.Success(data?.let(transformar))
    is Resource.Error -> this
    Resource.Loading -> Resource.Loading
}
