package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AttendanceDatabase
import com.example.data.AttendanceRepository
import com.example.data.model.AttendanceEntity
import com.example.data.model.UserEntity
import com.example.util.AttendanceNotificationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed class Screen {
    object WorkerLogin : Screen()
    object WorkerRegister : Screen()
    object WorkerHome : Screen()
    object AdminLogin : Screen()
    object AdminHome : Screen()
}

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AttendanceDatabase.getDatabase(application)
    private val repository = AttendanceRepository(database.userDao(), database.attendanceDao())

    // Backstack for simple, robust navigation
    private val _screenBackStack = mutableStateListOf<Screen>()
    val currentScreen: Screen get() = _screenBackStack.lastOrNull() ?: Screen.WorkerLogin

    // Session states
    private val sharedPrefs = application.getSharedPreferences("attendance_prefs", Context.MODE_PRIVATE)
    
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _isAdminLoggedIn = MutableStateFlow(false)
    val isAdminLoggedIn = _isAdminLoggedIn.asStateFlow()

    // Screen-specific state
    private val _loginError = MutableStateFlow<String?>(null)
    val loginError = _loginError.asStateFlow()

    private val _registerError = MutableStateFlow<String?>(null)
    val registerError = _registerError.asStateFlow()

    private val _registerSuccess = MutableStateFlow(false)
    val registerSuccess = _registerSuccess.asStateFlow()

    // Selected date for admin dashboard (YYYY-MM-DD)
    private val _selectedAdminDate = MutableStateFlow(AttendanceRepository.getTodayDateString())
    val selectedAdminDate = _selectedAdminDate.asStateFlow()

    // Admin state for searching and filtering workers
    private val _adminSearchQuery = MutableStateFlow("")
    val adminSearchQuery = _adminSearchQuery.asStateFlow()

    private val _adminSelectedFilter = MutableStateFlow("All") // "All", "Present", "Absent"
    val adminSelectedFilter = _adminSelectedFilter.asStateFlow()

    private val _adminSortBy = MutableStateFlow("Name A-Z") // "Name A-Z", "Name Z-A", "Status"
    val adminSortBy = _adminSortBy.asStateFlow()

    fun setAdminSearchQuery(query: String) {
        _adminSearchQuery.value = query
    }

    fun setAdminSelectedFilter(filter: String) {
        _adminSelectedFilter.value = filter
    }

    fun setAdminSortBy(sort: String) {
        _adminSortBy.value = sort
    }

    // Workers flow (role == "worker")
    val workers: StateFlow<List<UserEntity>> = repository.allWorkers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current worker's attendance history
    val currentWorkerAttendance: StateFlow<List<AttendanceEntity>> = _currentUser
        .filterNotNull()
        .flatMapLatest { user -> repository.getAttendanceForUser(user.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All attendance flow (for admin)
    val allAttendanceRecords: StateFlow<List<AttendanceEntity>> = repository.allAttendance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI state indicator for worker home (marked present today or not)
    val isMarkedToday: StateFlow<Boolean> = currentWorkerAttendance
        .map { records ->
            records.any { it.dateString == AttendanceRepository.getTodayDateString() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        // Initialize default admin account and notification channels
        viewModelScope.launch {
            repository.initializeDefaultAdmin()
            AttendanceNotificationHelper.createNotificationChannel(application)
            AttendanceNotificationHelper.scheduleDaily10AmAlarm(application)
            checkSavedSession()
        }
    }

    private fun checkSavedSession() {
        val loggedInUserId = sharedPrefs.getInt("logged_in_user_id", -1)
        val adminSession = sharedPrefs.getBoolean("admin_session", false)

        if (adminSession) {
            _isAdminLoggedIn.value = true
            navigateTo(Screen.AdminHome)
        } else if (loggedInUserId != -1) {
            viewModelScope.launch {
                val user = repository.getUserById(loggedInUserId)
                if (user != null && user.role == "worker") {
                    _currentUser.value = user
                    navigateTo(Screen.WorkerHome)
                    // Check if worker forgot to mark yesterday or today, send actual local reminder if they forgot yesterday!
                    checkAndTriggerForgoReminder(user.id)
                } else {
                    // Session invalid
                    clearSession()
                }
            }
        } else {
            navigateTo(Screen.WorkerLogin)
        }
    }

    private suspend fun checkAndTriggerForgoReminder(userId: Int) {
        // Check if marked today. If not, and it's later in the day (simulated or real), send prompt/notification.
        val todayStr = AttendanceRepository.getTodayDateString()
        val markedToday = repository.getAttendanceForUserOnDate(userId, todayStr) != null
        if (!markedToday) {
            // Post a system notification to remind them to mark attendance today!
            AttendanceNotificationHelper.showNotification(
                getApplication(),
                "Daily Attendance Reminder",
                "Hi! You haven't marked your attendance for today. Tap to mark it now."
            )
        }
    }

    fun navigateTo(screen: Screen) {
        if (_screenBackStack.contains(screen)) {
            // Pop up to the existing screen to avoid duplicate routes
            val index = _screenBackStack.indexOf(screen)
            while (_screenBackStack.size > index + 1) {
                _screenBackStack.removeAt(_screenBackStack.size - 1)
            }
        } else {
            _screenBackStack.add(screen)
        }
    }

    fun handleBack(): Boolean {
        return if (_screenBackStack.size > 1) {
            _screenBackStack.removeAt(_screenBackStack.size - 1)
            true
        } else {
            false
        }
    }

    // Login for Worker
    fun loginWorker(usernameInput: String, passwordInput: String) {
        if (usernameInput.isBlank() || passwordInput.isBlank()) {
            _loginError.value = "Please enter both username and password"
            return
        }

        viewModelScope.launch {
            val user = repository.getUserByUsername(usernameInput.trim())
            if (user != null && user.role == "worker" && user.password == passwordInput) {
                _currentUser.value = user
                _loginError.value = null
                
                // Save session
                sharedPrefs.edit()
                    .putInt("logged_in_user_id", user.id)
                    .putBoolean("admin_session", false)
                    .apply()

                navigateTo(Screen.WorkerHome)
            } else {
                _loginError.value = "Invalid username or password"
            }
        }
    }

    // Registration for Worker
    fun registerWorker(nameInput: String, usernameInput: String, passwordInput: String) {
        if (nameInput.isBlank() || usernameInput.isBlank() || passwordInput.isBlank()) {
            _registerError.value = "All fields are required"
            return
        }

        viewModelScope.launch {
            val existing = repository.getUserByUsername(usernameInput.trim())
            if (existing != null) {
                _registerError.value = "Username is already taken"
                return@launch
            }

            val newUser = UserEntity(
                name = nameInput.trim(),
                username = usernameInput.trim().lowercase(),
                password = passwordInput,
                role = "worker"
            )
            val result = repository.insertUser(newUser)
            if (result == -1L) {
                _registerError.value = "Username is already taken"
                return@launch
            }
            _registerError.value = null
            _registerSuccess.value = true
            
            // Navigate to login page
            navigateTo(Screen.WorkerLogin)
        }
    }

    fun clearRegisterSuccess() {
        _registerSuccess.value = false
    }

    fun clearErrors() {
        _loginError.value = null
        _registerError.value = null
    }

    // Login for Admin
    fun loginAdmin(usernameInput: String, passwordInput: String) {
        if (usernameInput.isBlank() || passwordInput.isBlank()) {
            _loginError.value = "Please enter admin credentials"
            return
        }

        viewModelScope.launch {
            val user = repository.getUserByUsername(usernameInput.trim())
            if (user != null && user.role == "admin" && user.password == passwordInput) {
                _isAdminLoggedIn.value = true
                _currentUser.value = user
                _loginError.value = null

                // Save session
                sharedPrefs.edit()
                    .putBoolean("admin_session", true)
                    .putInt("logged_in_user_id", user.id)
                    .apply()

                navigateTo(Screen.AdminHome)
            } else {
                _loginError.value = "Invalid admin credentials"
            }
        }
    }

    // Logout
    fun logout() {
        clearSession()
        navigateTo(Screen.WorkerLogin)
    }

    private fun clearSession() {
        _currentUser.value = null
        _isAdminLoggedIn.value = false
        sharedPrefs.edit()
            .remove("logged_in_user_id")
            .remove("admin_session")
            .apply()
    }

    // Mark today's attendance for the active worker
    fun markTodayAttendance() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val todayStr = AttendanceRepository.getTodayDateString()
            // Strictly check if they already have an existing attendance record for today
            val existing = repository.getAttendanceForUserOnDate(user.id, todayStr)
            if (existing == null) {
                repository.markAttendance(
                    userId = user.id,
                    dateString = todayStr,
                    status = "Present"
                )
            }
        }
    }

    // Toggle selected admin date
    fun setAdminDate(dateStr: String) {
        _selectedAdminDate.value = dateStr
    }

    // Toggle/Control attendance status for a worker as Admin
    fun toggleAttendanceAsAdmin(userId: Int, dateStr: String, currentStatus: String?) {
        viewModelScope.launch {
            if (currentStatus == "Present") {
                // If present, let's mark as Absent or delete record
                repository.removeAttendanceForUserOnDate(userId, dateStr)
            } else {
                // If absent/null, mark as Present
                repository.markAttendance(userId, dateStr, "Present")
            }
        }
    }

    // Admin action: Manually add a worker
    fun addWorkerByAdmin(name: String, username: String) {
        viewModelScope.launch {
            val existing = repository.getUserByUsername(username.trim())
            if (existing == null) {
                val newUser = UserEntity(
                    name = name.trim(),
                    username = username.trim().lowercase(),
                    password = "password123", // Default password for manually created workers
                    role = "worker"
                )
                repository.insertUser(newUser)
            }
        }
    }

    // Admin action: Manually delete a worker
    fun deleteWorkerByAdmin(worker: UserEntity) {
        viewModelScope.launch {
            repository.deleteUser(worker)
        }
    }

    // Trigger explicit manual reminder notifications from Admin to forgot-to-mark workers
    fun sendRemindersToForgotWorkers() {
        viewModelScope.launch {
            val activeWorkers = repository.getWorkersList()
            val todayStr = _selectedAdminDate.value
            var count = 0

            for (worker in activeWorkers) {
                val record = repository.getAttendanceForUserOnDate(worker.id, todayStr)
                if (record == null) {
                    count++
                }
            }

            if (count > 0) {
                AttendanceNotificationHelper.showNotification(
                    getApplication(),
                    "MTS Attendance Notice",
                    "$count workers have not marked attendance for $todayStr. Reminders have been sent!"
                )
            } else {
                AttendanceNotificationHelper.showNotification(
                    getApplication(),
                    "MTS Attendance Notice",
                    "All workers are fully marked present for $todayStr!"
                )
            }
        }
    }

    // Admin bulk action: Mark all current workers as Present for the selected date
    fun markAllWorkersPresent() {
        viewModelScope.launch {
            val activeWorkers = repository.getWorkersList()
            val dateStr = _selectedAdminDate.value
            for (worker in activeWorkers) {
                repository.markAttendance(worker.id, dateStr, "Present")
            }
        }
    }

    // Admin bulk action: Clear all attendance records for the selected date
    fun clearAllAttendanceForSelectedDate() {
        viewModelScope.launch {
            val activeWorkers = repository.getWorkersList()
            val dateStr = _selectedAdminDate.value
            for (worker in activeWorkers) {
                repository.removeAttendanceForUserOnDate(worker.id, dateStr)
            }
        }
    }
}
