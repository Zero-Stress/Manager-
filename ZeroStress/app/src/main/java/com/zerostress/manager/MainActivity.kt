package com.zerostress.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zerostress.manager.data.PreferenceManager
import com.zerostress.manager.navigation.AppNavigation
import com.zerostress.manager.ui.theme.ZeroStressTheme
import com.zerostress.manager.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = PreferenceManager(applicationContext)
        setContent {
            val isDark by prefs.isDarkTheme.collectAsState(initial = true)
            ZeroStressTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(prefs = prefs)
                }
            }
        }
    }
}
