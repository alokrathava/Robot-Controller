package me.alokrathava.robotcontroller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.alokrathava.robotcontroller.ui.theme.RobotControllerTheme

class MainActivity : ComponentActivity() {
    private lateinit var speaker: Speaker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        speaker = Speaker(this)

        setContent {
            RobotControllerTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFF121212),
                    content = { innerPadding ->
                        RobotControlPanel(
                            modifier = Modifier.padding(innerPadding),
                            speaker = speaker
                        )
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speaker.shutdown()
    }
}

@Composable
fun RobotControlPanel(modifier: Modifier = Modifier, speaker: Speaker) {
    var batteryLevel by remember { mutableStateOf("Unknown") }
    var currentPosition by remember { mutableStateOf("0, 0") }
    var mapData by remember { mutableStateOf("No map available") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color(0xFF121212)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                "Robot Navigation Controller",
                fontSize = 26.sp,
                color = Color.White
            )

            // Status Section
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    StatusRow(Icons.Default.BatteryFull, "Battery Level: $batteryLevel")
                    StatusRow(Icons.Default.LocationOn, "Current Position: $currentPosition")
                    StatusRow(Icons.Default.Map, "Map Data: $mapData")
                }
            }

            // Movement Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MovementButton(" ", Icons.Default.KeyboardArrowLeft, Color(0xFF03DAC5)) {
                    speaker.speak("Move Left")
                }
                Column {
                    MovementButton(" ", Icons.Default.KeyboardArrowUp, Color(0xFF03DAC5)) {
                        speaker.speak("Move Forward")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    MovementButton(" ", Icons.Default.KeyboardArrowDown, Color(0xFF03DAC5)) {
                        speaker.speak("Move Backward")
                    }
                }
                MovementButton(" ", Icons.Default.KeyboardArrowRight, Color(0xFF03DAC5)) {
                    speaker.speak("Move Right")
                }
            }

            ActionButtonRow(
                listOf(
                    Triple("Go to Charge", Icons.Default.EvStation, { speaker.speak("Going to Charge") }),
                    Triple("Cancel Navigation", Icons.Default.Cancel, { speaker.speak("Cancel Navigation") })
                )
            )

            ActionButtonRow(
                listOf(
                    Triple("Get Position", Icons.Default.GpsFixed, { speaker.speak("Getting Position") }),
                    Triple("Move to Position", Icons.Default.MyLocation, { speaker.speak("Moving to Position") })
                )
            )

            ActionButtonRow(
                listOf(
                    Triple("Get Map", Icons.Default.Map, { speaker.speak("Fetching Map Data") }),
                    Triple("Save Map", Icons.Default.Save, { speaker.speak("Saving Map") })
                )
            )


            // Battery Status Button
            Button(
                onClick = { speaker.speak("Battery Level is $batteryLevel") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC))
            ) {
                Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Get Battery Level", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

// Reusable Status Row with Icons
@Composable
fun StatusRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFFBB86FC), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontSize = 18.sp, color = Color.White)
    }
}

// Reusable Button for Movement with Icons
@Composable
fun MovementButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = MaterialTheme.shapes.extraSmall,
        modifier = Modifier.size(120.dp, 50.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.Black)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, color = Color.Black, fontSize = 12.sp)
    }
}

// Reusable Row for Action Buttons with Icons
@Composable
fun ActionButtonRow(buttons: List<Triple<String, androidx.compose.ui.graphics.vector.ImageVector, () -> Unit>>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        buttons.forEach { (text, icon, action) ->
            Button(
                onClick = action,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE)),
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.size(160.dp, 50.dp)
            ) {
                Icon(icon, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text, color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RobotControlPanelPreview() {
    RobotControllerTheme {
        RobotControlPanel(speaker = Speaker(LocalContext.current))
    }
}
