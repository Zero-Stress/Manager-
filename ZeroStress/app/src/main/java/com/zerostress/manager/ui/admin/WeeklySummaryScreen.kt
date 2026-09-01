package com.zerostress.manager.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerostress.manager.ui.theme.*
import com.zerostress.manager.viewmodel.AppViewModel

@Composable
fun WeeklySummaryScreen(appViewModel: AppViewModel) {
    val dailyLogs by appViewModel.dailyLogs.collectAsState()

    val weeklyStats = dailyLogs.groupBy { it.playerName }.map { (name, logs) ->
        val m = logs.sumOf { it.matches }; val w = logs.sumOf { it.wins }; val k = logs.sumOf { it.kills }
        val a = logs.sumOf { it.assists }; val d = logs.sumOf { it.damage }
        val surv = logs.sumOf { it.survivalMinutes * 60 + it.survivalSeconds }
        Triple(name, Triple(m, w, k), Triple(a, d, surv))
    }

    Column(modifier = Modifier.padding(12.dp)) {
        Text("Weekly Performance Summary", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Accent)
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn {
            itemsIndexed(weeklyStats) { _, (name, mwkd, ads) ->
                val (m, w, k) = mwkd; val (a, d, surv) = ads
                val wr = if (m > 0) (w.toDouble() / m) * 100 else 0.0
                val avgK = if (m > 0) k.toDouble() / m else 0.0
                val avgD = if (m > 0) d.toDouble() / m else 0.0
                val avgSurv = if (m > 0) surv / m else 0

                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Accent)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatCol("Matches", "$m")
                            StatCol("Wins", "$w")
                            StatCol("Kills", "$k")
                            StatCol("Assists", "$a")
                            StatCol("Damage", "$d")
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatCol("Win Rate", "${String.format("%.1f", wr)}%")
                            StatCol("Avg Kills", "${String.format("%.1f", avgK)}")
                            StatCol("Avg DMG", "${String.format("%.0f", avgD)}")
                            StatCol("Avg Survival", "${avgSurv / 60}m${avgSurv % 60}s")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCol(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
