package com.brightside.kotlinproj

import io.ktor.client.*
import io.ktor.client.engine.darwin.*
import platform.Foundation.*

actual fun createHttpClient(): HttpClient {
    val configuration = NSURLSessionConfiguration.backgroundSessionConfiguration("com.brightside.network")
    configuration.allowsCellularAccess = true
    configuration.timeoutIntervalForRequest = 15.0
    configuration.allowsExpensiveNetworkAccess = true

    return HttpClient(Darwin) {
        defaultConfig()
        engine {
            usePreconfiguredSession(NSURLSession.sessionWithConfiguration(configuration))
        }
    }
}