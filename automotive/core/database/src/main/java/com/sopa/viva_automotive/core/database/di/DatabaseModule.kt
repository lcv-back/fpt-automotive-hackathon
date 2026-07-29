package com.sopa.viva_automotive.core.database.di

import android.content.Context
import androidx.room.Room
import com.sopa.viva_automotive.core.database.VivaDatabase
import com.sopa.viva_automotive.core.database.command.CommandMappingDao
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
    fun provideDatabase(@ApplicationContext context: Context): VivaDatabase =
        Room.databaseBuilder(context, VivaDatabase::class.java, "viva.db").build()

    @Provides
    fun provideCommandMappingDao(database: VivaDatabase): CommandMappingDao =
        database.commandMappingDao()
}
