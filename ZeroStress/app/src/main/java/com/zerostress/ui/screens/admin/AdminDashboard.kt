package com.zerostress.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerostress.data.model.Player

@Composable
fun AdminDashboard(
    players: List<Player>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    onDelete: (String) -> Unit,
    onAddPlayer: (String, String, String) -> Unit,
    onResetPassword: (String, String) -> Unit,
    onRoleChange: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val pending = players.filter { it.status == "pending" }
    val confirmed = players.filter { it.status == "confirmed" }
    var showAddDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf<Player?>(null) }
    var showRoleDialog by remember { mutableStateOf<Player?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("📋 Player Management", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                FilledTonalButton(onClick = { showAddDialog = true }) {
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
                    onReject = { onReject(player.phone) }, onDelete = { onDelete(player.phone) },
                    onResetPassword = { showResetDialog = player },
                    onRoleChange = { showRoleDialog = player })
            }
        }

        item {
            Text("✅ Active Players (${confirmed.size})", fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(vertical = 4.dp))
        }
        items(confirmed) { player ->
            PlayerCard(player = player, onApprove = {}, onReject = {},
                onDelete = { onDelete(player.phone) },
                onResetPassword = { showResetDialog = player },
                onRoleChange = { showRoleDialog = player })
        }
    }

    // Add Player Dialog
    if (showAddDialog) {
        AddPlayerDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, phone, password -> onAddPlayer(name, phone, password); showAddDialog = false }
        )
    }

    // Reset Password Dialog
    showResetDialog?.let { player ->
        ResetPasswordDialog(
            playerName = player.name,
            onDismiss = { showResetDialog = null },
            onReset = { newPassword -> onResetPassword(player.phone, newPassword); showResetDialog = null }
        )
    }

    // Role Change Dialog
    showRoleDialog?.let { player ->
        RoleChangeDialog(
            playerName = player.name,
            currentRole = player.role,
            onDismiss = { showRoleDialog = null },
            onChange = { newRole -> onRoleChange(player.phone, newRole); showRoleDialog = null }
        )
    }
}

@Composable
fun PlayerCard(
    player: Player, onApprove: () -> Unit, onReject: () -> Unit,
    onDelete: () -> Unit, onResetPassword: () -> Unit, onRoleChange: () -> Unit,
    showActions: Boolean = true
) {
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
                    // Role badge
                    Text(player.role.uppercase(), style = MaterialTheme.typography.labelSmall,
                        color = when (player.role) {
                            "admin" -> MaterialTheme.colorScheme.error
                            "moderator" -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .background(
                                when (player.role) {
                                    "admin" -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                                    "moderator" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                },
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 1.dp))
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, null)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Change Role") },
                            onClick = { showMenu = false; onRoleChange() },
                            leadingIcon = { Icon(Icons.Default.AdminPanelSettings, null, tint = MaterialTheme.colorScheme.primary) }
                        )
                        DropdownMenuItem(
                            text = { Text("Reset Password") },
                            onClick = { showMenu = false; onResetPassword() },
                            leadingIcon = { Icon(Icons.Default.Key, null, tint = MaterialTheme.colorScheme.tertiary) }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; onDelete() },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }
            if (showActions && player.status == "pending") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onApprove, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Approve", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reject")
                    }
                }
            }
        }
    }
}

@Composable
fun AddPlayerDialog(onDismiss: () -> Unit, onAdd: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.PersonAdd, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Add New Player") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Player Name") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = phone, onValueChange = { phone = it.filter { c -> c.isDigit() } },
                    label = { Text("Phone (10 digits)") },
                    prefix = { Text("+880 ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = password, onValueChange = { password = it },
                    label = { Text("Password") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(name, phone, password) },
                enabled = name.isNotBlank() && phone.length >= 10 && password.isNotEmpty()) {
                Text("Add Player")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ResetPasswordDialog(playerName: String, onDismiss: () -> Unit, onReset: (String) -> Unit) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Key, null, tint = MaterialTheme.colorScheme.tertiary) },
        title = { Text("Reset Password") },
        text = {
            Column {
                Text("Reset password for $playerName", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = newPassword, onValueChange = { newPassword = it },
                    label = { Text("New Password") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = confirmPassword, onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                error?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (newPassword.length < 6) error = "Password must be at least 6 characters"
                else if (newPassword != confirmPassword) error = "Passwords don't match"
                else { error = null; onReset(newPassword) }
            }, enabled = newPassword.isNotEmpty()) {
                Text("Reset Password")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun RoleChangeDialog(playerName: String, currentRole: String, onDismiss: () -> Unit, onChange: (String) -> Unit) {
    var selectedRole by remember { mutableStateOf(currentRole) }
    val roles = listOf(
        "player" to "Player - Standard access",
        "moderator" to "Moderator - Can manage chat & announcements",
        "admin" to "Admin - Full access to all features"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.AdminPanelSettings, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Change Role") },
        text = {
            Column {
                Text("Change role for $playerName", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(16.dp))
                roles.forEach { (role, description) ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedRole == role) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            else MaterialTheme.colorScheme.surface
                        ),
                        onClick = { selectedRole = role }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedRole == role, onClick = { selectedRole = role })
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(role.uppercase(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(description, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onChange(selectedRole) }, enabled = selectedRole != currentRole) {
                Text("Change to ${selectedRole.uppercase()}")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
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
