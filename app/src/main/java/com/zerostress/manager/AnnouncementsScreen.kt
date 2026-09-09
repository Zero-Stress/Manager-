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
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AnnouncementsScreen() {
    var announcements by remember { mutableStateOf(listOf<DocumentSnapshot>()) }
    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("announcements").orderBy("timestamp").limit(50).addSnapshotListener { s, _ ->
            if (s != null) announcements = s.documents
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("📢 Announcements", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(12.dp))

        if (announcements.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No announcements yet", color = ZSTextMuted) }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(announcements) { doc ->
                    val text = doc.getString("text") ?: ""
                    val author = doc.getString("author") ?: "Admin"
                    val ts = doc.getLong("timestamp") ?: 0
                    val time = if (ts > 0) SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(ts)) else ""

                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = ZSCard)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(text, color = Color.White, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("$author • $time", color = ZSTextMuted, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
