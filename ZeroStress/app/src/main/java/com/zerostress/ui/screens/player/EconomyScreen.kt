package com.zerostress.ui.screens.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerostress.data.model.*
import com.zerostress.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EconomyScreen(
    coinBalance: ZSCoinBalance?,
    shopItems: List<ShopItem>,
    playerName: String,
    onSpinWheel: () -> Int,
    onPurchase: (ShopItem) -> Unit,
    onBuyLootBox: (Int) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("🪙 Coins", "🛒 Shop", "🎰 Spin", "🎁 Loot Box")
    var spinResult by remember { mutableIntStateOf(0) }
    var showSpinDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("💰 Economy") })

        // Coin balance header
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🪙", fontSize = 36.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("ZS Coins", color = Color.Gray, fontSize = 12.sp)
                    Text("${coinBalance?.balance ?: 0}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                }
                Spacer(modifier = Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Earned", color = Color.Gray, fontSize = 11.sp)
                    Text("${coinBalance?.totalEarned ?: 0}", color = ZSGreen, fontWeight = FontWeight.Bold)
                }
            }
        }

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 11.sp) })
            }
        }

        when (selectedTab) {
            0 -> CoinsTab(coinBalance)
            1 -> ShopTab(shopItems, onPurchase)
            2 -> SpinTab(onSpinWheel = {
                spinResult = onSpinWheel()
                showSpinDialog = true
            })
            3 -> LootBoxTab(onBuyLootBox)
        }
    }

    if (showSpinDialog) {
        AlertDialog(onDismissRequest = { showSpinDialog = false },
            title = { Text("🎉 Congratulations!", textAlign = TextAlign.Center) },
            text = { Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎰", fontSize = 64.sp)
                Text("You won $spinResult ZS Coins!", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = ZSOrange)
            }},
            confirmButton = { Button(onClick = { showSpinDialog = false }) { Text("CLAIM") } }
        )
    }
}

@Composable
private fun CoinsTab(balance: ZSCoinBalance?) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("📊 Your Stats", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatItem("Balance", "${balance?.balance ?: 0}", "🪙")
                        StatItem("Earned", "${balance?.totalEarned ?: 0}", "📈")
                        StatItem("Spent", "${balance?.totalSpent ?: 0}", "🛒")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatItem("Streak", "${balance?.loginStreak ?: 0} days", "🔥")
                        StatItem("Daily Spin", if ((balance?.lastDailySpin ?: 0) > System.currentTimeMillis() - 86400000) "Used" else "Available", "🎰")
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2D1B))) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🎁", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("How to Earn Coins", fontWeight = FontWeight.Bold, color = ZSGreen)
                        Text("• Win matches: +10 coins", fontSize = 12.sp)
                        Text("• Daily login: +5 coins", fontSize = 12.sp)
                        Text("• Complete missions: +5-100 coins", fontSize = 12.sp)
                        Text("• Weekly streak: +25 coins", fontSize = 12.sp)
                        Text("• Refer friends: +50 coins", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, icon: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 20.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ShopTab(items: List<ShopItem>, onPurchase: (ShopItem) -> Unit) {
    val categories = listOf("All", "Theme", "Badge", "Title", "Border", "Avatar")
    var selectedCategory by remember { mutableStateOf("All") }
    val filtered = if (selectedCategory == "All") items else items.filter { it.category.equals(selectedCategory, ignoreCase = true) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                categories.forEach { cat ->
                    FilterChip(selected = selectedCategory == cat, onClick = { selectedCategory = cat }, label = { Text(cat, fontSize = 11.sp) })
                }
            }
        }

        items(filtered) { item ->
            Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                        Text(item.icon.ifEmpty { "🎁" }, fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.name, fontWeight = FontWeight.Bold)
                        Text(item.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${item.price} 🪙", fontWeight = FontWeight.Bold, color = ZSOrange)
                        if (item.isOwned) {
                            Text("✅ Owned", color = ZSGreen, fontSize = 11.sp)
                        } else {
                            Button(onClick = { onPurchase(item) }, shape = RoundedCornerShape(8.dp),
                                contentPadding = ButtonDefaults.ContentPadding) { Text("Buy", fontSize = 12.sp) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpinTab(onSpinWheel: () -> Unit) {
    var isSpinning by remember { mutableStateOf(false) }
    val rotation by rememberInfiniteTransition().animateFloat(
        initialValue = 0f, targetValue = 3600f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing))
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎰 Daily Spin", fontWeight = FontWeight.Bold, fontSize = 24.sp)
                Text("Spin once per day for free coins!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(24.dp))

                // Wheel visualization
                Box(contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(200.dp).clip(CircleShape)
                        .background(
                            androidx.compose.ui.graphics.Brush.sweepGradient(listOf(ZSBlue, ZSGreen, ZSOrange, ZSRed, Color(0xFF9C27B0), ZSBlue))
                        ).then(if (isSpinning) Modifier.rotate(rotation) else Modifier),
                        contentAlignment = Alignment.Center) {
                        Text("🎰", fontSize = 64.sp)
                    }
                    // Pointer
                    Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.offset(y = (-100).dp), tint = Color.White, fontSize = 40.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                val rewards = listOf("5", "10", "15", "20", "25", "50", "100")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rewards.forEach { r ->
                        Text("$r", modifier = Modifier.clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant).padding(6.dp), fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    isSpinning = true
                    onSpinWheel()
                }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp),
                    enabled = !isSpinning) {
                    Text("SPIN NOW!", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun LootBoxTab(onBuyLootBox: (Int) -> Unit) {
    val boxes = listOf(
        Triple("📦 Bronze Box", 50, "Common items + badges"),
        Triple("📦 Silver Box", 100, "Rare items + themes"),
        Triple("📦 Gold Box", 250, "Epic items + titles"),
        Triple("💎 Diamond Box", 500, "Legendary items + borders")
    )

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("🎁 Loot Boxes", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        items(boxes) { (name, cost, desc) ->
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(name, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(desc, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$cost 🪙", fontWeight = FontWeight.Bold, color = ZSOrange)
                    }
                    Button(onClick = { onBuyLootBox(cost) }, shape = RoundedCornerShape(12.dp)) {
                        Text("OPEN")
                    }
                }
            }
        }
    }
}


