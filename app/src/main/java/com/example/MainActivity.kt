package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.screens.admin.AdminUnitScreen
import com.example.ui.screens.checklist.NewChecklistScreen
import com.example.ui.screens.checklist.ReportCompletedScreen
import com.example.ui.screens.history.SavedChecklistsScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    AppContent(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun AppContent(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    BackHandler(enabled = currentScreen != AppScreen.HOME) {
        viewModel.navigateTo(AppScreen.HOME)
    }

    when (currentScreen) {
        AppScreen.HOME -> {
            HomeScreen(
                viewModel = viewModel,
                onNavigate = { targetScreen ->
                    viewModel.navigateTo(targetScreen)
                }
            )
        }
        AppScreen.NEW_CHECKLIST -> {
            NewChecklistScreen(
                viewModel = viewModel,
                onNavigateBack = { viewModel.navigateTo(AppScreen.HOME) }
            )
        }
        AppScreen.REPORT_COMPLETED -> {
            ReportCompletedScreen(
                viewModel = viewModel,
                onNavigateHome = { viewModel.navigateTo(AppScreen.HOME) }
            )
        }
        AppScreen.ADMIN_UNITS -> {
            AdminUnitScreen(
                viewModel = viewModel,
                onNavigateBack = { viewModel.navigateTo(AppScreen.HOME) }
            )
        }
        AppScreen.SAVED_CHECKLISTS -> {
            SavedChecklistsScreen(
                viewModel = viewModel,
                onNavigateBack = { viewModel.navigateTo(AppScreen.HOME) }
            )
        }
    }
}

