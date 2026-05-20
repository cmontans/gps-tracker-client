package com.tracker.gps.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tracker.gps.ui.screens.DashboardScreen
import com.tracker.gps.ui.screens.HistoryScreen
import com.tracker.gps.ui.theme.JumpTrackerTheme
import com.tracker.gps.ui.viewmodel.JumpViewModel

class JumpTrackerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JumpTrackerTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: JumpViewModel = viewModel()) {
    var currentTab by remember { mutableIntStateOf(0) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    DisposableEffect(Unit) {
        viewModel.bindService(context)
        onDispose {
            viewModel.unbindService(context)
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") }
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.List, contentDescription = "History") },
                    label = { Text("History") }
                )
            }
        }
    ) { innerPadding ->
        Surface(modifier = Modifier.padding(innerPadding)) {
            when (currentTab) {
                0 -> DashboardScreen(viewModel)
                1 -> HistoryScreen(viewModel)
            }
        }
    }
}
