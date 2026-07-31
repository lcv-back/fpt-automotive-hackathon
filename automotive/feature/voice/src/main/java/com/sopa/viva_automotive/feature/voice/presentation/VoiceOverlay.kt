package com.sopa.viva_automotive.feature.voice.presentation

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sopa.viva_automotive.core.ui.theme.VivaDimens
import com.sopa.viva_automotive.feature.voice.R
import com.sopa.viva_automotive.feature.voice.domain.model.VoiceAssistantState

@Composable
fun VoiceOverlay(
    modifier: Modifier = Modifier,
    viewModel: VoiceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val restricted by viewModel.isDistractionRestricted.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val listening = state is VoiceAssistantState.Listening
    val processing = state is VoiceAssistantState.Processing ||
        state is VoiceAssistantState.Executing

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MicButton(
                listening = listening,
                animate = listening && !restricted,
                enabled = settings.voiceEnabled && !processing,
                onClick = {
                    if (listening) {
                        viewModel.onCancelPressed()
                    } else {
                        viewModel.onMicPressed()
                    }
                },
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = statusLabel(state, voiceEnabled = settings.voiceEnabled),
                    style = MaterialTheme.typography.titleMedium,
                    color = when (state) {
                        is VoiceAssistantState.Error -> MaterialTheme.colorScheme.error
                        is VoiceAssistantState.Success -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                )
                val detail = detailLabel(state, showPartial = settings.showPartialTranscription)
                if (detail.isNotEmpty()) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun MicButton(
    listening: Boolean,
    animate: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val scale = if (animate) {
        val transition = rememberInfiniteTransition(label = "micPulse")
        val pulse by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
            label = "micPulseScale",
        )
        pulse
    } else {
        1f
    }

    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(VivaDimens.TouchTarget)
            .scale(scale),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = if (listening) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
        ),
    ) {
        Icon(
            imageVector = if (enabled) Icons.Default.Mic else Icons.Default.MicOff,
            contentDescription = stringResource(
                if (listening) R.string.voice_cd_stop else R.string.voice_cd_start,
            ),
            modifier = Modifier.size(36.dp),
        )
    }
}

@Composable
private fun statusLabel(state: VoiceAssistantState, voiceEnabled: Boolean): String = when {
    !voiceEnabled -> stringResource(R.string.voice_disabled)
    state is VoiceAssistantState.Idle -> stringResource(R.string.voice_idle_prompt)
    state is VoiceAssistantState.Listening -> stringResource(R.string.voice_listening)
    state is VoiceAssistantState.Processing -> stringResource(R.string.voice_processing)
    state is VoiceAssistantState.Executing -> state.description
    state is VoiceAssistantState.Clarification -> state.promptVi
    state is VoiceAssistantState.Success -> state.message
    state is VoiceAssistantState.Error -> state.message
    else -> ""
}

private fun detailLabel(state: VoiceAssistantState, showPartial: Boolean): String = when (state) {
    is VoiceAssistantState.Listening ->
        if (showPartial) state.partialTranscription else ""
    is VoiceAssistantState.Processing -> "\u201C${state.utterance}\u201D"
    else -> ""
}
