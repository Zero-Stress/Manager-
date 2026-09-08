package com.zerostress.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

class FriendsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZeroStressTheme { FriendsScreen() } }
    }
}

@Composable
fun FriendsScreen() {
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().uid
    var friends by remember { mutableStateOf(listOf<DocumentSnapshot>()) }
    var pendingRequests by remember { mutableStateOf(listOf<DocumentSnapshot>()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var targetUid by remember { mutableStateOf("") }

    LaunchedEffect(userId) {
        if (userId == null) return@LaunchedEffect
        db.collection("friend_requests").whereEqualTo("from", userId).whereEqualTo("status", "accepted").addSnapshotListener { s, _ ->
            if (s != null) friends = s.documents
        }
        db.collection("friend_requests").whereEqualTo("to", userId).whereEqualTo("status", "pending").addSnapshotListener { s, _ ->
            if (s != null) pendingRequests = s.documents
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Friend") },
            text = {
                OutlinedTextField(value = targetUid, onValueChange = { targetUid = it }, label = { Text("Friend's UID") }, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(onClick = {
                    if (targetUid.isNotBlank() && userId != null) {
                        db.collection("friend_requests").add(
                            mapOf("from" to userId, "to" to targetUid.trim(), "status" to "pending", "timestamp" to System.currentTimeMillis())
                        )
                        targetUid = ""
                        showAddDialog = false
                    }
                }) { Text("Send Request") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("👥 Friends", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Button(onClick = { showAddDialog = true }) { Text("+ Add") }
        }

        if (pendingRequests.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("Pending Requests (${pendingRequests.size})", color = ZSWarning, fontWeight = FontWeight.Bold)
            pendingRequests.forEach { doc ->
                val from = doc.getString("from") ?: ""
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = ZSCard)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Request from: ${doc.getString("fromName") ?: from}", color = Color.White, modifier = Modifier.weight(1f))
                        Row {
                            TextButton(onClick = {
                                doc.reference.update("status", "accepted")
                            }) { Text("Accept", color = ZSPrimary) }
                            TextButton(onClick = { doc.reference.update("status", "rejected") }) { Text("Reject", color = ZSDanger) }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Friends (${friends.size})", color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(friends) { doc ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = ZSCard)) {
                    Text(doc.getString("name") ?: "Friend", modifier = Modifier.padding(14.dp), color = Color.White)
                }
            }
        }
    }
}
