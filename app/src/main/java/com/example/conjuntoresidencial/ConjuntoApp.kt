package com.example.conjuntoresidencial

import android.app.Application
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.push.AvisosCelular
import com.example.conjuntoresidencial.util.Tema

class ConjuntoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitClient.init(this)
        Tema.aplicar(this)
        AvisosCelular.crearCanal(this)
    }
}
