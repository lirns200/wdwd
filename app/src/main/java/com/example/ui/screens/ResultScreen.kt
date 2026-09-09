package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.JumpRecord
import com.example.ui.HighJumpViewModel
import com.example.ui.theme.AthleticBackground
import com.example.ui.theme.AthleticBorder
import com.example.ui.theme.AthleticSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GoldCrown
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.ShareHelper
import java.util.Locale

@Composable
fun ResultScreen(
    heightMeters: Float,
    flightTimeMs: Long,
    viewModel: HighJumpViewModel,
    onJumpAgain: () -> Unit,
    onGoToLeaderboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    var publishToGlobal by remember { mutableStateOf(true) } // Default ON as per spec 2.5
    var savedRecord by remember { mutableStateOf<JumpRecord?>(null) }

    val previousBest = currentUser?.bestJump ?: 0f
    val isNewRecord = heightMeters > previousBest

    // Save jump result once
    LaunchedEffect(heightMeters, flightTimeMs, publishToGlobal) {
        viewModel.saveJumpResult(
            heightMeters = heightMeters,
            flightTimeMs = flightTimeMs,
            videoUri = null,
            publishToGlobal = publishToGlobal
        ) { record ->
            savedRecord = record
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AthleticBackground)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // Trophy / Celebration Icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(GoldCrown, Color(0xFFB45309))
                        )
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFF0A0F1D),
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isNewRecord) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(GoldCrown)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "🎉 НОВЫЙ ЛИЧНЫЙ РЕКОРД! 🎉",
                        color = Color(0xFF0A0F1D),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Text(
                text = "РЕЗУЛЬТАТ ПРЫЖКА",
                color = ElectricCyan,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Giant Height Display
            Text(
                text = String.format(Locale.getDefault(), "%.2f м", heightMeters),
                color = Color.White,
                fontSize = 64.sp,
                fontWeight = FontWeight.Black
            )

            Text(
                text = "${(heightMeters * 100).toInt()} сантиметров",
                color = TextSecondary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Telemetry stats card
            Card(
                colors = CardDefaults.cardColors(containerColor = AthleticSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AthleticBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Время полета (t):", color = TextSecondary, fontSize = 14.sp)
                        Text("$flightTimeMs мс", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Формула расчета:", color = TextSecondary, fontSize = 14.sp)
                        Text("h = (g · t²) / 8", color = ElectricCyan, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Предыдущий рекорд:", color = TextSecondary, fontSize = 14.sp)
                        Text(
                            if (previousBest > 0f) String.format(Locale.getDefault(), "%.2f м", previousBest) else "—",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Option: "Опубликовать результат в общий рейтинг" (Default checked per spec)
            Card(
                colors = CardDefaults.cardColors(containerColor = AthleticSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AthleticBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Checkbox(
                        checked = publishToGlobal,
                        onCheckedChange = { publishToGlobal = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = NeonGreen,
                            checkmarkColor = Color(0xFF0A0F1D)
                        ),
                        modifier = Modifier.testTag("publish_checkbox")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Опубликовать результат в общий рейтинг",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Social Sharing Section (Section 2.5 in spec: Likee, Мой Мир / VK, 4kiz / Other)
            Text(
                text = "ПОДЕЛИТЬСЯ РЕЗУЛЬТАТОМ",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Share buttons row
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Likee
                Button(
                    onClick = {
                        val record = savedRecord ?: JumpRecord(
                            userId = currentUser?.uid ?: "",
                            userNickname = currentUser?.nickname ?: "Атлет",
                            jumpHeight = heightMeters,
                            flightTimeMs = flightTimeMs
                        )
                        ShareHelper.shareJumpResult(context, record, ShareHelper.ShareTarget.LIKEE)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2B54)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("share_likee_button")
                ) {
                    Text("Likee", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                // VK / Мой Мир
                Button(
                    onClick = {
                        val record = savedRecord ?: JumpRecord(
                            userId = currentUser?.uid ?: "",
                            userNickname = currentUser?.nickname ?: "Атлет",
                            jumpHeight = heightMeters,
                            flightTimeMs = flightTimeMs
                        )
                        ShareHelper.shareJumpResult(context, record, ShareHelper.ShareTarget.VK)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077FF)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("share_vk_button")
                ) {
                    Text("VK / Мир", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                // 4kiz / Другие
                Button(
                    onClick = {
                        val record = savedRecord ?: JumpRecord(
                            userId = currentUser?.uid ?: "",
                            userNickname = currentUser?.nickname ?: "Атлет",
                            jumpHeight = heightMeters,
                            flightTimeMs = flightTimeMs
                        )
                        ShareHelper.shareJumpResult(context, record, ShareHelper.ShareTarget.ALL)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("share_other_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("4kiz", color = ElectricCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Primary Navigation Buttons
            Button(
                onClick = onJumpAgain,
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("jump_again_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Replay, contentDescription = null, tint = Color(0xFF0A0F1D))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Прыгнуть еще раз", color = Color(0xFF0A0F1D), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onGoToLeaderboard,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("to_leaderboard_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Leaderboard, contentDescription = null, tint = ElectricCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("К общему рейтингу", color = ElectricCyan, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
