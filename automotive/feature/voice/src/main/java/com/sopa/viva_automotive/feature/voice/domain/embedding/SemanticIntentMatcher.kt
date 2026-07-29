package com.sopa.viva_automotive.feature.voice.domain.embedding

interface SemanticIntentMatcher {
        suspend fun bestIntent(utterance: String): String?

        suspend fun warmUp() {}
}
