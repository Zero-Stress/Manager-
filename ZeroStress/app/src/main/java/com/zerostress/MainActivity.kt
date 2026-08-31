package com.zerostress

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zerostress.ui.MainNavigation
import com.zerostress.ui.screens.auth.LoginScreen
import com.zerostress.ui.screens.auth.RegisterScreen
import com.zerostress.ui.theme.ZeroStressTheme
import com.zerostress.viewmodel.AppViewModel
import com.zerostress.viewmodel.AuthViewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val authViewModel: AuthViewModel = viewModel()
            val appViewModel: AppViewModel = viewModel()
            val authState by authViewModel.uiState.collectAsState()
            val isDark = isSystemInDarkTheme()
            var showRegister by remember { mutableStateOf(false) }
            var toastMessage by remember { mutableStateOf<String?>(null) }

            // Toast collector
            LaunchedEffect(Unit) {
                appViewModel.toastMessage.collectLatest { msg ->
                    toastMessage = msg
                }
            }

            ZeroStressTheme(darkTheme = isDark) {
                when {
                    authState.isLoggedIn && authState.user != null -> {
                        MainNavigation(
                            currentUser = authState.user!!,
                            viewModel = appViewModel,
                            onLogout = { authViewModel.logout() }
                        )
                    }
                    showRegister -> {
                        RegisterScreen(
                            onRegister = { name, phone, password ->
                                authViewModel.register(name, phone, password)
                            },
                            onBack = { showRegister = false },
                            isLoading = authState.isLoading,
                            error = authState.error
                        )
                    }
                    else -> {
                        LoginScreen(
                            onLogin = { phone, password ->
                                authViewModel.login("+880$phone", password)
                            },
                            onNavigateToRegister = { showRegister = true },
                            isLoading = authState.isLoading,
                            error = authState.error
                        )
                    }
                }

                // Toast
                toastMessage?.let { msg ->
                    LaunchedEffect(msg) {
                        kotlinx.coroutines.delay(2000)
                        toastMessage = null
                    }
                }
            }
        }
    }
}
