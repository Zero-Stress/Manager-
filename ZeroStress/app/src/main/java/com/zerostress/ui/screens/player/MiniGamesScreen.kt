package com.zerostress.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerostress.ui.theme.*
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniGamesScreen(
    playerName: String,
    onAddCoins: (String, Int) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("🧠 Trivia", "🎯 Prediction", "✊ RPS")

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("🎮 Mini Games") })
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 11.sp) })
            }
        }
        when (selectedTab) {
            0 -> TriviaGame(playerName, onAddCoins)
            1 -> PredictionGame(playerName, onAddCoins)
            2 -> RPSGame(playerName, onAddCoins)
        }
    }
}

@Composable
private fun TriviaGame(playerName: String, onAddCoins: (String, Int) -> Unit) {
    val questions = listOf(
        Triple("What does K/D stand for in gaming?", "Kill/Death Ratio", listOf("Kill/Death Ratio", "Key/Damage", "Kill/Damage", "Kick/Defend")),
        Triple("What is a 'clutch' in gaming?", "Winning when outnumbered", listOf("Dropping weapons", "Winning when outnumbered", "Teaming up", "Running away")),
        Triple("What does MVP stand for?", "Most Valuable Player", listOf("Most Valuable Player", "Match Victory Point", "Most Violent Player", "Multi-View Play")),
        Triple("What is 'loot' in gaming?", "Items collected from enemies", listOf("A type of weapon", "Items collected from enemies", "A game map", "Team strategy")),
        Triple("What is an 'FPS' game?", "First-Person Shooter", listOf("Frames Per Second", "First-Person Shooter", "Free Player Selection", "Fast Pace Strategy")),
        Triple("What does 'GG' mean?", "Good Game", listOf("Great Goal", "Good Game", "Go Go", "Game Glitch")),
        Triple("What is a 'headshot'?", "Hitting the head area", listOf("A new hairstyle", "Hitting the head area", "Starting a match", "Losing badly")),
        Triple("What is 'respawn'?", "Coming back after elimination", listOf("A new map", "Coming back after elimination", "Power-up", "Team formation")),
        Triple("What does 'AFK' mean?", "Away From Keyboard", listOf("Always Free to Kill", "Away From Keyboard", "All Friendly Kills", "A First Kill")),
        Triple("What is 'MMR'?", "Matchmaking Rating", listOf("Most Match Rating", "Matchmaking Rating", "Multi-Mode Ranking", "Maximum Match Record"))
    )

    var currentQ by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var answered by remember { mutableStateOf(false) }
    var selectedAnswer by remember { mutableStateOf(-1) }
    var showResult by remember { mutableStateOf(false) }

    val q = questions[currentQ]

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🧠", fontSize = 24.sp)
                        Text("Score", fontSize = 11.sp, color = Color.Gray)
                        Text("$score", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = ZSGreen)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📋", fontSize = 24.sp)
                        Text("Question", fontSize = 11.sp, color = Color.Gray)
                        Text("${currentQ + 1}/${questions.size}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🪙", fontSize = 24.sp)
                        Text("Reward", fontSize = 11.sp, color = Color.Gray)
                        Text("${score * 5}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = ZSOrange)
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Q${currentQ + 1}", color = ZSBlue, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(q.first, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 24.sp)
                }
            }
        }

        items(q.third.shuffled()) { answer ->
            val isCorrect = answer == q.second
            val isSelected = selectedAnswer == q.third.indexOf(answer)
            Card(shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().clickable(enabled = !answered) {
                    answered = true; selectedAnswer = q.third.indexOf(answer)
                    if (isCorrect) score++
                },
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        answered && isCorrect -> ZSGreen
                        answered && isSelected && !isCorrect -> ZSRed
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )) {
                Text(answer, modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Medium,
                    color = if (answered && (isCorrect || isSelected)) Color.White else MaterialTheme.colorScheme.onSurface)
            }
        }

        if (answered) {
            item {
                Button(onClick = {
                    if (currentQ < questions.size - 1) { currentQ++; answered = false; selectedAnswer = -1 }
                    else { showResult = true; onAddCoins(playerName, score * 5) }
                }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp)) {
                    Text(if (currentQ < questions.size - 1) "NEXT QUESTION" else "CLAIM REWARD", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showResult) {
            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎉", fontSize = 48.sp)
                        Text("Quiz Complete!", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                        Text("Score: $score/${questions.size}", fontSize = 16.sp, color = ZSGreen)
                        Text("Earned: ${score * 5} ZS Coins", color = ZSOrange, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PredictionGame(playerName: String, onAddCoins: (String, Int) -> Unit) {
    var predictedKills by remember { mutableStateOf("") }
    var actualKills by remember { mutableIntStateOf(-1) }
    var coinsWon by remember { mutableIntStateOf(0) }
    var showResult by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎯", fontSize = 48.sp)
                    Text("Match Prediction", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Predict your kills next match!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            OutlinedTextField(value = predictedKills, onValueChange = { predictedKills = it },
                label = { Text("Your predicted kills") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp))
        }

        item {
            Button(onClick = {
                actualKills = Random.nextInt(0, 20)
                val predicted = predictedKills.toIntOrNull() ?: 0
                val diff = kotlin.math.abs(predicted - actualKills)
                coinsWon = when {
                    diff == 0 -> 50
                    diff <= 2 -> 20
                    diff <= 5 -> 10
                    else -> 0
                }
                showResult = true
                if (coinsWon > 0) onAddCoins(playerName, coinsWon)
            }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp),
                enabled = predictedKills.isNotBlank() && !showResult) {
                Text("SUBMIT PREDICTION", fontWeight = FontWeight.Bold)
            }
        }

        if (showResult) {
            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Result", color = Color.Gray, fontSize = 12.sp)
                        Text("You predicted: $predictedKills", fontSize = 16.sp, color = Color.White)
                        Text("Actual kills: $actualKills", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = ZSBlue)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(if (coinsWon > 0) "🎉 Won $coinsWon ZS Coins!" else "😔 No coins this time",
                            fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (coinsWon > 0) ZSGreen else ZSRed)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { showResult = false; predictedKills = ""; actualKills = -1 },
                            modifier = Modifier.fillMaxWidth()) { Text("PLAY AGAIN") }
                    }
                }
            }
        }
    }
}

@Composable
private fun RPSGame(playerName: String, onAddCoins: (String, Int) -> Unit) {
    val choices = listOf("✊" to "Rock", "✋" to "Scissors", "✌️" to "Paper")
    var playerChoice by remember { mutableIntStateOf(-1) }
    var computerChoice by remember { mutableIntStateOf(-1) }
    var result by remember { mutableStateOf("") }
    var wins by remember { mutableIntStateOf(0) }
    var losses by remember { mutableIntStateOf(0) }
    var coins by remember { mutableIntStateOf(0) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏆", fontSize = 20.sp)
                        Text("$wins", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = ZSGreen)
                        Text("Wins", fontSize = 11.sp, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("😤", fontSize = 20.sp)
                        Text("$losses", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = ZSRed)
                        Text("Losses", fontSize = 11.sp, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🪙", fontSize = 20.sp)
                        Text("$coins", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = ZSOrange)
                        Text("Earned", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }

        item { Text("Choose your weapon!", fontWeight = FontWeight.Bold, fontSize = 18.sp) }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically) {
                // Player side
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("You", fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(ZSBlue), contentAlignment = Alignment.Center) {
                        Text(if (playerChoice >= 0) choices[playerChoice].first else "?", fontSize = 36.sp)
                    }
                }
                Text("VS", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = ZSOrange)
                // Computer side
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("CPU", fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(ZSRed), contentAlignment = Alignment.Center) {
                        Text(if (computerChoice >= 0) choices[computerChoice].first else "?", fontSize = 36.sp)
                    }
                }
            }
        }

        if (result.isNotEmpty()) {
            item {
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(
                    containerColor = when { result.contains("Win") -> ZSGreen; result.contains("Lose") -> ZSRed; else -> ZSOrange }
                )) {
                    Text(result, modifier = Modifier.padding(16.dp).fillMaxWidth(), textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }
            }
        }

        item {
            Text("Rock 🤜 Paper ✋ Scissors ✌️", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        items(choices) { (emoji, name) ->
            Button(onClick = {
                playerChoice = choices.indexOfFirst { it.first == emoji }
                computerChoice = Random.nextInt(3)
                val p = playerChoice
                val c = computerChoice
                result = when {
                    p == c -> "🤝 It's a Draw! +2 coins"
                    (p == 0 && c == 1) || (p == 1 && c == 2) || (p == 2 && c == 0) -> { wins++; coins += 5; "🎉 You Win! +5 coins" }
                    else -> { losses++; "😔 You Lose! +1 coin" }
                }
                if (result.contains("Draw")) coins += 2
                if (result.contains("Lose")) coins++
                onAddCoins(playerName, if (result.contains("Win")) 5 else if (result.contains("Draw")) 2 else 1)
            }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp)) {
                Text("$emoji $name", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
