package com.agrathava.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.agrathava.database.dao.RobotLogDao
import com.agrathava.database.entity.RobotLogEntity

@Database(
    entities = [RobotLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class RobotDatabase : RoomDatabase() {
    abstract fun robotLogDao(): RobotLogDao
}
