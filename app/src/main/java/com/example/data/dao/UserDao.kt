package com.example.data.dao

import androidx.room.*
import com.example.data.model.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUser(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:username) LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Int): UserEntity?

    @Query("SELECT * FROM users WHERE role = 'worker' ORDER BY name ASC")
    fun getWorkers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE role = 'worker' ORDER BY name ASC")
    suspend fun getWorkersList(): List<UserEntity>

    @Delete
    suspend fun deleteUser(user: UserEntity)
}
