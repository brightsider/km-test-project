package com.brightside.kotlinproj

enum class QueueType {
    HIGH_PRIORITY,
    MEDIUM_PRIORITY
}

enum class HttpMethod {
    GET, POST, PUT, DELETE
}

data class HttpRequest(
    val url: String,
    val method: HttpMethod = HttpMethod.GET,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null
)

data class HttpResponse(
    val isSuccessful: Boolean,
    val errorMessage: String = ""
)

class NetworkUnavailableException : Exception("Network Unavailable")
class ServerUnreachableException(message: String) : Exception(message)