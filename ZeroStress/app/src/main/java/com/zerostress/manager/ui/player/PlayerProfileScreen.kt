package com.zerostress.manager.ui.player

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerostress.manager.ui.theme.*
import com.zerostress.manager.viewmodel.AppViewModel
import java.io.File
import java.io.FileOutputStream

@Composable
fun PlayerProfileScreen(phone: String, name: String, appViewModel: AppViewModel) {
    val context = LocalContext.current
    val dailyLogs by appViewModel.dailyLogs.collectAsState()
    val playerLogs = dailyLogs.filter { it.phone == phone }

    val totalMatches = playerLogs.sumOf { it.matches }
    val totalWins = playerLogs.sumOf { it.wins }
    val totalKills = playerLogs.sumOf { it.kills }
    val totalAssists = playerLogs.sumOf { it.assists }
    val totalDamage = playerLogs.sumOf { it.damage }
    val winRate = if (totalMatches > 0) (totalWins.toDouble() / totalMatches) * 100 else 0.0
    val avgDamage = if (totalMatches > 0) totalDamage.toDouble() / totalMatches else 0.0
    val score = (totalKills * 10.0) + (totalDamage / 100.0) + (totalWins * 50.0)

    Column(modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("Player Profile", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Accent)
        Spacer(modifier = Modifier.height(8.dp))

        // Player header
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(modifier = Modifier.size(64.dp), shape = RoundedCornerShape(32.dp), color = Accent) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(name.take(1).uppercase(), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = DarkBg)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(name, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                Text("+880 $phone", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stats grid
        val stats = listOf(
            "Lifetime Matches" to "$totalMatches",
            "Lifetime Wins" to "$totalWins",
            "Win Rate" to "${String.format("%.1f", winRate)}%",
            "Total Kills" to "$totalKills",
            "Total Assists" to "$totalAssists",
            "Total Damage" to "$totalDamage",
            "Avg Damage/Match" to "${String.format("%.0f", avgDamage)}",
            "Lifetime Score" to "${score.toInt()} pts"
        )

        stats.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (label, value) ->
                    Card(modifier = Modifier.weight(1f).padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Accent)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Export scorecard
        Button(
            onClick = {
                val bitmap = createScorecardBitmap(name, phone, totalMatches, totalWins, winRate, totalKills, totalAssists, totalDamage, avgDamage, score)
                saveBitmapToGallery(context, bitmap, "ZeroStress_${name}_${System.currentTimeMillis()}.png")
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Success)
        ) {
            Icon(Icons.Filled.Share, null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Export Scorecard", fontWeight = FontWeight.ExtraBold, color = androidx.compose.ui.graphics.Color.White)
        }
    }
}

private fun createScorecardBitmap(
    name: String, phone: String, matches: Int, wins: Int, winRate: Double,
    kills: Int, assists: Int, damage: Int, avgDmg: Double, score: Double
): Bitmap {
    val width = 800; val height = 600
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint().apply { isAntiAlias = true }

    // Background
    paint.color = android.graphics.Color.parseColor("#06090F")
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

    // Header
    paint.color = android.graphics.Color.parseColor("#38BDF8")
    paint.textSize = 48f; paint.isFakeBoldText = true
    canvas.drawText("ZERO STRESS", 40f, 70f, paint)

    // Player name
    paint.color = android.graphics.Color.WHITE; paint.textSize = 32f
    canvas.drawText(name, 40f, 130f, paint)

    paint.color = android.graphics.Color.parseColor("#64748B"); paint.textSize = 20f
    canvas.drawText("+880 $phone", 40f, 160f, paint)

    // Stats
    paint.textSize = 22f; paint.color = android.graphics.Color.WHITE
    val stats = arrayOf(
        "Matches: $matches", "Wins: $wins", "Win Rate: ${String.format("%.1f", winRate)}%",
        "Kills: $kills", "Assists: $assists", "Damage: $damage",
        "Avg Damage: ${String.format("%.0f", avgDmg)}", "Score: ${score.toInt()} pts"
    )
    var y = 220f
    stats.forEach { stat ->
        canvas.drawText(stat, 40f, y, paint)
        y += 40f
    }

    // Score highlight
    paint.color = android.graphics.Color.parseColor("#38BDF8"); paint.textSize = 36f; paint.isFakeBoldText = true
    canvas.drawText("${score.toInt()} TOTAL POINTS", 40f, y + 30f, paint)

    return bitmap
}

private fun saveBitmapToGallery(context: Context, bitmap: Bitmap, filename: String) {
    try {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "ZeroStress")
        dir.mkdirs()
        val file = File(dir, filename)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    } catch (e: Exception) { e.printStackTrace() }
}
