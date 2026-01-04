package com.simplenotepad.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.simplenotepad.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val WORD_WRAP = booleanPreferencesKey("word_wrap")
        val FONT_SIZE = floatPreferencesKey("font_size")
        val SHOW_STATUS_BAR = booleanPreferencesKey("show_status_bar")
        val RECENT_FILES = stringSetPreferencesKey("recent_files")
        val ZOOM_LEVEL = floatPreferencesKey("zoom_level")
        val AUTO_SAVE = booleanPreferencesKey("auto_save")
        val SAVED_SESSION = stringPreferencesKey("saved_session")
        val ACTIVE_TAB_ID = stringPreferencesKey("active_tab_id")
        val NOTES_FOLDER_URI = stringPreferencesKey("notes_folder_uri")
        val FORMATTED_VIEW = booleanPreferencesKey("formatted_view")
        val TABS_AT_BOTTOM = booleanPreferencesKey("tabs_at_bottom")
    }

    data class AppPreferences(
        val themeMode: ThemeMode = ThemeMode.AUTO,
        val wordWrap: Boolean = true,
        val fontSize: Float = 14f,
        val showStatusBar: Boolean = true,
        val recentFiles: List<String> = emptyList(),
        val zoomLevel: Float = 1f,
        val autoSave: Boolean = false,
        val notesFolderUri: String? = null,
        val formattedView: Boolean = true,  // true = formatted, false = markdown syntax
        val tabsAtBottom: Boolean = false   // true = tabs at bottom, false = tabs at top
    )

    val preferences: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        AppPreferences(
            themeMode = prefs[Keys.THEME_MODE]?.let { ThemeMode.valueOf(it) } ?: ThemeMode.AUTO,
            wordWrap = prefs[Keys.WORD_WRAP] ?: true,
            fontSize = prefs[Keys.FONT_SIZE] ?: 14f,
            showStatusBar = prefs[Keys.SHOW_STATUS_BAR] ?: true,
            recentFiles = prefs[Keys.RECENT_FILES]?.toList() ?: emptyList(),
            zoomLevel = prefs[Keys.ZOOM_LEVEL] ?: 1f,
            autoSave = prefs[Keys.AUTO_SAVE] ?: false,
            notesFolderUri = prefs[Keys.NOTES_FOLDER_URI],
            formattedView = prefs[Keys.FORMATTED_VIEW] ?: true,
            tabsAtBottom = prefs[Keys.TABS_AT_BOTTOM] ?: false
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setWordWrap(enabled: Boolean) {
        context.dataStore.edit { it[Keys.WORD_WRAP] = enabled }
    }

    suspend fun setFontSize(size: Float) {
        context.dataStore.edit { it[Keys.FONT_SIZE] = size.coerceIn(8f, 32f) }
    }

    suspend fun setShowStatusBar(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_STATUS_BAR] = show }
    }

    suspend fun setZoomLevel(level: Float) {
        context.dataStore.edit { it[Keys.ZOOM_LEVEL] = level.coerceIn(0.5f, 3f) }
    }

    suspend fun setAutoSave(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_SAVE] = enabled }
    }

    suspend fun setFormattedView(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FORMATTED_VIEW] = enabled }
    }

    suspend fun setTabsAtBottom(enabled: Boolean) {
        context.dataStore.edit { it[Keys.TABS_AT_BOTTOM] = enabled }
    }

    suspend fun setNotesFolderUri(uri: String?) {
        context.dataStore.edit { prefs ->
            if (uri != null) {
                prefs[Keys.NOTES_FOLDER_URI] = uri
            } else {
                prefs.remove(Keys.NOTES_FOLDER_URI)
            }
        }
    }

    suspend fun addRecentFile(uri: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.RECENT_FILES]?.toMutableList() ?: mutableListOf()
            current.remove(uri)
            current.add(0, uri)
            prefs[Keys.RECENT_FILES] = current.take(10).toSet()
        }
    }

    suspend fun removeRecentFile(uri: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.RECENT_FILES]?.toMutableSet() ?: mutableSetOf()
            current.remove(uri)
            prefs[Keys.RECENT_FILES] = current
        }
    }

    suspend fun clearRecentFiles() {
        context.dataStore.edit { it[Keys.RECENT_FILES] = emptySet() }
    }

    // Session persistence
    suspend fun saveSession(sessionJson: String, activeTabId: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SAVED_SESSION] = sessionJson
            prefs[Keys.ACTIVE_TAB_ID] = activeTabId
        }
    }

    suspend fun getSession(): Pair<String?, String?> {
        val prefs = context.dataStore.data.map { prefs ->
            Pair(prefs[Keys.SAVED_SESSION], prefs[Keys.ACTIVE_TAB_ID])
        }
        return prefs.first()
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.SAVED_SESSION)
            prefs.remove(Keys.ACTIVE_TAB_ID)
        }
    }
}
