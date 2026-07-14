package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AttendanceRepository
import com.example.data.model.UserEntity
import com.example.ui.components.CustomButton
import com.example.ui.components.CustomTextField
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SlateMutedText
import com.example.viewmodel.AttendanceViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val workers by viewModel.workers.collectAsState()
    val allAttendance by viewModel.allAttendanceRecords.collectAsState()
    val selectedDate by viewModel.selectedAdminDate.collectAsState()
    val searchQuery by viewModel.adminSearchQuery.collectAsState()
    val selectedFilter by viewModel.adminSelectedFilter.collectAsState()
    val sortBy by viewModel.adminSortBy.collectAsState()

    var showAddWorkerDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val availableMonths = remember {
        val calendar = Calendar.getInstance()
        val format = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val list = mutableListOf<String>()
        for (i in 0 until 6) {
            list.add(format.format(calendar.time))
            calendar.add(Calendar.MONTH, -1)
        }
        list
    }
    var selectedExportMonth by remember { mutableStateOf(availableMonths.first()) }
    var showMonthDropdown by remember { mutableStateOf(false) }

    val filteredWorkers = remember(workers, allAttendance, selectedDate, searchQuery, selectedFilter, sortBy) {
        workers.filter { worker ->
            val matchesQuery = worker.name.contains(searchQuery, ignoreCase = true) ||
                    worker.username.contains(searchQuery, ignoreCase = true)
            val record = allAttendance.find { it.userId == worker.id && it.dateString == selectedDate }
            val isPresent = record?.status == "Present"
            val matchesFilter = when (selectedFilter) {
                "Present" -> isPresent
                "Absent" -> !isPresent
                else -> true
            }
            matchesQuery && matchesFilter
        }.let { list ->
            when (sortBy) {
                "Name A-Z" -> list.sortedBy { it.name.lowercase() }
                "Name Z-A" -> list.sortedByDescending { it.name.lowercase() }
                "Status" -> list.sortedBy { worker ->
                    val record = allAttendance.find { it.userId == worker.id && it.dateString == selectedDate }
                    if (record?.status == "Present") 0 else 1
                }
                else -> list
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "INSYNC ATTENDANCE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        )
                        Text(
                            text = "Admin Dashboard",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    // Send notifications to forgot workers trigger
                    IconButton(
                        onClick = { viewModel.sendRemindersToForgotWorkers() },
                        modifier = Modifier.testTag("send_reminders_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = "Send Reminder to Forgot Workers",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Logout
                    IconButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier.testTag("admin_logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ExitToApp,
                            contentDescription = "Log Out",
                            tint = CrimsonError
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddWorkerDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_worker_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add New Worker")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Date Controller Pane
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f)),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { changeDateOffset(viewModel, selectedDate, -1) }) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous Day", tint = MaterialTheme.colorScheme.primary)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showDatePicker = true }
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .testTag("admin_date_selector_click")
                    ) {
                        Text(
                            text = "ATTENDANCE DATE 📅",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formatStringDateForAdmin(selectedDate),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = "Choose Date",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    IconButton(onClick = { changeDateOffset(viewModel, selectedDate, 1) }) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = "Next Day", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Live Analytics Row
            val totalCount = workers.size
            val presentCount = workers.count { worker ->
                allAttendance.any { it.userId == worker.id && it.dateString == selectedDate && it.status == "Present" }
            }
            val absentCount = totalCount - presentCount
            val attendanceRate = if (totalCount > 0) (presentCount * 100) / totalCount else 0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Attendance Rate Card
                Card(
                    modifier = Modifier.weight(1.1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("ATTENDANCE RATE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$attendanceRate%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { attendanceRate.toFloat() / 100f },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                    }
                }

                // Present Card
                Card(
                    modifier = Modifier.weight(0.9f),
                    colors = CardDefaults.cardColors(containerColor = EmeraldSuccess.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("PRESENT", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess, letterSpacing = 0.5.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$presentCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("On Duty", fontSize = 9.sp, color = EmeraldSuccess.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
                    }
                }

                // Absent Card
                Card(
                    modifier = Modifier.weight(0.9f),
                    colors = CardDefaults.cardColors(containerColor = CrimsonError.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonError.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("ABSENT", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = CrimsonError, letterSpacing = 0.5.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$absentCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CrimsonError)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Off Duty", fontSize = 9.sp, color = CrimsonError.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Actions Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f)),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "QUICK TOOLS & BULK ACTIONS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Mark All Present
                        Button(
                            onClick = { viewModel.markAllWorkersPresent() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldSuccess.copy(alpha = 0.08f),
                                contentColor = EmeraldSuccess
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.15f)),
                            modifier = Modifier.weight(1f).height(40.dp).testTag("mark_all_present_button"),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("All Present", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Clear All Today
                        Button(
                            onClick = { viewModel.clearAllAttendanceForSelectedDate() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CrimsonError.copy(alpha = 0.08f),
                                contentColor = CrimsonError
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonError.copy(alpha = 0.15f)),
                            modifier = Modifier.weight(1f).height(40.dp).testTag("clear_all_button"),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Send Reminders Button
                        Button(
                            onClick = { viewModel.sendRemindersToForgotWorkers() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            modifier = Modifier.weight(1f).height(40.dp).testTag("bulk_reminders_button"),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Remind", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Monthly Attendance Reports Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f)),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "MONTHLY ATTENDANCE REPORTS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Generate and share attendance reports for all employees.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Month Selector Dropdown
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Box {
                        OutlinedButton(
                            onClick = { showMonthDropdown = true },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)),
                            modifier = Modifier.fillMaxWidth().testTag("export_month_selector_button"),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.DateRange,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = selectedExportMonth,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                Icon(
                                    Icons.Filled.ArrowDropDown,
                                    contentDescription = "Dropdown",
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = showMonthDropdown,
                            onDismissRequest = { showMonthDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            availableMonths.forEach { month ->
                                DropdownMenuItem(
                                    text = { Text(month, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        selectedExportMonth = month
                                        showMonthDropdown = false
                                    },
                                    modifier = Modifier.testTag("month_item_$month")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Export CSV
                        Button(
                            onClick = {
                                com.example.util.AttendanceExporter.exportMonthlyAttendance(
                                    context = context,
                                    monthYearString = selectedExportMonth,
                                    workers = workers,
                                    allAttendance = allAttendance,
                                    format = "CSV"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            modifier = Modifier.weight(1f).height(40.dp).testTag("export_csv_button"),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Export PDF
                        Button(
                            onClick = {
                                com.example.util.AttendanceExporter.exportMonthlyAttendance(
                                    context = context,
                                    monthYearString = selectedExportMonth,
                                    workers = workers,
                                    allAttendance = allAttendance,
                                    format = "PDF"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                                contentColor = MaterialTheme.colorScheme.secondary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                            modifier = Modifier.weight(1f).height(40.dp).testTag("export_pdf_button"),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            CustomTextField(
                value = searchQuery,
                onValueChange = { viewModel.setAdminSearchQuery(it) },
                label = "Search workers by name...",
                leadingIcon = Icons.Filled.Search,
                testTag = "admin_search_input"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
                
                listOf("All", "Present", "Absent").forEach { filter ->
                    val isSelected = selectedFilter == filter
                    Surface(
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier
                            .clickable { viewModel.setAdminSelectedFilter(filter) }
                            .testTag("filter_chip_$filter")
                    ) {
                        Text(
                            text = filter,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sort Dropdown Row
            var showSortDropdown by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sort:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
                
                Box {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier
                            .clickable { showSortDropdown = true }
                            .testTag("sort_selector_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = sortBy,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    DropdownMenu(
                        expanded = showSortDropdown,
                        onDismissRequest = { showSortDropdown = false }
                    ) {
                        listOf("Name A-Z", "Name Z-A", "Status").forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    viewModel.setAdminSortBy(option)
                                    showSortDropdown = false
                                },
                                modifier = Modifier.testTag("sort_item_$option")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Workers Status Overview Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.People,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Company Workers List",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Text(
                        text = "${filteredWorkers.size} shown",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredWorkers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotEmpty() || selectedFilter != "All") {
                            "No workers match your search or filter criteria."
                        } else {
                            "No workers registered yet.\nTap the + button to manually add one, or ask workers to register themselves!"
                        },
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredWorkers) { worker ->
                        // Find this worker's attendance on selected date
                        val record = allAttendance.find {
                            it.userId == worker.id && it.dateString == selectedDate
                        }
                        
                        AdminWorkerRow(
                            worker = worker,
                            attendanceStatus = record?.status,
                            onToggleAttendance = {
                                viewModel.toggleAttendanceAsAdmin(worker.id, selectedDate, record?.status)
                            },
                            onDeleteWorker = {
                                viewModel.deleteWorkerByAdmin(worker)
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Worker Dialog
    if (showAddWorkerDialog) {
        AddWorkerDialog(
            onDismiss = { showAddWorkerDialog = false },
            onConfirm = { name, username ->
                viewModel.addWorkerByAdmin(name, username)
                showAddWorkerDialog = false
            }
        )
    }

    if (showDatePicker) {
        AdminDatePickerDialog(
            initialDateString = selectedDate,
            onDateSelected = { newDateStr ->
                viewModel.setAdminDate(newDateStr)
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
fun AdminWorkerRow(
    worker: UserEntity,
    attendanceStatus: String?,
    onToggleAttendance: () -> Unit,
    onDeleteWorker: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = worker.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "@${worker.username}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Tapping this toggles Present / Absent status
                Surface(
                    color = if (attendanceStatus == "Present") EmeraldSuccess.copy(alpha = 0.12f) else CrimsonError.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (attendanceStatus == "Present") EmeraldSuccess.copy(alpha = 0.2f) else CrimsonError.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .clickable { onToggleAttendance() }
                        .testTag("toggle_status_${worker.username}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (attendanceStatus == "Present") EmeraldSuccess else CrimsonError)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = attendanceStatus ?: "Absent",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (attendanceStatus == "Present") EmeraldSuccess else CrimsonError
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Delete Worker action
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.testTag("delete_${worker.username}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Remove Worker",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Remove Worker", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to remove ${worker.name} from the company list? All their historic attendance logs will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteWorker()
                        showDeleteConfirm = false
                    },
                    modifier = Modifier.testTag("confirm_delete")
                ) {
                    Text("Remove", color = CrimsonError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AddWorkerDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Add New Worker",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                CustomTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Full Name",
                    leadingIcon = Icons.Filled.Person,
                    testTag = "dialog_name_input"
                )
                Spacer(modifier = Modifier.height(16.dp))
                CustomTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = "Username / Email",
                    leadingIcon = Icons.Filled.AlternateEmail,
                    testTag = "dialog_username_input"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Note: Worker's default password will be 'password123'. They can use it to log in immediately.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    lineHeight = 14.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && username.isNotBlank()) {
                        onConfirm(name, username)
                    }
                },
                enabled = name.isNotBlank() && username.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("dialog_submit")
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun changeDateOffset(viewModel: AttendanceViewModel, currentDateStr: String, offset: Int) {
    try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = parser.parse(currentDateStr) ?: return
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.DAY_OF_YEAR, offset)
        val newDateStr = parser.format(calendar.time)
        viewModel.setAdminDate(newDateStr)
    } catch (e: Exception) {
        // Fallback
    }
}

fun formatStringDateForAdmin(dateStr: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formatter = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault())
        val date = parser.parse(dateStr)
        if (date != null) {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            if (dateStr == todayStr) "Today, " + formatter.format(date) else formatter.format(date)
        } else dateStr
    } catch (e: Exception) {
        dateStr
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDatePickerDialog(
    initialDateString: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val parser = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val initialTimeInMillis = remember(initialDateString) {
        try {
            parser.parse(initialDateString)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialTimeInMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        // Correct for timezone offset to prevent picking previous day due to UTC conversion in some platforms
                        val calendar = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
                        calendar.timeInMillis = millis
                        val localCalendar = Calendar.getInstance()
                        localCalendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR))
                        localCalendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH))
                        localCalendar.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH))
                        
                        val selectedDateStr = parser.format(localCalendar.time)
                        onDateSelected(selectedDateStr)
                    }
                    onDismiss()
                }
            ) {
                Text("Select", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
