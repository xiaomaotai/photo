package com.ruolijianzhen.app.di

import com.ruolijianzhen.app.data.camera.CameraManagerImpl
import com.ruolijianzhen.app.domain.camera.CameraManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CameraModule {
    
    @Binds
    @Singleton
    abstract fun bindCameraManager(
        impl: CameraManagerImpl
    ): CameraManager
}
