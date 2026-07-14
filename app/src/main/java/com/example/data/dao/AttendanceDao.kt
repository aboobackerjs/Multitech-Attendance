package com.example.data.dao

import androidx.room.*
import com.example.data.model.AttendanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceEntity): Long

    @Update
    suspend fun updateAttendance(attendance: AttendanceEntity)

    @Query("SELECT * FROM attendance_records WHERE userId = :userId ORDER BY dateString DESC")
    fun getAttendanceForUser(userId: Int): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance_records WHERE userId = :userId AND dateString = :dateString LIMIT 1")
    suspend fun getAttendanceForUserOnDate(userId: Int, dateString: String): AttendanceEntity?

    @Query("SELECT * FROM attendance_records WHERE dateString = :dateString")
    fun getAttendanceOnDate(dateString: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance_records ORDER BY dateString DESC")
    fun getAllAttendance(): Flow<List<AttendanceEntity>>

    @Delete
    suspend fun deleteAttendance(attendance: AttendanceEntity)

    @Query("DELETE FROM attendance_records WHERE userId = :userId AND dateString = :dateString")
    suspend fun deleteAttendanceForUserOnDate(userId: Int, dateString: String)
}
