package com.example.data

import com.example.data.dao.AttendanceDao
import com.example.data.dao.UserDao
import com.example.data.model.AttendanceEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AttendanceRepository(
    private val userDao: UserDao,
    private val attendanceDao: AttendanceDao
) {
    val allWorkers: Flow<List<UserEntity>> = userDao.getWorkers()
    val allAttendance: Flow<List<AttendanceEntity>> = attendanceDao.getAllAttendance()

    suspend fun insertUser(user: UserEntity): Long {
        return userDao.insertUser(user)
    }

    suspend fun getUserByUsername(username: String): UserEntity? {
        return userDao.getUserByUsername(username)
    }

    suspend fun getUserById(id: Int): UserEntity? {
        return userDao.getUserById(id)
    }

    suspend fun deleteUser(user: UserEntity) {
        userDao.deleteUser(user)
    }

    suspend fun getWorkersList(): List<UserEntity> {
        return userDao.getWorkersList()
    }

    // Attendance
    fun getAttendanceForUser(userId: Int): Flow<List<AttendanceEntity>> {
        return attendanceDao.getAttendanceForUser(userId)
    }

    suspend fun getAttendanceForUserOnDate(userId: Int, dateString: String): AttendanceEntity? {
        return attendanceDao.getAttendanceForUserOnDate(userId, dateString)
    }

    fun getAttendanceOnDate(dateString: String): Flow<List<AttendanceEntity>> {
        return attendanceDao.getAttendanceOnDate(dateString)
    }

    suspend fun markAttendance(userId: Int, dateString: String, status: String): Long {
        val existing = attendanceDao.getAttendanceForUserOnDate(userId, dateString)
        return if (existing != null) {
            attendanceDao.updateAttendance(existing.copy(status = status))
            existing.id.toLong()
        } else {
            val record = AttendanceEntity(
                userId = userId,
                dateString = dateString,
                status = status
            )
            attendanceDao.insertAttendance(record)
        }
    }

    suspend fun removeAttendanceForUserOnDate(userId: Int, dateString: String) {
        attendanceDao.deleteAttendanceForUserOnDate(userId, dateString)
    }

    // Initialize Default Admin account if it does not exist
    suspend fun initializeDefaultAdmin() {
        val existingAdmin = userDao.getUserByUsername("admin")
        if (existingAdmin == null) {
            val defaultAdmin = UserEntity(
                name = "Company Administrator",
                username = "admin",
                password = "admin123", // Default admin password
                role = "admin"
            )
            userDao.insertUser(defaultAdmin)
        }
    }

    companion object {
        fun getTodayDateString(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }
    }
}
