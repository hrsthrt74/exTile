package com.hrsthrt74.qstile

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hrsthrt74.qstile.ui.screens.HomeScreen
import com.hrsthrt74.qstile.ui.screens.SettingsScreen
import com.hrsthrt74.qstile.ui.screens.TileConfigScreen
import com.hrsthrt74.qstile.ui.theme.ExTileTheme
import rikka.shizuku.Shizuku
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold

class MainActivity : ComponentActivity() {
    companion object {
        private const val REQUEST_CODE_SHIZUKU = 1001
    }

    private var shizukuPermissionGranted by mutableStateOf(false)
    private var onPermissionResult: ((Boolean) -> Unit)? = null

    private val shizukuPermissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        val granted = grantResult == PackageManager.PERMISSION_GRANTED
        shizukuPermissionGranted = granted
        onPermissionResult?.invoke(granted)
        if (granted) {
            Toast.makeText(this, "Shizuku 权限已授予", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Shizuku 权限被拒绝", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)

        setContent {
            ExTileTheme {
                MainApp(
                    onRequestShizukuPermission = { callback ->
                        onPermissionResult = callback
                        Shizuku.requestPermission(REQUEST_CODE_SHIZUKU)
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Screen("home", "主页", Icons.Default.AllInclusive)
    data object Config : Screen("config", "编辑", Icons.Default.Edit)
    data object Settings : Screen("settings", "设置", Icons.Default.Settings)
}

@Composable
fun MainApp(
    onRequestShizukuPermission: ((Boolean) -> Unit) -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val screens = listOf(Screen.Home, Screen.Config, Screen.Settings)

    MiuixScaffold(
        bottomBar = {
            NavigationBar {
                screens.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.Home.route) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = screen.icon,
                        label = screen.title
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onRequestShizukuPermission = onRequestShizukuPermission
                )
            }
            composable(Screen.Config.route) {
                TileConfigScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
