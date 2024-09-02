package com.tcs.tools.managePdf.ui.application

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.perf.FirebasePerformance

class Application:Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        FirebasePerformance.getInstance().isPerformanceCollectionEnabled = true
    }
}