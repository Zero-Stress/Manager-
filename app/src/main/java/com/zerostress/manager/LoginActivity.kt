package com.zerostress.manager

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.zerostress.manager.fcm.FCMConfig
import com.zerostress.manager.ui.theme.Accent
import com.zerostress.manager.ui.theme.BgMain
import com.zerostress.manager.ui.theme.BgCard
import com.zerostress.manager.ui.theme.Cyan
import com.zerostress.manager.ui.theme.GradientPrimary
import com.zerostress.manager.ui.theme.Primary
import com.zerostress.manager.ui.theme.TextMuted
import com.zerostress.manager.ui.theme.TextSecondary
import com.zerostress.manager.ui.theme.ZeroStressTheme

class LoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FCMConfig.checkFCMConfiguration(this)
        requestNotificationPermission()
        setContent {
            ZeroStressTheme {
                LoginScreen(
                    onLoggedIn = { role ->
                        val target = if (role == "admin") AdminDashboardActivity::class.java
                        else PlayerDashboardActivity::class.java
                        startActivity(
                            Intent(this, target).setFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            )
                        )
                        finish()
                    },
                    onGoRegister = { startActivity(Intent(this, RegisterActivity::class.java)) }
                )
            }
        }
    }

    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                FirebaseMessaging.getInstance().subscribeToTopic("all_players")
            }
        }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
fun LoginScreen(onLoggedIn: (String) -> Unit, onGoRegister: () -> Unit) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showForgot by remember { mutableStateOf(false) }
    var forgotPhone by remember { mutableStateOf("") }
    var showResetSent by remember { mutableStateOf(false) }
    var resetSentPhone by remember { mutableStateOf("") }
    var playedIntro by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { playedIntro = true }

    Box(
        Modifier
            .fillMaxSize()
            .background(BgMain)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(72.dp))

            // Logo
            AnimatedVisibility(
                visible = playedIntro,
                enter = fadeIn(tween(800)) + slideInVertically(tween(800)) { it / 2 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(GradientPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("ZS", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "ZERO STRESS",
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Login to Leaderboards", color = TextSecondary, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(36.dp))

            // Login card
            AnimatedVisibility(
                visible = playedIntro,
                enter = fadeIn(tween(700, delayMillis = 300)) + slideInVertically(tween(700, delayMillis = 300)) { it / 3 }
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(BgCard)
                        .padding(20.dp)
                ) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Accent,
                            unfocusedBorderColor = Color(0xFF4A5568),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Accent
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Accent,
                            unfocusedBorderColor = Color(0xFF4A5568),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Accent
                        )
                    )
                    error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = Color(0xFFFC4A1A), fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = {
                            if (phone.isBlank()) {
                                error = "Enter phone number"
                                return@Button
                            }
                            if (password.isBlank()) {
                                error = "Enter password"
                                return@Button
                            }
                            loading = true
                            error = null
                            val email = "${phone.trim()}@zerostress.local"
                            auth.signInWithEmailAndPassword(email, password)
                                .addOnSuccessListener { result ->
                                    val uid = result.user?.uid
                                    if (uid == null) {
                                        loading = false
                                        error = "Login failed"
                                        return@addOnSuccessListener
                                    }
                                    saveFcmToken(uid)
                                    db.collection("players").document(uid).get()
                                        .addOnSuccessListener { doc ->
                                            loading = false
                                            if (doc.exists()) {
                                                onLoggedIn(doc.getString("role") ?: "player")
                                            } else {
                                                error = "Player not found"
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            loading = false
                                            error = "Error: ${e.message}"
                                        }
                                }
                                .addOnFailureListener { e ->
                                    loading = false
                                    error = "Login failed: ${e.message}"
                                }
                        },
                        enabled = !loading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color(0xFF090D16)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                color = Color(0xFF090D16),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Text("LOGIN", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        TextButton(onClick = { showForgot = true }) {
                            Text("Forgot password?", color = TextMuted, fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            AnimatedVisibility(
                visible = playedIntro,
                enter = fadeIn(tween(500, delayMillis = 700))
            ) {
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    Text("New player? ", color = TextSecondary, fontSize = 14.sp)
                    Text(
                        "Create account",
                        color = Cyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickableNoRipple { onGoRegister() }
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
        }

        // Forgot password dialog
        if (showForgot) {
            AlertDialog(
                onDismissRequest = { showForgot = false },
                title = { Text("Forgot Password", color = Color.White) },
                text = {
                    Column {
                        Text(
                            "Enter your phone number so we can send a password reset link",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = forgotPhone,
                            onValueChange = { forgotPhone = it },
                            label = { Text("Enter your phone number") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Accent,
                                unfocusedBorderColor = Color(0xFF4A5568),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Accent
                            )
                        )
                    }
                },
                containerColor = BgCard,
                confirmButton = {
                    TextButton(onClick = {
                        if (forgotPhone.isNotBlank()) {
                            resetSentPhone = forgotPhone.trim()
                            showForgot = false
                            showResetSent = true
                        }
                    }) { Text("Send Reset Link", color = Accent) }
                },
                dismissButton = {
                    TextButton(onClick = { showForgot = false }) { Text("Cancel", color = TextMuted) }
                }
            )
        }

        if (showResetSent) {
            AlertDialog(
                onDismissRequest = { showResetSent = false },
                title = { Text("Reset Link Sent", color = Color.White) },
                text = {
                    Text(
                        "A password reset link has been sent to $resetSentPhone@zerostress.local\n\nCheck your email to reset your password.",
                        color = TextSecondary
                    )
                },
                containerColor = BgCard,
                confirmButton = {
                    TextButton(onClick = { showResetSent = false }) { Text("OK", color = Accent) }
                }
            )
        }
    }
}

private fun saveFcmToken(uid: String) {
    FirebaseMessaging.getInstance().token
        .addOnSuccessListener { token ->
            if (token != null) {
                FirebaseFirestore.getInstance()
                    .collection("players").document(uid)
                    .update("fcmToken", token)
            }
        }
    FirebaseMessaging.getInstance().subscribeToTopic("all_players")
    FirebaseMessaging.getInstance().subscribeToTopic("match_updates")
    FirebaseMessaging.getInstance().subscribeToTopic("announcements")
}
