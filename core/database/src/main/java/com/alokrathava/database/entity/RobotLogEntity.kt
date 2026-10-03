package com.alokrathava.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "robot_logs")
data class RobotLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val logType: String,
    val message: String
)
