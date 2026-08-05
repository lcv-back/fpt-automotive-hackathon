package com.sopa.viva_automotive.feature.voice.di

import android.content.Context
import com.sopa.viva_automotive.feature.voice.data.SpeechRecognitionEngine
import com.sopa.viva_automotive.feature.voice.data.audio.AndroidVolumeController
import com.sopa.viva_automotive.feature.voice.data.embedding.OnnxEmbeddingIntentMatcher
import com.sopa.viva_automotive.feature.voice.data.vosk.VoskSpeechRecognitionEngine
import com.sopa.viva_automotive.feature.voice.domain.delivery.DeliveryRepository
import com.sopa.viva_automotive.feature.voice.domain.delivery.InMemoryDeliveryRepository
import com.sopa.viva_automotive.feature.voice.domain.audio.VolumeController
import com.sopa.viva_automotive.feature.voice.domain.embedding.SemanticIntentMatcher
import com.viva.voice.audio.AndroidPcmSource
import com.viva.voice.audio.AudioCapture
import com.viva.voice.audio.PcmSourceAudioCapture
import com.viva.voice.intent.GrammarIntentRouter
import com.viva.voice.trace.SystemNanoClock
import com.viva.voice.intent.IntentRouter
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

    @Binds
    @Singleton
    abstract fun bindVolumeController(
        impl: AndroidVolumeController,
    ): VolumeController

    /**
     * Singleton because the route is mutable state: the stop the driver just
     * confirmed must still be delivered on the next turn, and a per-injection
     * instance would silently reset it.
     */
    @Binds
    @Singleton
    abstract fun bindDeliveryRepository(
        impl: InMemoryDeliveryRepository,
    ): DeliveryRepository

    companion object {
        /**
         * Chủ sở hữu microphone **duy nhất** của app (28-PIPELINE §0, quyết định 1).
         *
         * Singleton vì đúng một thành phần được cầm mic: `AndroidPcmSource.start()`
         * là no-op khi `AudioRecord` đang mở, nên hai session chồng nhau không tạo ra
         * hai handle. `VoiceAssistantService` vốn đã chặn lượt thứ hai bằng
         * `pipelineJob`; ràng buộc này là lớp thứ hai, không phải lớp duy nhất.
         */
        @Provides
        @Singleton
        fun provideAudioCapture(): AudioCapture = PcmSourceAudioCapture(
            source = AndroidPcmSource(),
            clock = SystemNanoClock,
        )

        /**
         * The T0 grammar tier, bound here instead of being constructed inside
         * `ProcessVoiceCommandUseCase` so the N4 ablation can replace it with a
         * no-op router and measure what the grammar tier is actually holding up
         * (`16-QUYET-DINH-DUONG-NLU.md`). Stateless apart from its rule list,
         * so one instance is enough.
         */
        @Provides
        @Singleton
        fun provideIntentRouter(): IntentRouter = GrammarIntentRouter()

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
