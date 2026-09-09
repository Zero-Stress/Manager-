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
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.zerostress.manager.ui.theme.*

@Composable
fun ManageSeasonsScreen() {
    val db = FirebaseFirestore.getInstance()
    var seasons by remember { mutableStateOf(listOf<DocumentSnapshot>()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var seasonName by remember { mutableStateOf("") }
    var seasonDesc by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        db.collection("seasons").addSnapshotListener { s, _ ->
            if (s != null) seasons = s.documents.sortedByDescending { it.getLong("createdAt") ?: 0 }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("➕ New Season") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = seasonName, onValueChange = { seasonName = it }, label = { Text("Season Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = seasonDesc, onValueChange = { seasonDesc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (seasonName.isNotBlank()) {
                        db.collection("seasons").add(
                            mapOf(
                                "name" to seasonName.trim(),
                                "description" to seasonDesc.trim(),
                                "isActive" to true,
                                "createdAt" to System.currentTimeMillis()
                            )
                        )
                        seasonName = ""
                        seasonDesc = ""
                        showAddDialog = false
                    }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("📅 Manage Seasons", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Button(onClick = { showAddDialog = true }) { Text("+ Add") }
        }
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(seasons) { doc ->
                val name = doc.getString("name") ?: "Season"
                val isActive = doc.getBoolean("isActive") ?: false
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = ZSCard)) {
                    Row(modifier = Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(doc.getString("description") ?: "", color = ZSTextMuted, fontSize = 12.sp)
                        }
                        Row {
                            TextButton(onClick = {
                                // Deactivate all, activate this one
                                seasons.filter { it.id != doc.id }.forEach { s -> s.reference.update("isActive", false) }
                                doc.reference.update("isActive", true)
                            }) { Text(if (isActive) "🟢 Active" else "Activate", color = ZSPrimary, fontSize = 11.sp) }
                            TextButton(onClick = { doc.reference.delete() }) { Text("🗑", color = ZSDanger) }
                        }
                    }
                }
            }
        }
    }
}
