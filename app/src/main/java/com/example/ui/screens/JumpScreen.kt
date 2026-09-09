package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.JumpRecord
import com.example.sensor.JumpState
import com.example.ui.HighJumpViewModel
import com.example.ui.components.CameraPreviewView
import com.example.ui.components.StickmanJumperView
import com.example.ui.theme.AthleticSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldCrown
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun JumpScreen(
    viewModel: HighJumpViewModel,
    onJumpFinished: (heightMeters: Float, flightTimeMs: Long) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val jumpState by viewModel.jumpState.collectAsState()
    val currentAcceleration by viewModel.currentAcceleration.collectAsState()

    DisposableEffect(Unit) {
        viewModel.startJumpMeasurement()
        onDispose {
            viewModel.resetJumpState()
        }
    }

    // React to landing
    LaunchedEffect(jumpState) {
        if (jumpState is JumpState.Landed) {
            val landed = jumpState as JumpState.Landed
            onJumpFinished(landed.heightMeters, landed.flightTimeMs)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Live Camera Viewfinder (Front Camera)
        CameraPreviewView(
            isRecording = jumpState is JumpState.ReadyToJump || jumpState is JumpState.InFlight
        )

        // Subtle dark gradient vignette for readable HUD elements
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xCC0A0F1D),
                            Color(0x220A0F1D),
                            Color(0x220A0F1D),
                            Color(0xEE0A0F1D)
                        )
                    )
                )
        )

        // 2. Overlay: Stickman with User's Face ("Человечек с твоим лицом")
        StickmanJumperView(
            jumpState = jumpState,
            avatarUrl = currentUser?.photoUrl,
            userNickname = currentUser?.nickname ?: "Атлет",
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 60.dp)
        )

        // 3. Top Action Bar: Close button & Acceleration Telemetry Gauge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 20.dp, end = 20.dp)
        ) {
            IconButton(
                onClick = onCancel,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0x88000000))
                    .testTag("jump_close_button")
            ) {
                Icon(Icons.Default.Close, contentDescription = "Отмена", tint = Color.White)
            }

            // Real-time Acceleration readout
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xAA131B2E)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Акселерометр: ",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f м/с²", currentAcceleration),
                        color = when {
                            currentAcceleration < 3.5f -> NeonGreen // Free fall
                            currentAcceleration > 15f -> GoldCrown // Impact
                            else -> TextPrimary
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 4. Center State HUD (Calibration Countdown, Instruction, Flight Timer)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .align(Alignment.TopCenter)
                .padding(top = 110.dp)
        ) {
            when (val state = jumpState) {
                is JumpState.Calibrating -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xCC0F172A)),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = "КАЛИБРОВКА СЕНСОРОВ",
                                color = ElectricCyan,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${state.secondsLeft}",
                                color = Color.White,
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // Mandatory instruction from spec section 4 & 5
                            Text(
                                text = "Держите телефон плотно в руке во время прыжка!",
                                color = GoldCrown,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                is JumpState.ReadyToJump -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xCC0F172A)),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "ГОТОВ К ПРЫЖКУ!",
                                color = NeonGreen,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Подпрыгните с телефоном в руке прямо сейчас!",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                is JumpState.InFlight -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xEE0F172A)),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(2.dp, GoldCrown),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "🚀 В ПОЛЕТЕ!",
                                color = GoldCrown,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${state.currentFlightMs} мс",
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Состояние невесомости зафиксировано",
                                color = NeonGreen,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                is JumpState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xEE450A0A)),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.message,
                                color = Color.White,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.startJumpMeasurement() },
                                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                                modifier = Modifier.testTag("retry_jump_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Попробовать снова")
                                }
                            }
                        }
                    }
                }

                else -> Unit
            }
        }

        // 5. Bottom Controls: Safe simulation helper (for desktop testing / emulators)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            // Testing simulator button so users in AI Studio preview / emulator can test jump mechanics
            Button(
                onClick = { viewModel.simulateJump(0.68f) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x881E293B)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("simulate_jump_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Science, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Тест-прыжок (Симуляция 68 см)",
                        color = ElectricCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
