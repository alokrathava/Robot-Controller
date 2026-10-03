package com.alokrathava.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.alokrathava.sdk.RobotEndpoint
import com.alokrathava.sdk.RobotSdk
import com.alokrathava.sdk.RobotSdkConfig
import com.alokrathava.sdk.model.Pose2D
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val robotClient = RobotSdk.create(
        RobotSdkConfig(
            endpoint = RobotEndpoint(host = "192.168.1.100", port = 8080)
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            robotClient.connect()
            robotClient.listMaps()
            robotClient.navigateTo(Pose2D(xMeters = 2.0, yMeters = 1.0, yawRadians = 0.0))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        robotClient.close()
    }
}
