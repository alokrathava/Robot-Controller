package com.alokrathava.sdk.model

/**
 * Defines capabilities supported by the robot gateway, including geospatial and 3D spatial features.
 */
enum class RobotCapability {
    NAVIGATION,
    MANUAL_CONTROL,
    DOCKING,
    SAFETY,
    TELEMETRY,
    BATTERY,
    HEALTH,
    MAP_SWITCHING,
    MAP_MANAGEMENT,
    MAPPING,
    VIRTUAL_WALLS,
    SAVED_LOCATIONS,
    WAYPOINT_NAVIGATION,
    MISSIONS,
    ROBOT_CONFIGURATION,
    ROBOT_DISCOVERY,
    DIAGNOSTICS,
    LOG_RETRIEVAL,
    GEOSPATIAL_NAVIGATION,
    GEOSPATIAL_TELEMETRY,
    SPATIAL_3D
}
