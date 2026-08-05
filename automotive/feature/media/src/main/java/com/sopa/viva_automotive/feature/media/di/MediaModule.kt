package com.sopa.viva_automotive.feature.media.di

import com.sopa.viva_automotive.feature.media.data.VivaMediaRepository
import com.sopa.viva_automotive.feature.media.domain.MediaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MediaModule {

    @Binds
    @Singleton
    abstract fun bindMediaRepository(impl: VivaMediaRepository): MediaRepository
}
