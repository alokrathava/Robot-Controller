package me.alokrathava.robotcontroller

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
import com.agrathava.home.HomeScreen
import com.agrathava.home.Sidebar
import com.agrathava.home.SidebarNavItem
import com.agrathava.manualcontrol.ManualControlScreen
import com.agrathava.navigation.NavigationScreen
import com.agrathava.theme.MonochromeTheme
import dagger.hilt.android.AndroidEntryPoint
import me.alokrathava.robotcontroller.ui.theme.RobotControllerTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
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
                .padding(start = 28.dp, top = 24.dp, end = 28.dp, bottom = 24.dp)
        ) {
            Text(
                text = selectedNav.title,
                style = typography.h2.copy(
                    fontSize = 28.sp,
                    lineHeight = 34.sp
                ),
                color = colors.primaryText,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${selectedNav.title} section",
                style = typography.bodySmall.copy(
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                ),
                color = colors.secondaryText
            )
        }
    }
}
