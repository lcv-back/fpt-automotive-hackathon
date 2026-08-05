package com.sopa.viva_automotive

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sopa.viva_automotive.core.database.settings.SettingsDataStore
import com.sopa.viva_automotive.core.database.settings.VoiceSettings
import com.sopa.viva_automotive.core.ui.locale.AppLanguage
import com.sopa.viva_automotive.core.ui.theme.ThemeMode
import com.sopa.viva_automotive.core.ui.theme.VivaTheme
import com.sopa.viva_automotive.locale.LocaleController
import com.sopa.viva_automotive.locale.LocalizedContent
import com.sopa.viva_automotive.navigation.VivaApp
import com.sopa.viva_automotive.feature.voice.service.VoiceAssistantService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    private val requestRecordAudio =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {  }

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences(LOCALE_PREFS, MODE_PRIVATE)
        val language = AppLanguage.fromStorageKey(prefs.getString(KEY_LANGUAGE, "system"))
        super.attachBaseContext(LocaleController.wrap(newBase, language))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestRecordAudio.launch(Manifest.permission.RECORD_AUDIO)
        }

        setContent {
            val settings by settingsDataStore.settings
                .collectAsStateWithLifecycle(initialValue = VoiceSettings())
            val language = AppLanguage.fromStorageKey(settings.language)

            LaunchedEffect(language) {
                getSharedPreferences(LOCALE_PREFS, MODE_PRIVATE)
                    .edit()
                    .putString(KEY_LANGUAGE, language.storageKey)
                    .apply()
                LocaleController.apply(language)
            }

            val darkTheme = when (ThemeMode.fromStorageKey(settings.themeMode)) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            VivaTheme(darkTheme = darkTheme) {
                LocalizedContent(language = language) {
                    VivaApp()
                }
            }
        }

        dispatchVoiceText(intent)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        dispatchVoiceText(intent)
    }

    private fun dispatchVoiceText(intent: android.content.Intent?) {
        val text = intent?.getStringExtra(EXTRA_VOICE_TEXT)?.trim().orEmpty()
        if (text.isNotEmpty()) {
            VoiceAssistantService.processText(this, text)
        }
    }

    private companion object {
        const val LOCALE_PREFS = "viva_locale"
        const val KEY_LANGUAGE = "language"
        const val EXTRA_VOICE_TEXT = "viva_voice_text"
    }
}
