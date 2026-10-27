package com.example.conjuntoresidencial.api

private object ClienteApp {
    val api: ApiResidente by lazy { RetrofitClient.retrofit.create(ApiResidente::class.java) }
}

/** La API de esta app: lo común más lo propio del flavor. En src/main se usa [RetrofitClient.comun]. */
val RetrofitClient.instance: ApiResidente get() = ClienteApp.api
