package com.brightside.kotlinproj

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity(), RequestQueueManagerListener {
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var errorMessage by mutableStateOf<String?>(null)
    private var highPriorityQueueLength by mutableIntStateOf(0)
    private var mediumPriorityQueueLength by mutableIntStateOf(0)

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        HttpRequestManager.addListener(this)

        startQueueProcessingService()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            App()
        }
    }

    private fun sendRequest(url: String, queueType: QueueType) {
        coroutineScope.launch {
            val request = HttpRequest(url = url)
            HttpRequestManager.sendRequest(request, queueType)
        }
    }

    private fun startQueueProcessingService() {
        val serviceIntent = Intent(this, QueueProcessingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onError(message: String) {
        errorMessage = message

        val channelId = "error_notifications"
        val notificationId = 1

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Error Notifications"
            val descriptionText = "Displays error notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            notify(notificationId, builder.build())
        }
    }

    override fun onQueueCountChanged() {
        highPriorityQueueLength = HttpRequestManager.getHighPriorityQueueLength()
        mediumPriorityQueueLength = HttpRequestManager.getMediumPriorityQueueLength()
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
        HttpRequestManager.removeListener(this)
    }

    @Composable
    fun App() {
        MaterialTheme {
            Scaffold(
                topBar = {
                    TopAppBar(title = { Text("Queue Manager") })
                },
                content = { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        MainContent(
                            highPriorityQueueLength,
                            mediumPriorityQueueLength,
                            ::sendRequest
                        )
                    }
                }
            )
        }
    }

    @Composable
    fun MainContent(
        highPriorityQueueLength: Int,
        mediumPriorityQueueLength: Int,
        onSendRequest: (String, QueueType) -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { onSendRequest("https://jsonplaceholder.typicode.com/todos/", QueueType.HIGH_PRIORITY) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Send High Priority Request")
            }

            Button(
                onClick = { onSendRequest("https://jsonplaceholder.typicode.com/todos/", QueueType.MEDIUM_PRIORITY) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Send Medium Priority Request")
            }

            Button(
                onClick = { onSendRequest("https://jsonplaceholder.typicode.com1/todos/", QueueType.HIGH_PRIORITY) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Send High Priority Request with DNS error")
            }

            Button(
                onClick = { onSendRequest("https://jsonplaceholder.typicode.com/error", QueueType.HIGH_PRIORITY) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Send High Priority Request with error")
            }

            Text("High Priority Queue Length: $highPriorityQueueLength")
            Text("Medium Priority Queue Length: $mediumPriorityQueueLength")

            errorMessage?.let {
                AlertDialog(
                    onDismissRequest = { errorMessage = null },
                    confirmButton = {
                        TextButton(
                            onClick = { errorMessage = null }
                        ) {
                            Text("OK")
                        }
                    },
                    title = {
                        Text(text = "Error")
                    },
                    text = {
                        Text(text = it)
                    }
                )
            }
        }
    }
}
