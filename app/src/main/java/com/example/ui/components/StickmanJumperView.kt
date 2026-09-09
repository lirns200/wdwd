package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.sensor.JumpState
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GoldCrown
import com.example.ui.theme.NeonGreen
import kotlin.math.roundToInt

@Composable
fun StickmanJumperView(
    jumpState: JumpState,
    avatarUrl: String?,
    userNickname: String,
    modifier: Modifier = Modifier
) {
    // Jump vertical offset animation
    val targetJumpOffset = when (jumpState) {
        is JumpState.Idle -> 0f
        is JumpState.Calibrating -> 15f // slight squat down
        is JumpState.ReadyToJump -> 25f // coiled ready to spring
        is JumpState.InFlight -> -180f // high leap in the air!
        is JumpState.Landed -> -10f // cushioned landing
        is JumpState.Error -> 0f
    }

    val animatedOffset by animateFloatAsState(
        targetValue = targetJumpOffset,
        animationSpec = tween(
            durationMillis = if (jumpState is JumpState.InFlight) 320 else 250,
            easing = FastOutSlowInEasing
        ),
        label = "stickman_jump"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Stickman Container
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset { IntOffset(0, animatedOffset.roundToInt()) }
        ) {
            // Head: User's Face Avatar
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
                    .border(3.dp, if (jumpState is JumpState.InFlight) GoldCrown else ElectricCyan, CircleShape)
            ) {
                if (!avatarUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(avatarUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Лицо атлета",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = userNickname.take(1).uppercase(),
                        color = ElectricCyan,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Body Canvas (Arms, Torso, Legs)
            Canvas(modifier = Modifier.size(width = 120.dp, height = 110.dp)) {
                val cx = size.width / 2f
                val neckY = 0f
                val waistY = 48f
                val limbColor = if (jumpState is JumpState.InFlight) Color(0xFF00F0FF) else Color.White
                val strokeW = 8f

                // Torso
                drawLine(
                    color = limbColor,
                    start = Offset(cx, neckY),
                    end = Offset(cx, waistY),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )

                // Athletic Jersey / Number
                drawCircle(
                    color = Color(0xFF10B981),
                    radius = 8f,
                    center = Offset(cx, neckY + 24f)
                )

                // Arms based on state
                if (jumpState is JumpState.InFlight) {
                    // Arms extended triumphantly upwards
                    drawLine(
                        color = limbColor,
                        start = Offset(cx, neckY + 10f),
                        end = Offset(cx - 38f, neckY - 14f),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = limbColor,
                        start = Offset(cx, neckY + 10f),
                        end = Offset(cx + 38f, neckY - 14f),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                } else {
                    // Arms bent naturally / ready
                    drawLine(
                        color = limbColor,
                        start = Offset(cx, neckY + 10f),
                        end = Offset(cx - 30f, neckY + 28f),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = limbColor,
                        start = Offset(cx, neckY + 10f),
                        end = Offset(cx + 30f, neckY + 28f),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }

                // Legs based on state
                if (jumpState is JumpState.InFlight) {
                    // Legs bent dynamic mid-air tuck
                    drawLine(
                        color = limbColor,
                        start = Offset(cx, waistY),
                        end = Offset(cx - 24f, waistY + 36f),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = limbColor,
                        start = Offset(cx - 24f, waistY + 36f),
                        end = Offset(cx - 38f, waistY + 24f),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        color = limbColor,
                        start = Offset(cx, waistY),
                        end = Offset(cx + 24f, waistY + 36f),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = limbColor,
                        start = Offset(cx + 24f, waistY + 36f),
                        end = Offset(cx + 38f, waistY + 24f),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                } else {
                    // Standing / ready knees bent
                    drawLine(
                        color = limbColor,
                        start = Offset(cx, waistY),
                        end = Offset(cx - 22f, size.height),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = limbColor,
                        start = Offset(cx, waistY),
                        end = Offset(cx + 22f, size.height),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Shadow on ground
            Canvas(modifier = Modifier.size(width = 80.dp, height = 12.dp)) {
                val shadowAlpha = if (jumpState is JumpState.InFlight) 0.15f else 0.45f
                val scale = if (jumpState is JumpState.InFlight) 0.5f else 1.0f
                drawOval(
                    color = Color.Black.copy(alpha = shadowAlpha),
                    topLeft = Offset(size.width * (1f - scale) / 2f, 0f),
                    size = androidx.compose.ui.geometry.Size(size.width * scale, size.height)
                )
            }
        }
    }
}
