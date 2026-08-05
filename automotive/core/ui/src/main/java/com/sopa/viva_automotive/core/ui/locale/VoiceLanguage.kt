package com.sopa.viva_automotive.core.ui.locale

enum class VoiceLanguage(
    val storageKey: String,
    val voskAssetDir: String,
) {
    ENGLISH("en", "model-en-us"),
    VIETNAMESE("vi", "model-vi"),
    ;

    companion object {
        fun fromStorageKey(key: String?): VoiceLanguage =
            entries.firstOrNull { it.storageKey == key } ?: VIETNAMESE
    }
}
