package com.brightside.kotlinproj

import android.app.Application

class KMApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val networkMonitor = NetworkMonitor(this)
        HttpRequestManager.initialize(networkMonitor)
    }
}