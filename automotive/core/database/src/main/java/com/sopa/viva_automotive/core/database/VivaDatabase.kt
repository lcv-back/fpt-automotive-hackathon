package com.sopa.viva_automotive.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sopa.viva_automotive.core.database.command.CommandMappingDao
import com.sopa.viva_automotive.core.database.command.CommandMappingEntity

@Database(
    entities = [CommandMappingEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class VivaDatabase : RoomDatabase() {
    abstract fun commandMappingDao(): CommandMappingDao
}
