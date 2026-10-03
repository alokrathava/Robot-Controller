package com.alokrathava.robotcontroller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alokrathava.home.HomeScreen
import com.alokrathava.home.Sidebar
import com.alokrathava.home.SidebarNavItem
import com.alokrathava.manualcontrol.ManualControlScreen
import com.alokrathava.map.MapScreen
import com.alokrathava.navigation.NavigationScreen
import com.alokrathava.robotstatus.RobotStatusScreen
import com.alokrathava.settings.SettingsScreen
import com.alokrathava.theme.MonochromeTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import com.alokrathava.robotcontroller.ui.theme.RobotControllerTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            RobotControllerTheme(darkTheme = false) {
                MainAppScreen()
            }
        }
    }
}

@Composable
fun MainAppScreen() {
    var selectedNav by remember { mutableStateOf(SidebarNavItem.HOME) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MonochromeTheme.colors.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedNav) {
                SidebarNavItem.HOME -> {
                    HomeScreen(
                        viewModel = hiltViewModel(),
                        selectedNav = selectedNav,
                        onSidebarItemSelected = { selectedNav = it }
                    )
                }

                SidebarNavItem.NAVIGATION -> {
                    NavigationScreen(
                        viewModel = hiltViewModel(),
                        onSidebarItemSelected = { selectedNav = it }
                    )
                }

                SidebarNavItem.MANUAL_CONTROL -> {
                    ManualControlScreen(
                        viewModel = hiltViewModel(),
                        onSidebarItemSelected = { selectedNav = it }
                    )
                }

                SidebarNavItem.ROBOT_STATUS -> {
                    RobotStatusScreen(
                        viewModel = hiltViewModel(),
                        selectedNav = selectedNav,
                        onSidebarItemSelected = { selectedNav = it }
                    )
                }

                SidebarNavItem.MAPS -> {
                    MapScreen(
                        viewModel = hiltViewModel(),
                        selectedNav = selectedNav,
                        onSidebarItemSelected = { selectedNav = it }
                    )
                }

                SidebarNavItem.SETTINGS -> {
                    SettingsScreen(
                        viewModel = hiltViewModel(),
                        selectedNav = selectedNav,
                        onSidebarItemSelected = { selectedNav = it }
                    )
                }

                else -> {
                    GenericPlaceholderScreen(
                        selectedNav = selectedNav,
                        onSidebarItemSelected = { selectedNav = it }
                    )
                }
            }
        }
    }
}

@Composable
fun GenericPlaceholderScreen(
    selectedNav: SidebarNavItem,
    onSidebarItemSelected: (SidebarNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MonochromeTheme.colors
    val typography = MonochromeTheme.typography

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Sidebar(
            selectedItem = selectedNav,
            onItemSelected = onSidebarItemSelected,
            isConnected = true,
            connectionAddress = "192.168.1.108:8080"
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(
                    start = MonochromeTheme.spacing.space6,
                    top = MonochromeTheme.spacing.cardPadding,
                    end = MonochromeTheme.spacing.space6,
                    bottom = MonochromeTheme.spacing.cardPadding
                )
        ) {
            Text(
                text = selectedNav.title,
                style = typography.h2,
                color = colors.primaryText,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(MonochromeTheme.spacing.space1))
            Text(
                text = "${selectedNav.title} section",
                style = typography.bodySmall,
                color = colors.secondaryText
            )
        }
    }
}
