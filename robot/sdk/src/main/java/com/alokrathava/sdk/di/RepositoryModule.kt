package com.alokrathava.sdk.di

import com.alokrathava.sdk.DefaultRobotRepository
import com.alokrathava.sdk.RobotRepository
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
