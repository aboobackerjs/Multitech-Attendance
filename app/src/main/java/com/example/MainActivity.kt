package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AttendanceViewModel
import com.example.viewmodel.Screen

class MainActivity : ComponentActivity() {
    private val viewModel: AttendanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainNavigation(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MainNavigation(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen = viewModel.currentScreen

    // Handle back button presses elegantly
    BackHandler(enabled = currentScreen != Screen.WorkerLogin && currentScreen != Screen.WorkerHome && currentScreen != Screen.AdminHome) {
        viewModel.handleBack()
    }

    // Single screen switching with beautiful slide transitions
    Box(modifier = modifier.fillMaxSize()) {
        when (currentScreen) {
            is Screen.WorkerLogin -> {
                WorkerLoginScreen(viewModel = viewModel)
            }
            is Screen.WorkerRegister -> {
                WorkerRegisterScreen(viewModel = viewModel)
            }
            is Screen.WorkerHome -> {
                WorkerHomeScreen(viewModel = viewModel)
            }
            is Screen.AdminLogin -> {
                AdminLoginScreen(viewModel = viewModel)
            }
            is Screen.AdminHome -> {
                AdminHomeScreen(viewModel = viewModel)
            }
        }
    }
}
