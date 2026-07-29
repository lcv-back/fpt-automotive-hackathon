package com.sopa.viva_automotive.feature.voice.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sopa.viva_automotive.core.database.settings.SettingsDataStore
import com.sopa.viva_automotive.core.database.settings.VoiceSettings
import com.sopa.viva_automotive.feature.voice.domain.VoiceAssistantStateManager
import com.sopa.viva_automotive.feature.voice.domain.model.VoiceAssistantState
import com.sopa.viva_automotive.feature.voice.domain.model.VoiceEvent
import com.sopa.viva_automotive.feature.voice.service.VoiceAssistantService
import com.sopa.viva_automotive.vehicleservice.api.UxRestrictionsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class VoiceViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    stateManager: VoiceAssistantStateManager,
    uxRestrictionsRepository: UxRestrictionsRepository,
    settingsDataStore: SettingsDataStore,
) : ViewModel() {

    val state: StateFlow<VoiceAssistantState> = stateManager.state

    val events: SharedFlow<VoiceEvent> = stateManager.events

        val isDistractionRestricted: StateFlow<Boolean> =
        uxRestrictionsRepository.isDistractionRestricted
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val settings: StateFlow<VoiceSettings> = settingsDataStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VoiceSettings())

    fun onMicPressed() {
        VoiceAssistantService.startListening(appContext)
    }

        fun onCancelPressed() {
        VoiceAssistantService.stop(appContext)
    }
}
