package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.JumpRecord
import com.example.model.User

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val uid: String,
    val email: String,
    val nickname: String,
    val age: Int,
    val photoUrl: String,
    val bestJump: Float,
    val createdAt: Long
) {
    fun toModel(): User = User(
        uid = uid,
        email = email,
        nickname = nickname,
        age = age,
        photoUrl = photoUrl,
        bestJump = bestJump,
        createdAt = createdAt
    )

    companion object {
        fun fromModel(user: User): UserEntity = UserEntity(
            uid = user.uid,
            email = user.email,
            nickname = user.nickname,
            age = user.age,
            photoUrl = user.photoUrl,
            bestJump = user.bestJump,
            createdAt = user.createdAt
        )
    }
}

@Entity(tableName = "jump_records")
data class JumpRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val userNickname: String,
    val userPhotoUrl: String,
    val jumpHeight: Float,
    val flightTimeMs: Long,
    val date: Long,
    val isUploaded: Boolean,
    val videoUri: String?
) {
    fun toModel(): JumpRecord = JumpRecord(
        id = id,
        userId = userId,
        userNickname = userNickname,
        userPhotoUrl = userPhotoUrl,
        jumpHeight = jumpHeight,
        flightTimeMs = flightTimeMs,
        date = date,
        isUploaded = isUploaded,
        videoUri = videoUri
    )

    companion object {
        fun fromModel(record: JumpRecord): JumpRecordEntity = JumpRecordEntity(
            id = record.id,
            userId = record.userId,
            userNickname = record.userNickname,
            userPhotoUrl = record.userPhotoUrl,
            jumpHeight = record.jumpHeight,
            flightTimeMs = record.flightTimeMs,
            date = record.date,
            isUploaded = record.isUploaded,
            videoUri = record.videoUri
        )
    }
}
