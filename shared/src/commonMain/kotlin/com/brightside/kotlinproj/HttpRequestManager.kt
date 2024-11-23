package com.brightside.kotlinproj

object HttpRequestManager {
    private lateinit var queueManager: RequestQueueManager

    /**
     * Initialize the HttpRequestManager with platform-specific dependencies.
     * Must be called once before using sendRequest.
     */
    fun initialize(networkMonitor: NetworkMonitor) {
        if (!::queueManager.isInitialized) {
            queueManager = RequestQueueManager(networkMonitor)
        }
    }

    fun addListener(listener: RequestQueueManagerListener) {
        queueManager.addListener(listener)
    }

    fun removeListener(listener: RequestQueueManagerListener) {
        queueManager.removeListener(listener)
    }

    fun processQueues() {
        queueManager.processQueues()
    }

    fun getHighPriorityQueueLength(): Int {
        if (!::queueManager.isInitialized) {
            throw IllegalStateException("HttpRequestManager is not initialized. Call initialize() first.")
        }
        return queueManager.getHighPriorityQueueLength()
    }

    fun getMediumPriorityQueueLength(): Int {
        if (!::queueManager.isInitialized) {
            throw IllegalStateException("HttpRequestManager is not initialized. Call initialize() first.")
        }
        return queueManager.getMediumPriorityQueueLength()
    }

    suspend fun sendRequest(request: HttpRequest, queueType: QueueType = QueueType.HIGH_PRIORITY) {
        if (!::queueManager.isInitialized) {
            throw IllegalStateException("HttpRequestManager is not initialized. Call initialize() first.")
        }
        queueManager.enqueueRequest(request, queueType)
    }
}