package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CompanyHeader
import com.example.ui.components.CustomButton
import com.example.ui.components.CustomPasswordField
import com.example.ui.components.CustomTextField
import com.example.viewmodel.AttendanceViewModel
import com.example.viewmodel.Screen

@Composable
fun WorkerLoginScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    val loginError by viewModel.loginError.collectAsState()
    val registerSuccess by viewModel.registerSuccess.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            CompanyHeader(
                title = "Worker Attendance",
                subtitle = "Sign in to record your daily attendance"
            )
            
            Spacer(modifier = Modifier.height(40.dp))

            if (registerSuccess) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "Registration successful! Please sign in below.",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    )
                }
            }

            if (loginError != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = loginError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    )
                }
            }

            CustomTextField(
                value = username,
                onValueChange = { 
                    username = it
                    viewModel.clearErrors()
                },
                label = "Username / Email",
                leadingIcon = Icons.Filled.Person,
                testTag = "login_username_input"
            )

            Spacer(modifier = Modifier.height(16.dp))

            CustomPasswordField(
                value = password,
                onValueChange = { 
                    password = it
                    viewModel.clearErrors()
                },
                label = "Password",
                leadingIcon = Icons.Filled.Lock,
                testTag = "login_password_input"
            )

            Spacer(modifier = Modifier.height(32.dp))

            CustomButton(
                text = "Sign In",
                onClick = {
                    keyboardController?.hide()
                    viewModel.loginWorker(username, password)
                },
                testTag = "login_button"
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New worker? ",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Text(
                    text = "Register here",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable {
                            viewModel.clearErrors()
                            viewModel.clearRegisterSuccess()
                            viewModel.navigateTo(Screen.WorkerRegister)
                        }
                        .testTag("go_to_register")
                )
            }
            
            Spacer(modifier = Modifier.height(64.dp))
        }

        // Admin entry portal at the bottom (small and simple)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        ) {
            Surface(
                onClick = {
                    viewModel.clearErrors()
                    viewModel.clearRegisterSuccess()
                    viewModel.navigateTo(Screen.AdminLogin)
                },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .testTag("go_to_admin_login")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.SupervisorAccount,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Access Administrative Portal",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun WorkerRegisterScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    val registerError by viewModel.registerError.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        CompanyHeader(
            title = "Worker Registration",
            subtitle = "Create your professional account"
        )
        
        Spacer(modifier = Modifier.height(40.dp))

        if (registerError != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = registerError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                )
            }
        }

        CustomTextField(
            value = name,
            onValueChange = { 
                name = it
                viewModel.clearErrors()
            },
            label = "Full Name",
            leadingIcon = Icons.Filled.Person,
            testTag = "register_name_input"
        )

        Spacer(modifier = Modifier.height(16.dp))

        CustomTextField(
            value = username,
            onValueChange = { 
                username = it
                viewModel.clearErrors()
            },
            label = "Username / Email",
            leadingIcon = Icons.Filled.Person,
            testTag = "register_username_input"
        )

        Spacer(modifier = Modifier.height(16.dp))

        CustomPasswordField(
            value = password,
            onValueChange = { 
                password = it
                viewModel.clearErrors()
            },
            label = "Password",
            leadingIcon = Icons.Filled.Lock,
            testTag = "register_password_input"
        )

        Spacer(modifier = Modifier.height(32.dp))

        CustomButton(
            text = "Register Account",
            onClick = {
                keyboardController?.hide()
                viewModel.registerWorker(name, username, password)
            },
            testTag = "register_submit_button"
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Back to Sign In",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable {
                    viewModel.clearErrors()
                    viewModel.navigateTo(Screen.WorkerLogin)
                }
                .padding(8.dp)
                .testTag("back_to_login")
        )
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun AdminLoginScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    val loginError by viewModel.loginError.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        CompanyHeader(
            title = "Admin Console",
            subtitle = "Authorized administrative access only"
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        if (loginError != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = loginError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                )
            }
        }

        CustomTextField(
            value = username,
            onValueChange = { 
                username = it
                viewModel.clearErrors()
            },
            label = "Admin Username",
            leadingIcon = Icons.Filled.SupervisorAccount,
            testTag = "admin_username_input"
        )

        Spacer(modifier = Modifier.height(16.dp))

        CustomPasswordField(
            value = password,
            onValueChange = { 
                password = it
                viewModel.clearErrors()
            },
            label = "Admin Password",
            leadingIcon = Icons.Filled.Lock,
            testTag = "admin_password_input"
        )

        Spacer(modifier = Modifier.height(32.dp))

        CustomButton(
            text = "Sign In as Admin",
            onClick = {
                keyboardController?.hide()
                viewModel.loginAdmin(username, password)
            },
            containerColor = MaterialTheme.colorScheme.secondary,
            testTag = "admin_login_button"
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Back to Worker Portal",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable {
                    viewModel.clearErrors()
                    viewModel.navigateTo(Screen.WorkerLogin)
                }
                .padding(8.dp)
                .testTag("admin_back_to_worker")
        )
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}
