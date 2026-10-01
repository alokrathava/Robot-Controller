package com.agrathava.database.di

import android.content.Context
import androidx.room.Room
import com.agrathava.database.RobotDatabase
import com.agrathava.database.dao.RobotLogDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideRobotDatabase(
        @ApplicationContext context: Context
    ): RobotDatabase {
        return Room.databaseBuilder(
            context,
            RobotDatabase::class.java,
            "robot_controller.db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideRobotLogDao(
        database: RobotDatabase
    ): RobotLogDao {
        return database.robotLogDao()
    }
}
