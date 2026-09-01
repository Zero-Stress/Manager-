package com.zerostress.manager.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.zerostress.manager.models.User
import com.zerostress.manager.ui.theme.*
import com.zerostress.manager.viewmodel.AppViewModel

@Composable
fun PlayerManagementScreen(appViewModel: AppViewModel) {
    val users by appViewModel.users.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingPhone by remember { mutableStateOf("") }
    var dialogType by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Player Management", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Accent)
            Button(
                onClick = { dialogType = "add"; showDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Success),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Player", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn {
            itemsIndexed(users) { index, user ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(user.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("+880 ${user.phone}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (user.status == "confirmed") Success.copy(alpha = 0.15f) else Warning.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        user.status.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                                        color = if (user.status == "confirmed") Success else Warning,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.outline) {
                                    Text(user.role.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                }
                            }
                        }
                        Row {
                            if (user.status == "pending") {
                                IconButton(onClick = { appViewModel.approvePlayer(user.phone) }) {
                                    Icon(Icons.Filled.CheckCircle, "Approve", tint = Success)
                                }
                            } else {
                                IconButton(onClick = { appViewModel.setPlayerPending(user.phone) }) {
                                    Icon(Icons.Filled.HourglassTop, "Set Pending", tint = Warning)
                                }
                            }
                            IconButton(onClick = { editingPhone = user.phone; dialogType = "resetPass"; showDialog = true }) {
                                Icon(Icons.Filled.LockReset, "Reset Password", tint = Accent)
                            }
                            IconButton(onClick = { editingPhone = user.phone; dialogType = "role"; showDialog = true }) {
                                Icon(Icons.Filled.AdminPanelSettings, "Change Role", tint = Accent)
                            }
                            IconButton(onClick = { appViewModel.deletePlayer(user.phone) }) {
                                Icon(Icons.Filled.Delete, "Delete", tint = Danger)
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showDialog) {
        when (dialogType) {
            "add" -> AddPlayerDialog(onDismiss = { showDialog = false }) { user ->
                appViewModel.addPlayer(user)
                showDialog = false
            }
            "resetPass" -> ResetPasswordDialog(phone = editingPhone, onDismiss = { showDialog = false }) { newPass ->
                appViewModel.resetPlayerPassword(editingPhone, newPass)
                showDialog = false
            }
            "role" -> ChangeRoleDialog(phone = editingPhone, onDismiss = { showDialog = false }) { newRole ->
                appViewModel.updatePlayerRole(editingPhone, newRole)
                showDialog = false
            }
        }
    }
}

@Composable
fun AddPlayerDialog(onDismiss: () -> Unit, onAdd: (User) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Player", fontWeight = FontWeight.Bold, color = Accent) },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it.filter { c -> c.isDigit() }.take(10) }, label = { Text("Phone (10 digits)") }, singleLine = true)
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Temporary Password") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(User(phone = phone, name = name, password = password, status = "confirmed")) },
                colors = ButtonDefaults.buttonColors(containerColor = Success)) {
                Text("Add Player", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    )
}

@Composable
fun ResetPasswordDialog(phone: String, onDismiss: () -> Unit, onReset: (String) -> Unit) {
    var newPass by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset Password", fontWeight = FontWeight.Bold, color = Accent) },
        text = {
            Column {
                Text("For: +880 $phone", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = newPass, onValueChange = { newPass = it }, label = { Text("New Password") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = { if (newPass.isNotBlank()) onReset(newPass) },
                colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
                Text("Reset", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ChangeRoleDialog(phone: String, onDismiss: () -> Unit, onChange: (String) -> Unit) {
    var role by remember { mutableStateOf("player") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Role", fontWeight = FontWeight.Bold, color = Accent) },
        text = {
            Column {
                Text("For: +880 $phone", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(selected = role == "player", onClick = { role = "player" }, label = { Text("Player") })
                    FilterChip(selected = role == "admin", onClick = { role = "admin" }, label = { Text("Admin") })
                    FilterChip(selected = role == "moderator", onClick = { role = "moderator" }, label = { Text("Moderator") })
                }
            }
        },
        confirmButton = {
            Button(onClick = { onChange(role) }, colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
                Text("Update", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
