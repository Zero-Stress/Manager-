package com.zerostress.manager.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerostress.manager.ui.theme.*
import com.zerostress.manager.viewmodel.AppViewModel

@Composable
fun PlayerComparisonScreen(myPhone: String, myName: String, appViewModel: AppViewModel) {
    val users by appViewModel.users.collectAsState()
    val dailyLogs by appViewModel.dailyLogs.collectAsState()
    var selectedOpponent by remember { mutableStateOf("") }

    val myLogs = dailyLogs.filter { it.phone == myPhone }
    val oppLogs = dailyLogs.filter { it.phone == selectedOpponent }

    val myStats = mapStats(myLogs)
    val oppStats = if (selectedOpponent.isNotEmpty()) mapStats(oppLogs) else null
    val oppName = users.find { it.phone == selectedOpponent }?.name ?: ""

    Column(modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("Player Comparison", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Accent)
        Spacer(modifier = Modifier.height(12.dp))

        // Opponent selector
        var expanded by remember { mutableStateOf(false) }
        Text("Compare with:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(6.dp))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = oppName.ifEmpty { "Select a player" },
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent)
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                users.filter { it.phone != myPhone && it.status == "confirmed" }.forEach { user ->
                    DropdownMenuItem(text = { Text(user.name) }, onClick = {
                        selectedOpponent = user.phone; expanded = false
                    })
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Comparison cards
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Player 1 header
            Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Accent.copy(alpha = 0.1f))) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(modifier = Modifier.size(48.dp), shape = RoundedCornerShape(24.dp), color = Accent) {
                        Box(contentAlignment = Alignment.Center) { Text(myName.take(1).uppercase(), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = DarkBg) }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(myName, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
                }
            }
            // VS
            Box(modifier = Modifier.align(Alignment.CenterVertically)) {
                Text("VS", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Warning)
            }
            // Player 2 header
            Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Danger.copy(alpha = 0.1f))) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(modifier = Modifier.size(48.dp), shape = RoundedCornerShape(24.dp), color = Danger) {
                        Box(contentAlignment = Alignment.Center) { Text(oppName.take(1).ifEmpty { "?" }.uppercase(), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = androidx.compose.ui.graphics.Color.White) }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(oppName.ifEmpty { "Select" }, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Stats comparison bars
        if (oppStats != null) {
            val comparisons = listOf(
                "Total Score" to Pair(myStats.score, oppStats.score),
                "Kills" to Pair(myStats.kills.toDouble(), oppStats.kills.toDouble()),
                "Wins" to Pair(myStats.wins.toDouble(), oppStats.wins.toDouble()),
                "Matches" to Pair(myStats.matches.toDouble(), oppStats.matches.toDouble()),
                "Damage" to Pair(myStats.damage.toDouble(), oppStats.damage.toDouble()),
                "Win Rate %" to Pair(myStats.winRate, oppStats.winRate),
                "Avg Kills/Match" to Pair(myStats.avgKills, oppStats.avgKills),
                "Avg Damage/Match" to Pair(myStats.avgDamage, oppStats.avgDamage)
            )

            comparisons.forEach { (label, values) ->
                val (myVal, oppVal) = values
                val total = myVal + oppVal
                val myPct = if (total > 0) (myVal / total).toFloat() else 0.5f

                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(formatVal(myVal), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (myVal >= oppVal) Success else MaterialTheme.colorScheme.onSurface)
                            Text(formatVal(oppVal), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (oppVal > myVal) Success else MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        // Progress bar
                        Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(MaterialTheme.colorScheme.outline, RoundedCornerShape(3.dp))) {
                            Box(modifier = Modifier.fillMaxWidth(myPct).fillMaxHeight().background(Accent, RoundedCornerShape(3.dp)))
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                Text("Select a player above to compare stats", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private data class Stats(
    val matches: Int, val wins: Int, val kills: Int, val damage: Int,
    val score: Double, val winRate: Double, val avgKills: Double, val avgDamage: Double
)

private fun mapStats(logs: List<com.zerostress.manager.models.DailyLog>): Stats {
    val m = logs.sumOf { it.matches }; val w = logs.sumOf { it.wins }; val k = logs.sumOf { it.kills }; val d = logs.sumOf { it.damage }
    val score = (k * 10.0) + (d / 100.0) + (w * 50.0)
    return Stats(m, w, k, d, score, if (m > 0) (w.toDouble() / m) * 100 else 0.0, if (m > 0) k.toDouble() / m else 0.0, if (m > 0) d.toDouble() / m else 0.0)
}

private fun formatVal(v: Double): String = if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.1f", v)
