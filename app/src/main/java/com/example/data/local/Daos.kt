package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE uid = :uid")
    fun getUser(uid: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE uid = :uid")
    suspend fun getUserSync(uid: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(nickname) = LOWER(:nickname) LIMIT 1")
    suspend fun getUserByNickname(nickname: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("UPDATE users SET bestJump = :bestJump WHERE uid = :uid")
    suspend fun updateBestJump(uid: String, bestJump: Float)

    @Query("SELECT * FROM users ORDER BY bestJump DESC")
    fun getAllUsersSortedByBestJump(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUsers(users: List<UserEntity>)
}

@Dao
interface JumpRecordDao {
    @Query("SELECT * FROM jump_records WHERE userId = :userId ORDER BY date DESC")
    fun getJumpsForUser(userId: String): Flow<List<JumpRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJump(record: JumpRecordEntity): Long

    @Query("SELECT * FROM jump_records ORDER BY date DESC")
    fun getAllJumps(): Flow<List<JumpRecordEntity>>

    @Query("SELECT * FROM jump_records WHERE isUploaded = 0")
    suspend fun getPendingUploadJumps(): List<JumpRecordEntity>

    @Query("UPDATE jump_records SET isUploaded = :isUploaded WHERE id = :id")
    suspend fun updateUploadStatus(id: Long, isUploaded: Boolean)
}
