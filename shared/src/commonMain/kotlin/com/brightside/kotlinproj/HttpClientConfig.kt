package com.brightside.kotlinproj

import io.ktor.client.*

expect fun createHttpClient(): HttpClient

fun HttpClientConfig<*>.defaultConfig() {
}