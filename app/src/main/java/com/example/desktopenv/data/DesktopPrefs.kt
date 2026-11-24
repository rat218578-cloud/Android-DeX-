package com.example.desktopenv.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeChoice { SYSTEM, LIGHT, DARK }

data class DesktopPrefsState(
    val theme: ThemeChoice = ThemeChoice.SYSTEM,
    val wallpaper: Int = 0,
    val pinned: List<String> = emptyList(),
    val recent: List<String> = emptyList(),
    /** Layout das janelas no formato de [com.example.desktopenv.model.WindowLayoutCodec]. */
    val layout: String = "",
)

/** Deve ser igual ao número de papéis de parede definidos na UI. */
const val WALLPAPER_COUNT = 4

private val Context.desktopDataStore: DataStore<Preferences> by preferencesDataStore(name = "desktop_prefs")

/** Persistência das configurações do desktop com DataStore. */
class DesktopPrefs(context: Context) {

    private val store = context.applicationContext.desktopDataStore

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val WALLPAPER = intPreferencesKey("wallpaper")
        val PINNED = stringPreferencesKey("pinned")
        val RECENT = stringPreferencesKey("recent")
        val LAYOUT = stringPreferencesKey("window_layout")
    }

    val state: Flow<DesktopPrefsState> = store.data.map { p ->
        DesktopPrefsState(
            theme = p[Keys.THEME]?.let { name -> ThemeChoice.entries.firstOrNull { it.name == name } }
                ?: ThemeChoice.SYSTEM,
            wallpaper = (p[Keys.WALLPAPER] ?: 0).coerceIn(0, WALLPAPER_COUNT - 1),
            pinned = decode(p[Keys.PINNED]),
            recent = decode(p[Keys.RECENT]),
            layout = p[Keys.LAYOUT].orEmpty(),
        )
    }

    suspend fun setLayout(encoded: String) {
        store.edit { it[Keys.LAYOUT] = encoded }
    }

    suspend fun setTheme(choice: ThemeChoice) {
        store.edit { it[Keys.THEME] = choice.name }
    }

    suspend fun setWallpaper(index: Int) {
        store.edit { it[Keys.WALLPAPER] = index.coerceIn(0, WALLPAPER_COUNT - 1) }
    }

    suspend fun setPinned(packages: List<String>) {
        store.edit { it[Keys.PINNED] = packages.joinToString(SEPARATOR) }
    }

    suspend fun setRecent(packages: List<String>) {
        store.edit { it[Keys.RECENT] = packages.joinToString(SEPARATOR) }
    }

    private fun decode(raw: String?): List<String> =
        raw?.split(SEPARATOR)?.filter { it.isNotBlank() }.orEmpty()

    private companion object {
        const val SEPARATOR = ","
    }
}
