package com.zerostress.manager

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.zerostress.manager.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotificationsScreen() {
    val userId = FirebaseAuth.getInstance().uid
    var notifications by remember { mutableStateOf(listOf<DocumentSnapshot>()) }

    LaunchedEffect(userId) {
        if (userId == null) return@LaunchedEffect
        FirebaseFirestore.getInstance().collection("notifications")
            .orderBy("timestamp")
            .addSnapshotListener { s, _ ->
                if (s != null) notifications = s.documents
            }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("🔔 Notifications", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(12.dp))

        if (notifications.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No notifications", color = ZSTextMuted)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(notifications) { doc ->
                    val title = doc.getString("title") ?: "Notification"
                    val msg = doc.getString("message") ?: doc.getString("text") ?: ""
                    val author = doc.getString("author") ?: "System"
                    val ts = doc.getLong("timestamp") ?: 0
                    val time = if (ts > 0) SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(ts)) else ""

                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = ZSCard)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(time, color = ZSTextMuted, fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(msg, color = ZSText, fontSize = 13.sp)
                            Text("From: $author", color = ZSTextMuted, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
