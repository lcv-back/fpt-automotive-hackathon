package com.sopa.viva_automotive.feature.media.domain

import kotlinx.coroutines.flow.StateFlow

interface MediaRepository {
    val state: StateFlow<PlaybackUiState>

    suspend fun play(query: String? = null): Result<String>
    suspend fun pause(): Result<String>
    suspend fun next(): Result<String>
    suspend fun previous(): Result<String>
    fun adjustVolume(delta: Int): Result<String>
    fun setMediaVolume(volume: Float): Result<String>

    suspend fun setSource(source: MediaSource): Result<String>
    suspend fun tuneRadio(query: String? = null): Result<String>
    suspend fun selectStation(stationId: String): Result<String>
}
