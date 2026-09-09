package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.JumpRecordEntity
import com.example.data.local.UserEntity
import com.example.data.local.UserPreferences
import com.example.model.JumpRecord
import com.example.model.LeaderboardEntry
import com.example.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class HighJumpRepository(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val userDao = database.userDao()
    private val jumpDao = database.jumpRecordDao()
    private val prefs = UserPreferences(context)

    private val _currentUserIdFlow = MutableStateFlow(prefs.currentUserId)

    init {
        // Seed default leaderboard champions if first run
        CoroutineScope(Dispatchers.IO).launch {
            seedInitialLeaderboardIfNeeded()
        }
    }

    val currentUserId: String?
        get() = _currentUserIdFlow.value

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getCurrentUserFlow(): Flow<User?> {
        return _currentUserIdFlow.flatMapLatest { uid ->
            if (uid.isNullOrBlank()) {
                flowOf(null)
            } else {
                userDao.getUser(uid).map { it?.toModel() }
            }
        }
    }

    suspend fun getCurrentUser(): User? = withContext(Dispatchers.IO) {
        val uid = _currentUserIdFlow.value ?: return@withContext null
        userDao.getUserSync(uid)?.toModel()
    }

    suspend fun isNicknameAvailable(nickname: String): Boolean = withContext(Dispatchers.IO) {
        if (nickname.length < 3) return@withContext false
        userDao.getUserByNickname(nickname.trim()) == null
    }

    suspend fun registerUser(
        email: String,
        nickname: String,
        age: Int,
        photoUrl: String
    ): Result<User> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val cleanNickname = nickname.trim()

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return@withContext Result.failure(IllegalArgumentException("Неверный формат Email"))
        }
        if (cleanNickname.length < 3) {
            return@withContext Result.failure(IllegalArgumentException("Никнейм должен содержать минимум 3 символа"))
        }
        if (age !in 14..99) {
            return@withContext Result.failure(IllegalArgumentException("Возраст должен быть от 14 до 99 лет"))
        }
        if (userDao.getUserByNickname(cleanNickname) != null) {
            return@withContext Result.failure(IllegalArgumentException("Никнейм '$cleanNickname' уже занят"))
        }
        if (userDao.getUserByEmail(cleanEmail) != null) {
            return@withContext Result.failure(IllegalArgumentException("Пользователь с таким Email уже зарегистрирован"))
        }

        val uid = UUID.randomUUID().toString()
        val newUser = User(
            uid = uid,
            email = cleanEmail,
            nickname = cleanNickname,
            age = age,
            photoUrl = photoUrl,
            bestJump = 0f,
            createdAt = System.currentTimeMillis()
        )

        userDao.insertUser(UserEntity.fromModel(newUser))
        prefs.currentUserId = uid
        _currentUserIdFlow.value = uid
        Result.success(newUser)
    }

    suspend fun loginUser(loginIdentifier: String, password: String = ""): Result<User> = withContext(Dispatchers.IO) {
        val cleanIdentifier = loginIdentifier.trim()
        if (cleanIdentifier.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Пожалуйста, введите Email или Никнейм"))
        }
        val cleanEmail = cleanIdentifier.lowercase()
        // Check by email first, then by nickname
        val entity = userDao.getUserByEmail(cleanEmail)
            ?: userDao.getUserByNickname(cleanIdentifier)

        if (entity != null) {
            val user = entity.toModel()
            prefs.currentUserId = user.uid
            _currentUserIdFlow.value = user.uid
            Result.success(user)
        } else {
            Result.failure(IllegalArgumentException("Пользователь «$cleanIdentifier» не найден. Зарегистрируйтесь на вкладке «Регистрация»."))
        }
    }

    suspend fun loginDemoUser(): Result<User> = withContext(Dispatchers.IO) {
        // Find top champion or first available user
        val topUser = userDao.getAllUsersSortedByBestJump().firstOrNull()?.firstOrNull()?.toModel()
        if (topUser != null) {
            prefs.currentUserId = topUser.uid
            _currentUserIdFlow.value = topUser.uid
            Result.success(topUser)
        } else {
            // Seed and register
            val demoUser = User("demo-athlete-1", "sokol@jump.com", "Сокол_Алекс", 22, "", 1.12f, System.currentTimeMillis())
            userDao.insertUser(UserEntity.fromModel(demoUser))
            prefs.currentUserId = demoUser.uid
            _currentUserIdFlow.value = demoUser.uid
            Result.success(demoUser)
        }
    }

    fun logout() {
        prefs.clearSession()
        _currentUserIdFlow.value = null
    }

    fun getLeaderboardFlow(): Flow<List<LeaderboardEntry>> {
        return combine(
            userDao.getAllUsersSortedByBestJump(),
            _currentUserIdFlow
        ) { userList, currentUid ->
            userList.mapIndexed { index, entity ->
                LeaderboardEntry(
                    rank = index + 1,
                    userId = entity.uid,
                    nickname = entity.nickname,
                    age = entity.age,
                    photoUrl = entity.photoUrl,
                    bestJump = entity.bestJump,
                    date = entity.createdAt,
                    isCurrentUser = entity.uid == currentUid
                )
            }
        }.distinctUntilChanged()
    }

    fun getUserJumpsFlow(userId: String): Flow<List<JumpRecord>> {
        return jumpDao.getJumpsForUser(userId).map { list ->
            list.map { it.toModel() }
        }
    }

    suspend fun saveJumpResult(
        heightMeters: Float,
        flightTimeMs: Long,
        videoUri: String?,
        publishToGlobal: Boolean = true
    ): Result<JumpRecord> = withContext(Dispatchers.IO) {
        val currentUser = getCurrentUser()
            ?: return@withContext Result.failure(IllegalStateException("Пользователь не авторизован"))

        val record = JumpRecord(
            userId = currentUser.uid,
            userNickname = currentUser.nickname,
            userPhotoUrl = currentUser.photoUrl,
            jumpHeight = heightMeters,
            flightTimeMs = flightTimeMs,
            date = System.currentTimeMillis(),
            isUploaded = publishToGlobal,
            videoUri = videoUri
        )

        val insertedId = jumpDao.insertJump(JumpRecordEntity.fromModel(record))
        val savedRecord = record.copy(id = insertedId)

        // If publishToGlobal and height is higher than previous best, update user best
        if (publishToGlobal && heightMeters > currentUser.bestJump) {
            userDao.updateBestJump(currentUser.uid, heightMeters)
        }

        Result.success(savedRecord)
    }

    private suspend fun seedInitialLeaderboardIfNeeded() {
        val count = userDao.getAllUsersSortedByBestJump().firstOrNull()?.size ?: 0
        if (count < 15) {
            val sampleChampions = listOf(
                User("champ-1", "sokol@jump.com", "Сокол_Алекс", 22, "", 1.12f, System.currentTimeMillis() - 86400000L * 5),
                User("champ-2", "mikhail@jump.com", "Михаил_Dunk", 24, "", 1.05f, System.currentTimeMillis() - 86400000L * 4),
                User("champ-3", "elena@jump.com", "Елена_Взлёт", 20, "", 0.98f, System.currentTimeMillis() - 86400000L * 3),
                User("champ-4", "dmitry@jump.com", "Дмитрий_Sky", 26, "", 0.94f, System.currentTimeMillis() - 86400000L * 3),
                User("champ-5", "artem@jump.com", "Артём_Rocket", 19, "", 0.91f, System.currentTimeMillis() - 86400000L * 2),
                User("champ-6", "anna@jump.com", "Анна_Air", 21, "", 0.88f, System.currentTimeMillis() - 86400000L * 2),
                User("champ-7", "vlad@jump.com", "Влад_Gravity", 25, "", 0.85f, System.currentTimeMillis() - 86400000L * 2),
                User("champ-8", "sergey@jump.com", "Сергей_Прыгун", 23, "", 0.82f, System.currentTimeMillis() - 86400000L * 1),
                User("champ-9", "polina@jump.com", "Полина_Fly", 18, "", 0.79f, System.currentTimeMillis() - 86400000L * 1),
                User("champ-10", "ivan@jump.com", "Иван_Вертикаль", 27, "", 0.77f, System.currentTimeMillis() - 86400000L * 1),
                User("champ-11", "kirill@jump.com", "Кирилл_Peak", 22, "", 0.74f, System.currentTimeMillis() - 3600000L * 12),
                User("champ-12", "olga@jump.com", "Ольга_Jump", 24, "", 0.71f, System.currentTimeMillis() - 3600000L * 8),
                User("champ-13", "roman@jump.com", "Роман_Spring", 20, "", 0.68f, System.currentTimeMillis() - 3600000L * 5),
                User("champ-14", "katya@jump.com", "Катя_Bounce", 19, "", 0.65f, System.currentTimeMillis() - 3600000L * 2),
                User("champ-15", "egor@jump.com", "Егор_Flight", 28, "", 0.62f, System.currentTimeMillis() - 3600000L * 1),
                User("champ-16", "alisa@jump.com", "Алиса_Orbit", 21, "", 0.58f, System.currentTimeMillis() - 1800000L),
                User("champ-17", "gleb@jump.com", "Глеб_Leap", 23, "", 0.55f, System.currentTimeMillis() - 900000L),
                User("champ-18", "daria@jump.com", "Дарья_Skyline", 22, "", 0.52f, System.currentTimeMillis() - 300000L)
            )
            userDao.insertUsers(sampleChampions.map { UserEntity.fromModel(it) })
        }
    }
}
