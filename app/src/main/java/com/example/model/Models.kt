package com.example.model

data class User(
    val uid: String,
    val email: String,
    val nickname: String,
    val age: Int,
    val photoUrl: String = "",
    val bestJump: Float = 0f,
    val createdAt: Long = System.currentTimeMillis()
)

data class JumpRecord(
    val id: Long = 0,
    val userId: String,
    val userNickname: String,
    val userPhotoUrl: String = "",
    val jumpHeight: Float, // in meters (e.g. 0.85f)
    val flightTimeMs: Long, // delta_t in milliseconds
    val date: Long = System.currentTimeMillis(),
    val isUploaded: Boolean = true,
    val videoUri: String? = null
)

data class LeaderboardEntry(
    val rank: Int,
    val userId: String,
    val nickname: String,
    val age: Int,
    val photoUrl: String,
    val bestJump: Float,
    val date: Long = System.currentTimeMillis(),
    val isCurrentUser: Boolean = false
)
