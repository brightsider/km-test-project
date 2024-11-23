package com.brightside.kotlinproj

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

actual class NetworkMonitor actual constructor(context: Any) {
    private val applicationContext = context as Context

    private var networkChangeListener: ((Boolean) -> Unit)? = null

    private val connectivityManager =
        applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val networkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            networkChangeListener?.invoke(isNetworkAvailable())
        }
    }

    init {
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        applicationContext.registerReceiver(networkReceiver, filter)
    }

    @SuppressLint("MissingPermission")
    actual fun isNetworkAvailable(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

        return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    actual fun setNetworkChangeListener(listener: (Boolean) -> Unit) {
        networkChangeListener = listener
    }

    actual fun stopMonitoring() {
        applicationContext.unregisterReceiver(networkReceiver)
    }
}