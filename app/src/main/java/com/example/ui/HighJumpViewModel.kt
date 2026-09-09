package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.HighJumpRepository
import com.example.model.JumpRecord
import com.example.model.LeaderboardEntry
import com.example.model.User
import com.example.sensor.JumpSensorDetector
import com.example.sensor.JumpState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HighJumpViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HighJumpRepository(application)
    private val detector = JumpSensorDetector(application, viewModelScope)

    val currentUser: StateFlow<User?> = repository.getCurrentUserFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val allLeaderboardEntries = repository.getLeaderboardFlow()

    private val _displayedCount = MutableStateFlow(15)
    val displayedCount = _displayedCount.asStateFlow()

    val leaderboardEntries: StateFlow<List<LeaderboardEntry>> = combine(
        allLeaderboardEntries,
        _displayedCount
    ) { all, count ->
        all.take(count)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalLeaderboardCount: StateFlow<Int> = allLeaderboardEntries
        .combine(_displayedCount) { all, _ -> all.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val userHistory: StateFlow<List<JumpRecord>> = repository.getCurrentUserFlow()
        .combine(repository.getLeaderboardFlow()) { user, _ ->
            if (user != null) {
                repository.getUserJumpsFlow(user.uid)
            } else {
                null
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
        .combine(allLeaderboardEntries) { _, _ -> emptyList<JumpRecord>() } // Initial fallback
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Direct jump history flow
    private val _myJumps = MutableStateFlow<List<JumpRecord>>(emptyList())
    val myJumps: StateFlow<List<JumpRecord>> = _myJumps.asStateFlow()

    val jumpState: StateFlow<JumpState> = detector.jumpState
    val currentAcceleration: StateFlow<Float> = detector.currentAcceleration

    private val _lastCompletedJump = MutableStateFlow<JumpRecord?>(null)
    val lastCompletedJump: StateFlow<JumpRecord?> = _lastCompletedJump.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _authSuccessMessage = MutableStateFlow<String?>(null)
    val authSuccessMessage: StateFlow<String?> = _authSuccessMessage.asStateFlow()

    private val _nicknameValidationState = MutableStateFlow<Boolean?>(null)
    val nicknameValidationState: StateFlow<Boolean?> = _nicknameValidationState.asStateFlow()

    init {
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null) {
                    repository.getUserJumpsFlow(user.uid).collect { jumps ->
                        _myJumps.value = jumps
                    }
                } else {
                    _myJumps.value = emptyList()
                }
            }
        }
    }

    fun showMoreLeaderboard() {
        _displayedCount.value += 15
    }

    fun checkNicknameAvailability(nickname: String) {
        viewModelScope.launch {
            if (nickname.length >= 3) {
                _nicknameValidationState.value = repository.isNicknameAvailable(nickname)
            } else {
                _nicknameValidationState.value = null
            }
        }
    }

    fun register(
        email: String,
        nickname: String,
        age: Int,
        photoUrl: String
    ) {
        viewModelScope.launch {
            _authError.value = null
            val result = repository.registerUser(email, nickname, age, photoUrl)
            result.onFailure { error ->
                _authError.value = error.message ?: "Ошибка регистрации"
            }
        }
    }

    fun login(identifier: String, password: String = "") {
        viewModelScope.launch {
            _authError.value = null
            val cleanIdentifier = identifier.trim()
            if (cleanIdentifier.isBlank()) {
                _authError.value = "Пожалуйста, введите Email или Никнейм"
                return@launch
            }
            val result = repository.loginUser(cleanIdentifier, password)
            result.onFailure { error ->
                _authError.value = error.message ?: "Ошибка входа"
            }
        }
    }

    fun loginAsDemo() {
        viewModelScope.launch {
            _authError.value = null
            val result = repository.loginDemoUser()
            result.onFailure { error ->
                _authError.value = error.message ?: "Не удалось войти как демо-атлет"
            }
        }
    }

    fun showAuthError(message: String) {
        _authError.value = message
    }

    fun sendPasswordReset(email: String) {
        _authSuccessMessage.value = "Инструкция по восстановлению пароля отправлена на $email"
    }

    fun clearAuthMessages() {
        _authError.value = null
        _authSuccessMessage.value = null
    }

    fun logout() {
        repository.logout()
    }

    fun startJumpMeasurement() {
        detector.startMeasurement()
    }

    fun simulateJump(height: Float = 0.65f) {
        detector.simulateJump(height)
    }

    fun resetJumpState() {
        detector.reset()
    }

    fun saveJumpResult(
        heightMeters: Float,
        flightTimeMs: Long,
        videoUri: String? = null,
        publishToGlobal: Boolean = true,
        onComplete: (JumpRecord) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.saveJumpResult(
                heightMeters = heightMeters,
                flightTimeMs = flightTimeMs,
                videoUri = videoUri,
                publishToGlobal = publishToGlobal
            )
            result.onSuccess { record ->
                _lastCompletedJump.value = record
                onComplete(record)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        detector.stopMeasurement()
    }
}
