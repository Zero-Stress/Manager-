package com.zerostress.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
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

class VoiceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZeroStressTheme { VoiceScreen() } }
    }
}

@Composable
fun VoiceScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().uid
    var channels by remember { mutableStateOf(listOf<DocumentSnapshot>()) }
    var usersInChannel by remember { mutableStateOf(listOf<DocumentSnapshot>()) }
    var selectedChannel by remember { mutableStateOf<String?>(null) }
    var channelName by remember { mutableStateOf("General") }

    LaunchedEffect(Unit) {
        db.collection("voice_channels").addSnapshotListener { snapshots, _ ->
            if (snapshots != null) channels = snapshots.documents
        }
    }

    LaunchedEffect(selectedChannel) {
        if (selectedChannel != null) {
            db.collection("voice_users").whereEqualTo("channelId", selectedChannel).addSnapshotListener { snapshots, _ ->
                if (snapshots != null) usersInChannel = snapshots.documents
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("🎙 Voice Channel", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(12.dp))

        if (selectedChannel == null) {
            Text("Channels", color = ZSTextMuted, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(channels) { doc ->
                    val name = doc.getString("name") ?: "General"
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            selectedChannel = doc.id
                            channelName = name
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = ZSCard)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔊 $name", color = Color.White, fontSize = 16.sp)
                            Text(doc.getString("status") ?: "Active", color = ZSTextMuted, fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("🔊 $channelName", color = Color.White, fontWeight = FontWeight.Bold)
                TextButton(onClick = { selectedChannel = null }) { Text("← Back") }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (userId != null) {
                Button(
                    onClick = {
                        db.collection("voice_users").document(userId).set(
                            mapOf(
                                "userId" to userId,
                                "channelId" to selectedChannel,
                                "userName" to (FirebaseAuth.getInstance().currentUser?.displayName ?: "Player"),
                                "joinedAt" to System.currentTimeMillis(),
                                "status" to "ONLINE"
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("🎤 Join Channel") }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Users in channel (${usersInChannel.size})", color = ZSTextMuted, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(usersInChannel) { user ->
                    val name = user.getString("userName") ?: "Unknown"
                    val status = user.getString("status") ?: "ONLINE"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = ZSCard)
                    ) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("🎤 $name", color = Color.White)
                            Text(status, color = if (status == "ONLINE") ZSPrimary else ZSTextMuted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
