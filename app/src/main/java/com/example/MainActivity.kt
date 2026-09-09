package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.HighJumpViewModel
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.JumpScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.ResultScreen
import com.example.ui.theme.MyApplicationTheme

enum class AppScreen {
    AUTH,
    LEADERBOARD,
    JUMP,
    RESULT
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: HighJumpViewModel = viewModel()
                val currentUser by viewModel.currentUser.collectAsState()

                var currentScreen by remember { mutableStateOf(AppScreen.LEADERBOARD) }
                var lastJumpHeight by remember { mutableFloatStateOf(0f) }
                var lastFlightTimeMs by remember { mutableLongStateOf(0L) }

                val activeScreen = if (currentUser == null) AppScreen.AUTH else currentScreen

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AnimatedContent(
                        targetState = activeScreen,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "screen_transition"
                    ) { target ->
                        when (target) {
                            AppScreen.AUTH -> {
                                AuthScreen(
                                    viewModel = viewModel,
                                    onAuthSuccess = { currentScreen = AppScreen.LEADERBOARD },
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                            AppScreen.LEADERBOARD -> {
                                LeaderboardScreen(
                                    viewModel = viewModel,
                                    onStartJump = { currentScreen = AppScreen.JUMP },
                                    onLogout = { currentScreen = AppScreen.AUTH },
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                            AppScreen.JUMP -> {
                                JumpScreen(
                                    viewModel = viewModel,
                                    onJumpFinished = { height, flightMs ->
                                        lastJumpHeight = height
                                        lastFlightTimeMs = flightMs
                                        currentScreen = AppScreen.RESULT
                                    },
                                    onCancel = { currentScreen = AppScreen.LEADERBOARD },
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                            AppScreen.RESULT -> {
                                ResultScreen(
                                    heightMeters = lastJumpHeight,
                                    flightTimeMs = lastFlightTimeMs,
                                    viewModel = viewModel,
                                    onJumpAgain = { currentScreen = AppScreen.JUMP },
                                    onGoToLeaderboard = { currentScreen = AppScreen.LEADERBOARD },
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "HighJump $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme { Greeting("Athlete") }
}

