package me.alokrathava.robotcontroller

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.agrathava.home.HomeScreen
import com.agrathava.theme.MonochromeTheme
import dagger.hilt.android.AndroidEntryPoint
import me.alokrathava.robotcontroller.ui.theme.RobotControllerTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        enableEdgeToEdge()

        setContent {
            RobotControllerTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MonochromeTheme.colors.background,
                ) { innerPadding ->
                    HomeScreen(
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}
