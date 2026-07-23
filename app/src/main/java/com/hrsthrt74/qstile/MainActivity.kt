package com.hrsthrt74.qstile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hrsthrt74.qstile.ui.screens.BackupScreen
import com.hrsthrt74.qstile.ui.screens.HomeScreen
import com.hrsthrt74.qstile.ui.screens.TileConfigScreen
import com.hrsthrt74.qstile.ui.theme.ExTileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExTileTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ExTileNavigation()
                }
            }
        }
    }
}

@Composable
fun ExTileNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                onNavigateToConfig = { navController.navigate("config") },
                onNavigateToBackup = { navController.navigate("backup") }
            )
        }
        composable("config") {
            TileConfigScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("backup") {
            BackupScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
