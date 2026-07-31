package com.sopa.viva_automotive.feature.voice.di

import android.content.Context
import com.sopa.viva_automotive.feature.voice.data.SpeechRecognitionEngine
import com.sopa.viva_automotive.feature.voice.data.embedding.OnnxEmbeddingIntentMatcher
import com.sopa.viva_automotive.feature.voice.data.vosk.VoskSpeechRecognitionEngine
import com.sopa.viva_automotive.feature.voice.domain.embedding.SemanticIntentMatcher
import com.viva.voice.tts.AndroidTtsSpeaker
import com.viva.voice.tts.TtsSpeaker
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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

    companion object {
        /**
         * Application-scoped: `TextToSpeech` init costs hundreds of
         * milliseconds, and paying it inside a turn would land straight in the
         * p95 the 1500ms claim is about. It owns its own audio focus, which is
         * what ducks the music while the assistant answers (L7).
         */
        @Provides
        @Singleton
        fun provideTtsSpeaker(@ApplicationContext context: Context): TtsSpeaker =
            AndroidTtsSpeaker(context)
    }
}
