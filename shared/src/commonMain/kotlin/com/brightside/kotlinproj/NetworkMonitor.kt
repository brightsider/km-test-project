package com.brightside.kotlinproj

expect class NetworkMonitor(context: Any) {
    fun isNetworkAvailable(): Boolean
    fun setNetworkChangeListener(listener: (Boolean) -> Unit)
    fun stopMonitoring()
}