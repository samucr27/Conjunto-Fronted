package com.example.conjuntoresidencial.api

private object ClienteApp {
    val api: ApiPlataforma by lazy { RetrofitClient.retrofit.create(ApiPlataforma::class.java) }
}

/** La API de esta app. En src/main se usa [RetrofitClient.comun]. */
val RetrofitClient.instance: ApiPlataforma get() = ClienteApp.api
