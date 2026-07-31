package com.sopa.viva_automotive.core.database.command

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CommandMappingDao {

    @Query("SELECT * FROM command_mappings ORDER BY priority DESC")
    fun observeAll(): Flow<List<CommandMappingEntity>>

    @Query("SELECT * FROM command_mappings ORDER BY priority DESC")
    suspend fun getAll(): List<CommandMappingEntity>

    @Query("SELECT COUNT(*) FROM command_mappings")
    suspend fun count(): Int

    @Query("DELETE FROM command_mappings")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(mappings: List<CommandMappingEntity>)
}
