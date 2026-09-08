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

class PerformanceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZeroStressTheme { PerformanceScreen() } }
    }
}

@Composable
fun PerformanceScreen() {
    val userId = FirebaseAuth.getInstance().uid
    var logs by remember { mutableStateOf(listOf<DocumentSnapshot>()) }
    var totalKills by remember { mutableLongStateOf(0L) }
    var totalDeaths by remember { mutableLongStateOf(0L) }
    var totalDamage by remember { mutableLongStateOf(0L) }

    LaunchedEffect(userId) {
        if (userId == null) return@LaunchedEffect
        FirebaseFirestore.getInstance().collection("match_logs").whereEqualTo("playerId", userId).orderBy("date").limit(50)
            .addSnapshotListener { s, _ ->
                if (s != null) {
                    logs = s.documents
                    totalKills = s.documents.sumOf { it.getLong("kills") ?: 0 }
                    totalDeaths = s.documents.sumOf { it.getLong("deaths") ?: 0 }
                    totalDamage = s.documents.sumOf { it.getLong("damage") ?: 0 }
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("📊 Performance", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(12.dp))

        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = ZSCard)) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column { Text("$totalKills", color = ZSPrimary, fontWeight = FontWeight.Bold); Text("Kills", color = ZSTextMuted, fontSize = 11.sp) }
                Column { Text("$totalDeaths", color = ZSDanger, fontWeight = FontWeight.Bold); Text("Deaths", color = ZSTextMuted, fontSize = 11.sp) }
                Column { Text("$totalDamage", color = ZSWarning, fontWeight = FontWeight.Bold); Text("Damage", color = ZSTextMuted, fontSize = 11.sp) }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(logs) { doc ->
                val kills = doc.getLong("kills") ?: 0
                val deaths = doc.getLong("deaths") ?: 0
                val dmg = doc.getLong("damage") ?: 0
                val type = doc.getString("type") ?: "Match"
                val ts = doc.getLong("date") ?: 0
                val dateStr = if (ts > 0) SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(ts)) else ""

                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = ZSCard)) {
                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text(type, color = Color.White); Text(dateStr, color = ZSTextMuted, fontSize = 11.sp) }
                        Text("K:$kills D:$deaths DMG:$dmg", color = ZSText, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
