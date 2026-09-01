package com.zerostress.manager.ui.auth

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zerostress.manager.ui.theme.*
import com.zerostress.manager.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onBack: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.padding(24.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(32.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Create Account", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Accent)
                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Player Name") },
                    placeholder = { Text("e.g. ShadowWarrior") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("+880 Phone Number", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))) {
                    Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
                        Text("+880", color = Accent, fontWeight = FontWeight.Bold)
                    }
                    OutlinedTextField(
                        value = phone, onValueChange = { if (it.length <= 10) phone = it.filter { c -> c.isDigit() } },
                        modifier = Modifier.weight(1f), placeholder = { Text("1700000000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text("Password") }, placeholder = { Text("********") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier.clickable { passwordVisible = !passwordVisible }
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent)
                )

                if (message.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(message, color = if (isError) Danger else Success, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (name.isBlank() || phone.length < 10 || password.isBlank()) {
                            message = "Please fill all fields"
                            isError = true
                            return@Button
                        }
                        authViewModel.register(name, phone, password) { success, msg ->
                            message = msg
                            isError = !success
                            if (success) onRegistered()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent)
                ) { Text("Register Account", fontWeight = FontWeight.ExtraBold, color = DarkBg) }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Already have an account?", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Text("Login", color = Accent, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.clickable { onBack() })
            }
        }
    }
}
