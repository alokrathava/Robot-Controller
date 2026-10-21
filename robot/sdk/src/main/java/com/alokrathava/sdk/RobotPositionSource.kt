package com.alokrathava.sdk

import com.alokrathava.sdk.model.LiveRobotPosition
import kotlinx.coroutines.flow.StateFlow

/** Optional position capability that leaves the existing RobotRepository contract intact. */
interface RobotPositionSource {
    val livePosition: StateFlow<LiveRobotPosition>
}
