package com.brightside.kotlinproj

import platform.Network.*
import platform.darwin.dispatch_get_main_queue

actual class NetworkMonitor actual constructor(context: Any) {

    private val monitor = nw_path_monitor_create()
    private var isAvailable: Boolean = true
    private var networkChangeListener: ((Boolean) -> Unit)? = null

    init {
        nw_path_monitor_set_update_handler(monitor) { path ->
            isAvailable = nw_path_get_status(path) == nw_path_status_satisfied
            networkChangeListener?.invoke(isAvailable)
        }
        nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
        nw_path_monitor_start(monitor)
    }

    actual fun isNetworkAvailable(): Boolean {
        return isAvailable
    }

    actual fun setNetworkChangeListener(listener: (Boolean) -> Unit) {
        networkChangeListener = listener
    }

    actual fun stopMonitoring() {
        nw_path_monitor_cancel(monitor)
    }
}