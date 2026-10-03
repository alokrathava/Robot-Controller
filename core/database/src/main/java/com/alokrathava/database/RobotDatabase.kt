package com.alokrathava.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.alokrathava.database.dao.RobotLogDao
import com.alokrathava.database.entity.RobotLogEntity

@Database(
    entities = [RobotLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class RobotDatabase : RoomDatabase() {
    abstract fun robotLogDao(): RobotLogDao
}
