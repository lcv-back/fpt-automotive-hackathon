package com.sopa.viva_automotive.feature.voice

import com.sopa.viva_automotive.feature.voice.domain.embedding.SemanticIntentMatcher

class FakeSemanticIntentMatcher(
    private val answers: Map<String, String> = emptyMap(),
) : SemanticIntentMatcher {
    override suspend fun bestIntent(utterance: String): String? =
        answers[utterance.lowercase().trim()]
}
