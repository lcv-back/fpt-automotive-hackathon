package com.sopa.viva_automotive.feature.voice

import com.sopa.viva_automotive.core.database.command.CommandMappingDao
import com.sopa.viva_automotive.core.database.command.CommandMappingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeCommandMappingDao : CommandMappingDao {

    private val mappings = MutableStateFlow<List<CommandMappingEntity>>(emptyList())

    override fun observeAll(): Flow<List<CommandMappingEntity>> = mappings

    override suspend fun getAll(): List<CommandMappingEntity> =
        mappings.value.sortedByDescending { it.priority }

    override suspend fun count(): Int = mappings.value.size

    override suspend fun deleteAll() {
        mappings.value = emptyList()
    }

    override suspend fun insertAll(mappings: List<CommandMappingEntity>) {
        this.mappings.value = this.mappings.value + mappings
    }
}
