package com.tcs.tools.managePdf.ui.application

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace

class Application:Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        FirebasePerformance.getInstance().isPerformanceCollectionEnabled = true
    }
}