package com.zerostress.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerostress.data.model.Player

@Composable
fun AdminDashboard(
    players: List<Player>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    onDelete: (String) -> Unit,
    onAddPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pending = players.filter { it.status == "pending" }
    val confirmed = players.filter { it.status == "confirmed" }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("📋 Player Management", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                FilledTonalButton(onClick = onAddPlayer) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Player")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Stats
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminStatCard("Total", players.size, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                AdminStatCard("Pending", pending.size, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                AdminStatCard("Active", confirmed.size, MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (pending.isNotEmpty()) {
            item {
                Text("⏳ Pending Approval (${pending.size})", fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(vertical = 4.dp))
            }
            items(pending) { player ->
                PlayerCard(player = player, onApprove = { onApprove(player.phone) },
                    onReject = { onReject(player.phone) }, onDelete = { onDelete(player.phone) })
            }
        }

        item {
            Text("✅ Active Players (${confirmed.size})", fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(vertical = 4.dp))
        }
        items(confirmed) { player ->
            PlayerCard(player = player, onApprove = {}, onReject = {},
                onDelete = { onDelete(player.phone) }, showActions = false)
        }
    }
}

@Composable
fun PlayerCard(player: Player, onApprove: () -> Unit, onReject: () -> Unit,
               onDelete: () -> Unit, showActions: Boolean = true) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center) {
                    Text(player.name.take(1).uppercase(), color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(player.name, fontWeight = FontWeight.Bold)
                    Text(player.phone, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text(player.role.uppercase(), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, null)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("Delete") }, onClick = { showMenu = false; onDelete() },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) })
                    }
                }
            }
            if (showActions && player.status == "pending") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onApprove, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(8.dp)) {
                        Text("Approve", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(8.dp)) {
                        Text("Reject")
                    }
                }
            }
        }
    }
}

@Composable
fun AdminStatCard(label: String, value: Int, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$value", fontWeight = FontWeight.Black, color = color, fontSize = 24.sp)
            Text(label, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}
