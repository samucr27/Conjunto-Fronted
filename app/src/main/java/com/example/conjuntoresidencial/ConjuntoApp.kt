package com.example.conjuntoresidencial

import android.app.Application
import com.example.conjuntoresidencial.api.RetrofitClient

class ConjuntoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitClient.init(this)
    }
}
