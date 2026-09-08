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
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.zerostress.manager.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

class ScheduleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZeroStressTheme { ScheduleScreen() } }
    }
}

@Composable
fun ScheduleScreen() {
    var schedules by remember { mutableStateOf(listOf<DocumentSnapshot>()) }

    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("match_schedules").addSnapshotListener { s, _ ->
            if (s != null) schedules = s.documents
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("📅 Match Schedule", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(12.dp))

        if (schedules.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No scheduled matches", color = ZSTextMuted)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(schedules) { doc ->
                    val title = doc.getString("title") ?: "Match"
                    val dateTime = doc.getString("dateTime") ?: "TBD"
                    val type = doc.getString("type") ?: "Custom"
                    val status = doc.getString("status") ?: "Upcoming"

                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = ZSCard)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(status, color = ZSPrimary, fontSize = 12.sp)
                            }
                            Text("🕐 $dateTime", color = ZSTextMuted, fontSize = 13.sp)
                            Text("Type: $type", color = ZSTextMuted, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
