package com.alokrathava.sdk

import com.alokrathava.sdk.internal.RobotClientImpl

object RobotSdk {
    fun create(config: RobotSdkConfig): RobotClient {
        return RobotClientImpl(config)
    }
}
