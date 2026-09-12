package io.github.mauricem0ss.fastdroid.data

import android.content.Context
import io.github.mauricem0ss.fastdroid.domain.FastRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppTheme { TERMINAL, GITHUB }

/**
 * Two preferences don't justify DataStore. This process is the only writer, so
 * the flows are simply updated alongside each write.
 */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _targetMinutes = MutableStateFlow(
        FastRules.clampTarget(prefs.getInt(KEY_TARGET, FastRules.DEFAULT_TARGET_MINUTES))
    )
    val targetMinutes: StateFlow<Int> = _targetMinutes.asStateFlow()

    private val _theme = MutableStateFlow(
        AppTheme.entries.firstOrNull { it.name == prefs.getString(KEY_THEME, null) } ?: AppTheme.TERMINAL
    )
    val theme: StateFlow<AppTheme> = _theme.asStateFlow()

    fun setTargetMinutes(minutes: Int) {
        val clamped = FastRules.clampTarget(minutes)
        prefs.edit().putInt(KEY_TARGET, clamped).apply()
        _targetMinutes.value = clamped
    }

    fun setTheme(theme: AppTheme) {
        prefs.edit().putString(KEY_THEME, theme.name).apply()
        _theme.value = theme
    }

    private companion object {
        const val KEY_TARGET = "target_minutes"
        const val KEY_THEME = "theme"
    }
}
