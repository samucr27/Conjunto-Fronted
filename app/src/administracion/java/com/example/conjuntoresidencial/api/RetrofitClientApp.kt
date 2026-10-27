package com.example.conjuntoresidencial.api

private object ClienteApp {
    val api: ApiAdmin by lazy { RetrofitClient.retrofit.create(ApiAdmin::class.java) }
}

/** La API de esta app: lo común más lo propio del flavor. En src/main se usa [RetrofitClient.comun]. */
val RetrofitClient.instance: ApiAdmin get() = ClienteApp.api
