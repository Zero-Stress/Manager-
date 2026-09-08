package com.zerostress.manager

import android.content.Intent
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.zerostress.manager.ui.theme.*

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZeroStressTheme { SettingsScreen() } }
    }
}

@Composable
fun SettingsScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var notifications by remember { mutableStateOf(true) }
    var chatNotifs by remember { mutableStateOf(true) }
    var scheduleNotifs by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("⚠️ Delete Account") },
            text = { Text("This will permanently delete your account and all data. This action CANNOT be undone!") },
            confirmButton = {
                TextButton(onClick = {
                    val uid = FirebaseAuth.getInstance().uid
                    if (uid != null) {
                        FirebaseFirestore.getInstance().collection("players").document(uid).delete()
                        FirebaseAuth.getInstance().currentUser?.delete()
                        FirebaseAuth.getInstance().signOut()
                        context.startActivity(Intent(context, LoginActivity::class.java))
                        (context as? ComponentActivity)?.finish()
                    }
                    showDeleteDialog = false
                }) { Text("Delete Everything", color = Color.Red) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("⚙ Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))

        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = ZSCard)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Notifications", color = Color.White); Switch(checked = notifications, onCheckedChange = { notifications = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Chat Notifications", color = Color.White); Switch(checked = chatNotifs, onCheckedChange = { chatNotifs = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Schedule Notifications", color = Color.White); Switch(checked = scheduleNotifs, onCheckedChange = { scheduleNotifs = it })
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = ZSCard)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Button(onClick = { /* clear cache */ }, modifier = Modifier.fillMaxWidth()) { Text("Clear Cache") }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = {
                    androidx.appcompat.app.AlertDialog.Builder(context)
                        .setTitle("ZERO STRESS")
                        .setMessage("Version 4.0\n\nPerformance & Leaderboard Manager\n\nBuilt with Firebase + Kotlin + Jetpack Compose")
                        .setPositiveButton("OK", null)
                        .show()
                }, modifier = Modifier.fillMaxWidth()) { Text("About") }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                FirebaseAuth.getInstance().signOut()
                context.startActivity(Intent(context, LoginActivity::class.java))
                (context as? ComponentActivity)?.finish()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = ZSWarning)
        ) { Text("Logout", color = Color.Black) }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { showDeleteDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = ZSDanger)
        ) { Text("Delete Account") }
    }
}
