package com.alokrathava.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alokrathava.database.entity.RobotLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RobotLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: RobotLogEntity)

    @Query("SELECT * FROM robot_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<RobotLogEntity>>

    @Query("DELETE FROM robot_logs")
    suspend fun clearLogs()
}
