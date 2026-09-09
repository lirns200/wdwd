package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.JumpRecord
import com.example.model.LeaderboardEntry
import com.example.ui.HighJumpViewModel
import com.example.ui.theme.AthleticBackground
import com.example.ui.theme.AthleticBorder
import com.example.ui.theme.AthleticSurface
import com.example.ui.theme.AthleticSurfaceVariant
import com.example.ui.theme.BronzeBackground
import com.example.ui.theme.BronzeCrown
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GoldBackground
import com.example.ui.theme.GoldCrown
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.SilverBackground
import com.example.ui.theme.SilverCrown
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LeaderboardScreen(
    viewModel: HighJumpViewModel,
    onStartJump: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val leaderboardEntries by viewModel.leaderboardEntries.collectAsState()
    val totalCount by viewModel.totalLeaderboardCount.collectAsState()
    val displayedCount by viewModel.displayedCount.collectAsState()
    val myJumps by viewModel.myJumps.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Топ чемпионов, 1: Мои прыжки (Room)

    val currentUserRank = leaderboardEntries.find { it.isCurrentUser }?.rank
        ?: leaderboardEntries.indexOfFirst { it.userId == currentUser?.uid }.let { if (it >= 0) it + 1 else null }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AthleticBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(ElectricCyan)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsGymnastics,
                            contentDescription = null,
                            tint = Color(0xFF0A0F1D),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "HighJump Challenge",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Мировой рейтинг прыгунов",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                IconButton(
                    onClick = {
                        viewModel.logout()
                        onLogout()
                    },
                    modifier = Modifier.testTag("logout_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Выйти из аккаунта",
                        tint = TextMuted
                    )
                }
            }

            // Tabs: Топ чемпионов / Мои прыжки
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = AthleticSurface,
                contentColor = ElectricCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = ElectricCyan
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, AthleticBorder, RoundedCornerShape(10.dp))
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Топ чемпионов", fontSize = 13.sp)
                        }
                    }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("История прыжков", fontSize = 13.sp)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Content List
            if (activeTab == 0) {
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("leaderboard_list")
                ) {
                    itemsIndexed(leaderboardEntries) { index, entry ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically { it / 2 }
                        ) {
                            LeaderboardCard(entry = entry)
                        }
                    }

                    if (displayedCount < totalCount) {
                        item {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.showMoreLeaderboard() },
                                    colors = ButtonDefaults.buttonColors(containerColor = AthleticSurfaceVariant),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("show_more_button")
                                ) {
                                    Text(
                                        text = "Показать еще (+15)",
                                        color = ElectricCyan,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // My Jumps History from Room
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("my_history_list")
                ) {
                    if (myJumps.isEmpty()) {
                        item {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("История прыжков пуста", color = TextSecondary, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Нажмите большую кнопку 'ПРЫГНУТЬ', чтобы сделать первый прыжок!",
                                        color = TextMuted,
                                        fontSize = 13.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        itemsIndexed(myJumps) { _, jump ->
                            JumpHistoryCard(jump = jump)
                        }
                    }
                }
            }
        }

        // Sticky Bottom Panel: User personal status + Big JUMP Button
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color(0xEE0A0F1D),
                            Color(0xFF0A0F1D)
                        )
                    )
                )
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp, top = 16.dp)
        ) {
            // User Rank Status Bar
            Card(
                colors = CardDefaults.cardColors(containerColor = AthleticSurface),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                        ) {
                            Text(
                                text = "#${currentUserRank ?: "-"}",
                                color = GoldCrown,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = currentUser?.nickname ?: "Вы",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Личный статус",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if ((currentUser?.bestJump ?: 0f) > 0f) {
                                String.format(Locale.getDefault(), "%.2f м", currentUser?.bestJump)
                            } else {
                                "Нет прыжков"
                            },
                            color = ElectricCyan,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Личный рекорд",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Big Prominent "ПРЫГНУТЬ" Button (Required by section 2.3)
            Button(
                onClick = onStartJump,
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .testTag("jump_action_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFF0A0F1D),
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ПРЫГНУТЬ",
                        color = Color(0xFF0A0F1D),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }
            }
        }
    }
}

@Composable
fun LeaderboardCard(entry: LeaderboardEntry) {
    val rankBadgeColor = when (entry.rank) {
        1 -> GoldCrown
        2 -> SilverCrown
        3 -> BronzeCrown
        else -> TextMuted
    }

    val rankCardBackground = when (entry.rank) {
        1 -> GoldBackground
        2 -> SilverBackground
        3 -> BronzeBackground
        else -> if (entry.isCurrentUser) AthleticSurfaceVariant else AthleticSurface
    }

    val borderStroke = if (entry.isCurrentUser) {
        androidx.compose.foundation.BorderStroke(1.5.dp, ElectricCyan)
    } else if (entry.rank <= 3) {
        androidx.compose.foundation.BorderStroke(1.dp, rankBadgeColor.copy(alpha = 0.6f))
    } else {
        androidx.compose.foundation.BorderStroke(1.dp, AthleticBorder)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = rankCardBackground),
        shape = RoundedCornerShape(14.dp),
        border = borderStroke,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // Rank Number or Crown
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        when (entry.rank) {
                            1 -> Color(0x33F59E0B)
                            2 -> Color(0x33CBD5E1)
                            3 -> Color(0x33FB923C)
                            else -> Color(0x221E293B)
                        }
                    )
            ) {
                if (entry.rank <= 3) {
                    Text(
                        text = "👑",
                        fontSize = 18.sp
                    )
                } else {
                    Text(
                        text = "${entry.rank}",
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Avatar
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
                    .border(1.5.dp, rankBadgeColor, CircleShape)
            ) {
                if (entry.photoUrl.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(entry.photoUrl)
                            .crossfade(true)
                            .size(200, 200)
                            .build(),
                        contentDescription = "Аватар ${entry.nickname}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = entry.nickname.take(1).uppercase(),
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Athlete Nickname & Age
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.nickname,
                        color = if (entry.isCurrentUser) ElectricCyan else TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    if (entry.isCurrentUser) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(ElectricCyan)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("Вы", color = Color(0xFF0A0F1D), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text(
                    text = "${entry.age} лет",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }

            // Jump Height (in meters and cm)
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format(Locale.getDefault(), "%.2f м", entry.bestJump),
                    color = if (entry.rank <= 3) rankBadgeColor else ElectricCyan,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "${(entry.bestJump * 100).toInt()} см",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun JumpHistoryCard(jump: JumpRecord) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AthleticSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AthleticBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(14.dp)
        ) {
            Column {
                Text(
                    text = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("ru")).format(Date(jump.date)),
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Полет: ${jump.flightTimeMs} мс",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (jump.isUploaded) Color(0x3310B981) else Color(0x33F59E0B))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (jump.isUploaded) "Синхронизировано" else "Локально",
                            color = if (jump.isUploaded) NeonGreen else GoldCrown,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format(Locale.getDefault(), "%.2f м", jump.jumpHeight),
                    color = ElectricCyan,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
                Text(
                    text = "${(jump.jumpHeight * 100).toInt()} см",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}
