package com.zerostress.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.zerostress.manager.ui.theme.*

class SendNotificationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZeroStressTheme { SendNotificationScreen() } }
    }
}

@Composable
fun SendNotificationScreen() {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        FirebaseMessaging.getInstance().subscribeToTopic("all_players")
        FirebaseMessaging.getInstance().subscribeToTopic("match_updates")
        FirebaseMessaging.getInstance().subscribeToTopic("announcements")
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("🔔 Send Notification", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))

        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = ZSCard)) {
            Column(modifier = Modifier.padding(20.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Notification Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Notification Message") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (title.isNotBlank() && message.isNotBlank()) {
                            FirebaseFirestore.getInstance().collection("notifications").add(
                                mapOf(
                                    "title" to title.trim(),
                                    "message" to message.trim(),
                                    "author" to "Admin",
                                    "timestamp" to System.currentTimeMillis(),
                                    "topic" to "all_players"
                                )
                            ).addOnSuccessListener {
                                status = "✅ Notification sent!"
                                title = ""
                                message = ""
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Send to All Players") }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val quickAlerts = listOf(
                            "Maintenance" to "Server maintenance in 30 minutes.",
                            "Update" to "New update available! Check the changelog.",
                            "Event" to "Special event starting now!"
                        )
                        quickAlerts.random().let { (t, m) ->
                            title = t
                            message = m
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Quick Alert") }

                if (status.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(status, color = ZSPrimary)
                }
            }
        }
    }
}
