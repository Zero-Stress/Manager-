package com.zerostress.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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

class ChatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZeroStressTheme { ChatScreen() } }
    }
}

@Composable
fun ChatScreen() {
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().uid
    var messages by remember { mutableStateOf(listOf<DocumentSnapshot>()) }
    var inputText by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("Player") }

    LaunchedEffect(Unit) {
        if (userId != null) {
            db.collection("players").document(userId).get()
                .addOnSuccessListener { if (it.exists()) userName = it.getString("name") ?: "Player" }
        }
        db.collection("chat_messages").orderBy("timestamp").limit(100).addSnapshotListener { snapshots, _ ->
            if (snapshots != null) messages = snapshots.documents
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("💬 Team Chat", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(messages) { doc ->
                val sender = doc.getString("senderName") ?: "Unknown"
                val text = doc.getString("text") ?: ""
                val ts = doc.getLong("timestamp") ?: 0
                val isMe = doc.getString("senderId") == userId
                val timeStr = if (ts > 0) SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts)) else ""

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isMe) ZSPrimary.copy(alpha = 0.2f) else ZSCard)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(sender, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ZSPrimary)
                            Text(timeStr, fontSize = 10.sp, color = ZSTextMuted)
                        }
                        Text(text, color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type message...") },
                singleLine = true
            )
            IconButton(onClick = {
                if (inputText.isNotBlank() && userId != null) {
                    db.collection("chat_messages").add(
                        mapOf(
                            "text" to inputText.trim(),
                            "senderId" to userId,
                            "senderName" to userName,
                            "timestamp" to System.currentTimeMillis()
                        )
                    )
                    inputText = ""
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = ZSPrimary)
            }
        }
    }
}
