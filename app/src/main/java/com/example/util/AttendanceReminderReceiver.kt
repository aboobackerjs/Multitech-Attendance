package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AttendanceDatabase
import com.example.data.AttendanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class AttendanceReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            AttendanceNotificationHelper.scheduleDaily10AmAlarm(context)
            return
        }

        // Check if today is a working day (Monday to Friday)
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val isWorkingDay = dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY

        if (!isWorkingDay) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AttendanceDatabase.getDatabase(context)
                val repository = AttendanceRepository(db.userDao(), db.attendanceDao())
                val todayStr = AttendanceRepository.getTodayDateString()
                val workers = repository.getWorkersList()

                // Check if any registered worker has not marked attendance today
                var hasUnmarkedWorker = false
                for (worker in workers) {
                    val attendance = repository.getAttendanceForUserOnDate(worker.id, todayStr)
                    if (attendance == null) {
                        hasUnmarkedWorker = true
                        break
                    }
                }

                if (hasUnmarkedWorker) {
                    AttendanceNotificationHelper.showNotification(
                        context,
                        "Attendance Reminder",
                        "It is already 10:00 AM! Please don't forget to mark your attendance today."
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
