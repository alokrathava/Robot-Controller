package com.alokrathava.sdk

data class RobotConnectionProfile(
    val host: String = "192.168.1.100",
    val port: Int = 8080,
    val token: String = "",
    val useTls: Boolean = true,
    val selectedSsid: String? = null
)
