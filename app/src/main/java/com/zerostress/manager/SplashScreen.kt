package com.zerostress.manager

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.zerostress.manager.fcm.ZSFCMService
import com.zerostress.manager.ui.theme.Accent
import com.zerostress.manager.ui.theme.BgCard
import com.zerostress.manager.ui.theme.BgMain
import com.zerostress.manager.ui.theme.Cyan
import com.zerostress.manager.ui.theme.Primary
import com.zerostress.manager.ui.theme.TextMuted

private val SPLASH_MESSAGES = arrayOf(
    "Initializing system...",
    "Loading player data...",
    "Syncing leaderboards...",
    "Connecting to voice servers...",
    "Preparing battle arena...",
    "Almost ready..."
)

/**
 * Splash screen as a Compose route. Determines start destination based on auth + role.
 */
@Composable
fun SplashScreen(onFinish: (String) -> Unit) {
    var progress by remember { mutableFloatStateOf(0f) }
    var messageIndex by remember { mutableIntStateOf(0) }
    var navigated by remember { mutableStateOf(false) }

    val logoAlpha by animateFloatAsState(
        targetValue = if (progress > 0f) 1f else 0f,
        animationSpec = tween(500), label = "logoAlpha"
    )
    val logoScale by animateFloatAsState(
        targetValue = if (progress > 0f) 1f else 0.5f,
        animationSpec = tween(500), label = "logoScale"
    )
    val titleAlpha by animateFloatAsState(
        targetValue = if (progress >= 0.3f) 1f else 0f,
        animationSpec = tween(400), label = "titleAlpha"
    )
    val loadingAlpha by animateFloatAsState(
        targetValue = if (progress >= 0.6f) 1f else 0f,
        animationSpec = tween(300), label = "loadingAlpha"
    )

    LaunchedEffect(Unit) {
        while (progress < 1f) {
            kotlinx.coroutines.delay(100)
            progress = (progress + 0.05f).coerceAtMost(1f)
            messageIndex = ((progress * 100).toInt() / 20)
                .coerceAtMost(SPLASH_MESSAGES.size - 1)
        }
        if (!navigated) {
            navigated = true
            resolveStartDestination(LocalContext.current) { role -> onFinish(role) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgMain),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Zero Stress logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(110.dp)
                    .alpha(logoAlpha)
                    .scale(logoScale)
                    .background(Primary, RoundedCornerShape(26.dp))
                    .padding(14.dp)
            )
            Spacer(Modifier.height(18.dp))
            Text(
                "ZERO STRESS",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                modifier = Modifier.alpha(titleAlpha)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Performance & Leaderboard Manager",
                color = Cyan,
                fontSize = 14.sp,
                modifier = Modifier.alpha(titleAlpha)
            )
            Spacer(Modifier.height(56.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(280.dp)
                    .alpha(loadingAlpha)
            ) {
                Text(
                    SPLASH_MESSAGES[messageIndex],
                    color = TextMuted,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = Accent,
                    trackColor = BgCard
                )
            }
        }
    }
}

private fun resolveStartDestination(context: android.content.Context, onResult: (String) -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    if (user == null) {
        onResult("logged_out")
        return
    }
    // Save FCM token so push notifications keep working across restarts
    ZSFCMService.saveTokenToFirestore(context)
    FirebaseFirestore.getInstance().collection("players").document(user.uid).get()
        .addOnSuccessListener { doc ->
            onResult(
                when {
                    doc.exists() && doc.getString("role") == "admin" -> "admin"
                    doc.exists() -> "player"
                    else -> "logged_out"
                }
            )
        }
        .addOnFailureListener { onResult("logged_out") }
}
