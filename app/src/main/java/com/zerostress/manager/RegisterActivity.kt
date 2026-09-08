package com.zerostress.manager

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.zerostress.manager.ui.theme.*

class RegisterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZeroStressTheme {
                RegisterScreen(
                    onRegisterSuccess = {
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    },
                    onLoginClick = {
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun RegisterScreen(onRegisterSuccess: () -> Unit, onLoginClick: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ZSBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🎯 ZERO STRESS", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Create Account", fontSize = 16.sp, color = ZSTextMuted)
            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ZSCard)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Player Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMsg.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(errorMsg, color = Color.Red, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (name.isBlank() || phone.isBlank() || password.isBlank()) {
                                errorMsg = "All fields are required"
                                return@Button
                            }
                            if (password.length < 6) {
                                errorMsg = "Min 6 characters"
                                return@Button
                            }
                            isLoading = true
                            errorMsg = ""
                            val email = "${phone.trim()}@zerostress.local"
                            FirebaseAuth.getInstance()
                                .createUserWithEmailAndPassword(email, password.trim())
                                .addOnSuccessListener { result ->
                                    val uid = result.user?.uid ?: return@addOnSuccessListener
                                    val playerData = hashMapOf(
                                        "uid" to uid,
                                        "name" to name.trim(),
                                        "phone" to phone.trim(),
                                        "role" to "player",
                                        "status" to "pending",
                                        "score" to 0, "kills" to 0, "deaths" to 0,
                                        "assists" to 0, "damage" to 0L,
                                        "wins" to 0, "matches" to 0,
                                        "xp" to 0, "level" to 1, "coins" to 0,
                                        "rank" to "Iron"
                                    )
                                    FirebaseFirestore.getInstance()
                                        .collection("players").document(uid)
                                        .set(playerData)
                                        .addOnSuccessListener {
                                            isLoading = false
                                            onRegisterSuccess()
                                        }
                                        .addOnFailureListener { e ->
                                            isLoading = false
                                            errorMsg = "Error: ${e.message}"
                                        }
                                }
                                .addOnFailureListener { e ->
                                    isLoading = false
                                    errorMsg = "Failed: ${e.message}"
                                }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                        else Text("Register")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onLoginClick) {
                Text("Already have an account? Login", color = ZSTextMuted)
            }
        }
    }
}
