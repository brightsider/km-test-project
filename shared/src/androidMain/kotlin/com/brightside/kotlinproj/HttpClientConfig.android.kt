package com.brightside.kotlinproj

import io.ktor.client.*

actual fun createHttpClient(): HttpClient {
    return HttpClient() {
        defaultConfig()
    }
}