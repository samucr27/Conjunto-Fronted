package com.example.conjuntoresidencial.api

private object ClienteApp {
    val api: ApiRecepcion by lazy { RetrofitClient.retrofit.create(ApiRecepcion::class.java) }
}

/** La API de esta app: lo común más el turno. En src/main se usa [RetrofitClient.comun]. */
val RetrofitClient.instance: ApiRecepcion get() = ClienteApp.api
