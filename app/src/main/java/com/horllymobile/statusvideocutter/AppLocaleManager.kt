package com.horllymobile.statusvideocutter

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.horllymobile.statusvideocutter.ui.LanguagePreferences
import java.util.Locale

data class Language(
    val code: String,
    val displayLanguage: Int
)

val appLanguages = listOf(
    Language(code = "en", displayLanguage = R.string.english),
    Language(code = "pt", displayLanguage = R.string.portuguese),
    Language(code = "es", displayLanguage = R.string.spanish),
    Language(code = "fr", displayLanguage = R.string.french)
)

class AppLocaleManager {
    fun changeLanguage(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = context.resources.configuration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            config.setLocale(locale)
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                LocaleList.forLanguageTags(languageCode)
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode))
        }
    }

    suspend fun setLocale(context: Context, languageCode: String) {
        // Save language to DataStore
        LanguagePreferences.saveLanguage(context, languageCode)

        // Apply new locale
        val locale = Locale.forLanguageTag(languageCode)
        Locale.setDefault(locale)

        val config = context.resources.configuration

        // Apply configuration without full restart

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.createConfigurationContext(config)
            config.setLocale(locale)
//            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                LocaleList.forLanguageTags(languageCode)
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode))
        }
    }

    fun getLanguage(context: Context): String {
        val locale =  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)?.applicationLocales?.get(0)
        } else {
            AppCompatDelegate.getApplicationLocales().get(0)
        }

        return locale?.language ?: getDefaultLanguageCode()
    }

    private fun getDefaultLanguageCode(): String {
        return  appLanguages.first().code
    }
}