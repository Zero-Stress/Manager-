package com.zerostress.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerostress.manager.ui.theme.Accent
import com.zerostress.manager.ui.theme.BgMain
import com.zerostress.manager.ui.theme.BgCard
import com.zerostress.manager.ui.theme.BgCardElevated
import com.zerostress.manager.ui.theme.Cyan
import com.zerostress.manager.ui.theme.Gold
import com.zerostress.manager.ui.theme.Primary
import com.zerostress.manager.ui.theme.PrimaryDark
import com.zerostress.manager.ui.theme.TextMuted
import com.zerostress.manager.ui.theme.TextSecondary

/** Click without ripple effect. */
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )

val GradientPrimary = Brush.linearGradient(listOf(Primary, PrimaryDark))
val GradientAccent = Brush.linearGradient(listOf(Color(0xFF11998E), Accent))

@Composable
fun ZsScreen(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    topBarEnd: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgMain)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(BgCardElevated)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("←", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
            }
            Text(
                title,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            topBarEnd()
        }
        content()
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.White,
    icon: String? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(label, color = TextSecondary, fontSize = 11.sp)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Text(icon, fontSize = 15.sp)
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    value,
                    color = valueColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ActionGridTile(
    icon: String,
    label: String,
    modifier: Modifier = Modifier,
    tint: Color = Primary,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 20.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                label,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = Cyan,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun EmptyText(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = TextMuted,
        fontSize = 14.sp,
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        textAlign = TextAlign.Center
    )
}

@Composable
fun LoadingBar(show: Boolean, modifier: Modifier = Modifier) {
    if (show) {
        LinearProgressIndicator(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            color = Accent
        )
    }
}

@Composable
fun FullScreenLoader() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Accent)
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "OK",
    dismissLabel: String? = "Cancel",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = Color.White) },
        text = { Text(message, color = TextSecondary) },
        containerColor = BgCard,
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel, color = Accent) }
        },
        dismissButton = {
            if (dismissLabel != null) {
                TextButton(onClick = onDismiss) { Text(dismissLabel, color = TextMuted) }
            }
        }
    )
}

@Composable
fun SingleChoiceDialog(
    title: String,
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = Color.White) },
        containerColor = BgCard,
        text = {
            Column {
                options.forEachIndexed { i, opt ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(i) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (i == selected) "●" else "○",
                            color = if (i == selected) Accent else TextMuted,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(opt, color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = TextMuted) }
        }
    )
}

@Composable
fun OptionsDialog(
    title: String,
    options: List<String>,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = Color.White) },
        containerColor = BgCard,
        text = {
            Column {
                options.forEachIndexed { i, opt ->
                    Text(
                        opt,
                        color = Color.White,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(i) }
                            .padding(vertical = 12.dp)
                    )
                }
            }
        },
        confirmButton = {}
    )
}

/** Formatted list container with scroll + bottom padding for content screens. */
@Composable
fun ZsList(
    state: LazyListState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    LazyColumn(state = state, modifier = modifier.fillMaxWidth()) {
        item { content() }
        item { Spacer(Modifier.height(32.dp)) }
    }
}
