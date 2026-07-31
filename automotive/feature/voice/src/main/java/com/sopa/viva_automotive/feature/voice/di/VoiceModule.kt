package com.sopa.viva_automotive.feature.voice.di

import com.sopa.viva_automotive.feature.voice.data.SpeechRecognitionEngine
import com.sopa.viva_automotive.feature.voice.data.embedding.OnnxEmbeddingIntentMatcher
import com.sopa.viva_automotive.feature.voice.data.vosk.VoskSpeechRecognitionEngine
import com.sopa.viva_automotive.feature.voice.domain.embedding.SemanticIntentMatcher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VoiceModule {

    @Binds
    @Singleton
    abstract fun bindSpeechRecognitionEngine(
        impl: VoskSpeechRecognitionEngine,
    ): SpeechRecognitionEngine

    @Binds
    @Singleton
    abstract fun bindSemanticIntentMatcher(
        impl: OnnxEmbeddingIntentMatcher,
    ): SemanticIntentMatcher
}
