package com.sopa.viva_automotive.di

import com.sopa.viva_automotive.BuildConfig
import com.sopa.viva_automotive.core.common.buildinfo.BuildInfo
import com.sopa.viva_automotive.core.common.buildinfo.BuildInfoProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BuildInfoModule {

    @Provides
    @Singleton
    fun provideBuildInfoProvider(): BuildInfoProvider = object : BuildInfoProvider {
        override val buildInfo = BuildInfo(
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
            applicationId = BuildConfig.APPLICATION_ID,
            buildType = BuildConfig.BUILD_TYPE,
            vehicleBackend = BuildConfig.FLAVOR,
            isDebuggable = BuildConfig.DEBUG,
        )
    }
}
