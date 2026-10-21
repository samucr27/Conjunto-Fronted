package com.example.conjuntoresidencial.api

import android.content.Context
import com.example.conjuntoresidencial.BuildConfig
import com.example.conjuntoresidencial.model.MensajeError
import com.example.conjuntoresidencial.util.SessionManager
import com.google.gson.Gson
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // debug: backend local (10.0.2.2); release: Render. Ver app/build.gradle.kts
    private val BASE_URL = BuildConfig.API_URL

    private lateinit var appContext: Context

    /** Se llama una vez desde ConjuntoApp.onCreate(), antes de cualquier petición. */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(TiempoEsperaInterceptor())
            .addInterceptor(AuthInterceptor(SessionManager(appContext)))
            .connectTimeout(15, TimeUnit.SECONDS)
            // 30 s: el plan gratuito de Render tarda en responder tras un arranque en frío
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    /** Lee el {"mensaje": "..."} del cuerpo de error del backend, o devuelve [porDefecto]. */
    fun mensajeDeError(response: Response<*>, porDefecto: String): String {
        return try {
            val cuerpo = response.errorBody()?.string()
            Gson().fromJson(cuerpo, MensajeError::class.java)?.mensaje ?: porDefecto
        } catch (_: Exception) {
            porDefecto
        }
    }
}
