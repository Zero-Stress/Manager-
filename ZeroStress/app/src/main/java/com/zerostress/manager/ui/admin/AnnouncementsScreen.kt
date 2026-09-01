package com.zerostress.manager.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerostress.manager.models.Announcement
import com.zerostress.manager.ui.theme.*
import com.zerostress.manager.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementsScreen(appViewModel: AppViewModel) {
    val announcements by appViewModel.announcements.collectAsState()
    var message by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(12.dp)) {
        Text("Broadcast Announcements", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Accent)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = message, onValueChange = { message = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Type match timing, updates, or rules...") },
            minLines = 3, maxLines = 5,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent)
        )
        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (message.isNotBlank()) {
                    appViewModel.postAnnouncement(Announcement(message = message.trim(), postedBy = "Admin"))
                    message = ""
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent)
        ) {
            Icon(Icons.Filled.Send, null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Publish Announcement", fontWeight = FontWeight.ExtraBold, color = DarkBg)
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("Active Broadcasts", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Accent)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn {
            items(announcements) { ann ->
                val dateStr = try {
                    SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(ann.timestamp))
                } catch (e: Exception) { "" }

                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(dateStr, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(ann.message, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { appViewModel.deleteAnnouncement(ann.id) }) {
                            Text("Delete", color = Danger, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
