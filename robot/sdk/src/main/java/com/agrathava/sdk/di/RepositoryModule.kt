package com.agrathava.sdk.di

import com.agrathava.sdk.DefaultRobotRepository
import com.agrathava.sdk.RobotRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRobotRepository(
        impl: DefaultRobotRepository
    ): RobotRepository
}
