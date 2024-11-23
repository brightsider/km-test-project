package com.brightside.kotlinproj

import io.ktor.client.request.*
import io.ktor.http.isSuccess
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.seconds

interface RequestQueueManagerListener {
    fun onError(message: String)
    fun onQueueCountChanged()
}

class RequestQueueManager(private val networkMonitor: NetworkMonitor) {
    private val client = createHttpClient()

    private val listeners = mutableListOf<RequestQueueManagerListener>()

    private val highPriorityQueue = ArrayDeque<HttpRequest>()
    private val mediumPriorityQueue = ArrayDeque<HttpRequest>()

    private val queueMutex = Mutex()
    private var isProcessing = false

    fun getHighPriorityQueueLength(): Int {
        return highPriorityQueue.size
    }

    fun getMediumPriorityQueueLength(): Int {
        return mediumPriorityQueue.size
    }

    init {
        val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        networkMonitor.setNetworkChangeListener { isAvailable ->
            if (isAvailable) {
                coroutineScope.launch {
                    processQueues()
                }
            }
        }
    }

    fun addListener(listener: RequestQueueManagerListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: RequestQueueManagerListener) {
        listeners.remove(listener)
    }

    suspend fun enqueueRequest(request: HttpRequest, queueType: QueueType = QueueType.HIGH_PRIORITY) {
        addRequest(request, queueType)
    }

    private suspend fun addRequest(request: HttpRequest, queueType: QueueType = QueueType.HIGH_PRIORITY, isLast: Boolean = true) {
        queueMutex.withLock {
            when (queueType) {
                QueueType.HIGH_PRIORITY -> highPriorityQueue.run { if (isLast) addLast(request) else addFirst(request) }
                QueueType.MEDIUM_PRIORITY -> mediumPriorityQueue.run { if (isLast) addLast(request) else addFirst(request) }
            }
        }
        listeners.forEach { it.onQueueCountChanged() }
        processQueues()
    }

    fun processQueues() {
        if (isProcessing || !networkMonitor.isNetworkAvailable()) return

        isProcessing = true

        println("process queue")

        CoroutineScope(Dispatchers.Default).launch {
                while (networkMonitor.isNetworkAvailable() && (highPriorityQueue.isNotEmpty() || mediumPriorityQueue.isNotEmpty())) {
                    val request = queueMutex.withLock {
                        when {
                            highPriorityQueue.isNotEmpty() -> QueueType.HIGH_PRIORITY to highPriorityQueue.removeFirst()
                            mediumPriorityQueue.isNotEmpty() -> QueueType.MEDIUM_PRIORITY to mediumPriorityQueue.removeFirst()
                            else -> null
                        }
                    }

                    listeners.forEach { it.onQueueCountChanged() }

                    request?.let {
                        handleRequestWithRetry(request.second, request.first)
                    }
                }
                isProcessing = false
        }
    }

    private suspend fun handleRequestWithRetry(request: HttpRequest, queueType: QueueType) {
        var attempt = 0
        val maxAttempts = 3
        var backoffTime = 1.seconds

        while (attempt < maxAttempts) {
            val isLastAttmept = attempt == maxAttempts - 1
            try {
                println("request started - ${request.url} ($attempt)")
                val response = sendHttpRequest(request)
                if (response.isSuccessful) {
                    println("request finished - ${request.url}")
                    break
                } else {
                    if (response.errorMessage.contains("hostname could not be found")) {
                        throw ServerUnreachableException("DNS Not Found")
                    }
                    if (isLastAttmept) {
                        println("request finished - ${request.url}, ${response.errorMessage}")
                        notifyServerError(response.errorMessage)
                    }
                }
            } catch (e: NetworkUnavailableException) {
                addRequest(request, queueType, false)
                println("request failed - ${request.url}, ${e.message}")
                break
            } catch (e: ServerUnreachableException) {
                println("request failed - ${request.url}, ${e.message}")
                notifyServerUnreachable(e.message ?: "Server Unreachable")
                break
            }
            attempt++
            if (!isLastAttmept) {
                delay(backoffTime)
                backoffTime *= 2
            }
        }
    }

    private suspend fun sendHttpRequest(request: HttpRequest): HttpResponse {
        if (!networkMonitor.isNetworkAvailable()) throw NetworkUnavailableException()

        delay(1500) // for easier testing counters

        return try {
            val response = client.request(request.url) {
                method = io.ktor.http.HttpMethod.parse(request.method.toString())
                headers {
                    request.headers.forEach { (key, value) ->
                        append(key, value)
                    }
                }
                request.body?.let { setBody(it) }
            }
            HttpResponse(isSuccessful = response.status.isSuccess())
        } catch (e: UnresolvedAddressException) {
            throw ServerUnreachableException("DNS Not Found")
        } catch (e: Exception) {
            HttpResponse(isSuccessful = false, errorMessage = e.message ?: "Unknown Error")
        }
    }

    private fun notifyServerError(message: String) {
        listeners.forEach { it.onError("Server Error: $message") }
    }

    private fun notifyServerUnreachable(message: String) {
        listeners.forEach { it.onError("Server Unreachable: $message") }
    }
}