package com.sopa.viva_automotive.core.database.command

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "command_mappings")
data class CommandMappingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val intentType: String,
        val keywords: String,
        val priority: Int = 0,
)
