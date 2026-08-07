package com.sopa.viva_automotive.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sopa.viva_automotive.feature.media.domain.MediaRepository
import com.sopa.viva_automotive.feature.media.domain.PlaybackUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
) : ViewModel() {

    val playback: StateFlow<PlaybackUiState> = mediaRepository.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaybackUiState())

    fun togglePlayPause() {
        viewModelScope.launch {
            if (playback.value.isPlaying) {
                mediaRepository.pause()
            } else {
                mediaRepository.play()
            }
        }
    }

    fun next() {
        viewModelScope.launch { mediaRepository.next() }
    }

    fun previous() {
        viewModelScope.launch { mediaRepository.previous() }
    }

    fun selectStation(stationId: String) {
        viewModelScope.launch { mediaRepository.selectStation(stationId) }
    }
}
