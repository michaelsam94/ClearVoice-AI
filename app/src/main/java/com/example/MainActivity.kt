package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.source.local.ClearVoicePrefs
import com.example.ui.screens.batch.BatchScreen
import com.example.ui.screens.batch.BatchViewModel
import com.example.ui.screens.home.ProcessScreen
import com.example.ui.screens.home.ProcessViewModel
import com.example.ui.screens.livemic.LiveMicScreen
import com.example.ui.screens.livemic.LiveMicViewModel
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.SettingsViewModel
import com.example.ui.theme.MyApplicationTheme

enum class NavigationScreen(val route: String, val titleRes: Int, val icon: ImageVector) {
    PROCESS("process", R.string.nav_process, Icons.Default.MusicNote),
    LIVE("live", R.string.nav_live, Icons.Default.Mic),
    BATCH("batch", R.string.nav_batch, Icons.Default.QueueMusic),
    SETTINGS("settings", R.string.nav_settings, Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefs = remember { ClearVoicePrefs(context) }
            
            var themeState by remember { mutableStateOf(prefs.themePreference) }
            
            val isDarkTheme = when (themeState) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars).testTag("bottom_navigation_bar")
                        ) {
                            NavigationScreen.values().forEach { screen ->
                                val selected = currentRoute == screen.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        if (currentRoute != screen.route) {
                                            navController.navigate(screen.route) {
                                                popUpTo(NavigationScreen.PROCESS.route) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = screen.icon,
                                            contentDescription = stringResource(screen.titleRes)
                                        )
                                    },
                                    label = {
                                        Text(text = stringResource(screen.titleRes))
                                    },
                                    modifier = Modifier.testTag("nav_item_${screen.route}")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = NavigationScreen.PROCESS.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(NavigationScreen.PROCESS.route) {
                            val processVm: ProcessViewModel = viewModel()
                            ProcessScreen(viewModel = processVm)
                        }
                        composable(NavigationScreen.LIVE.route) {
                            val liveMicVm: LiveMicViewModel = viewModel()
                            LiveMicScreen(viewModel = liveMicVm)
                        }
                        composable(NavigationScreen.BATCH.route) {
                            val batchVm: BatchViewModel = viewModel()
                            BatchScreen(viewModel = batchVm)
                        }
                        composable(NavigationScreen.SETTINGS.route) {
                            val settingsVm: SettingsViewModel = viewModel()
                            SettingsScreen(
                                viewModel = settingsVm,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            LaunchedEffect(settingsVm.themePref) {
                                settingsVm.themePref.collect { theme ->
                                    themeState = theme
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
