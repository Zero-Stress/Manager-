package com.zerostress.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.zerostress.manager.ui.theme.*

class PerformanceGraphsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZeroStressTheme { PerformanceGraphsScreen() } }
    }
}

@Composable
fun PerformanceGraphsScreen() {
    val userId = FirebaseAuth.getInstance().uid
    var kills by remember { mutableStateOf(0L) }
    var deaths by remember { mutableStateOf(0L) }
    var wins by remember { mutableStateOf(0L) }
    var matches by remember { mutableStateOf(0L) }
    var damage by remember { mutableStateOf(0L) }
    var xp by remember { mutableStateOf(0L) }
    var level by remember { mutableStateOf(1L) }

    LaunchedEffect(userId) {
        if (userId == null) return@LaunchedEffect
        FirebaseFirestore.getInstance().collection("players").document(userId).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                kills = doc.getLong("kills") ?: 0
                deaths = doc.getLong("deaths") ?: 0
                wins = doc.getLong("wins") ?: 0
                matches = doc.getLong("matches") ?: 0
                damage = doc.getLong("damage") ?: 0
                xp = doc.getLong("xp") ?: 0
                level = doc.getLong("level") ?: 1
            }
        }
    }

    val winRate = if (matches > 0) wins * 100f / matches else 0f
    val kdRatio = if (deaths > 0) kills.toFloat() / deaths else kills.toFloat()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("📈 Performance Overview", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))

        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = ZSCard)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Stats Summary", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    PerfStatItem("Level", "$level")
                    PerfStatItem("XP", "$xp")
                    PerfStatItem("Win Rate", "${winRate.toInt()}%")
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    PerfStatItem("K/D Ratio", "%.2f".format(kdRatio))
                    PerfStatItem("Total Kills", "$kills")
                    PerfStatItem("Total Deaths", "$deaths")
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    PerfStatItem("Total Damage", "$damage")
                    PerfStatItem("Matches", "$matches")
                    PerfStatItem("Wins", "$wins")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Visual bar representations
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = ZSCard)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Performance Bars", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                val maxStat = maxOf(kills, deaths, damage, 1L)
                PerfBar("Kills", kills, maxStat, ZSPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                PerfBar("Deaths", deaths, maxStat, ZSDanger)
                Spacer(modifier = Modifier.height(8.dp))
                PerfBar("Damage", damage, maxStat, ZSWarning)
                Spacer(modifier = Modifier.height(8.dp))
                PerfBar("Wins", wins, maxOf(matches, 1), ZSPrimary)
            }
        }
    }
}

@Composable
private fun PerfStatItem(label: String, value: String) {
    Column { Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ZSPrimary); Text(label, fontSize = 11.sp, color = ZSTextMuted) }
}

@Composable
private fun PerfBar(label: String, value: Long, max: Long, color: Color) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = ZSText, fontSize = 13.sp)
            Text("$value", color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { if (max > 0) (value.toFloat() / max).coerceIn(0f, 1f) else 0f },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
    }
}
