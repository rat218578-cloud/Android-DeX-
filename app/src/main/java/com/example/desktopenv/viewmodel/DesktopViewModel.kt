package com.example.desktopenv.viewmodel

import android.app.Application
import android.content.Intent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.desktopenv.data.AppCatalog
import com.example.desktopenv.data.AppEntry
import com.example.desktopenv.data.BatteryInfo
import com.example.desktopenv.data.DesktopPrefs
import com.example.desktopenv.data.DesktopPrefsState
import com.example.desktopenv.data.NetworkKind
import com.example.desktopenv.data.SystemStatus
import com.example.desktopenv.data.ThemeChoice
import com.example.desktopenv.data.WALLPAPER_COUNT
import com.example.desktopenv.model.RecentList
import com.example.desktopenv.model.WindowKind
import com.example.desktopenv.model.WindowLogic
import com.example.desktopenv.model.WindowsState
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado e ações do desktop. Toda lógica de janelas está em [WindowLogic], testável sem Android. */
class DesktopViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = DesktopPrefs(application)
    private val catalog = AppCatalog(application)
    private val status = SystemStatus(application)

    val prefsState: StateFlow<DesktopPrefsState> =
        prefs.state.stateIn(viewModelScope, SharingStarted.Eagerly, DesktopPrefsState())

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    private val _icons = MutableStateFlow<Map<String, ImageBitmap>>(emptyMap())
    val icons: StateFlow<Map<String, ImageBitmap>> = _icons.asStateFlow()

    private val _windows = MutableStateFlow(WindowsState())
    val windows: StateFlow<WindowsState> = _windows.asStateFlow()

    private val _startOpen = MutableStateFlow(false)
    val startOpen: StateFlow<Boolean> = _startOpen.asStateFlow()

    private val _quickOpen = MutableStateFlow(false)
    val quickOpen: StateFlow<Boolean> = _quickOpen.asStateFlow()

    val battery: StateFlow<BatteryInfo> = status.battery
    val network: StateFlow<NetworkKind> = status.network

    val clock: StateFlow<LocalDateTime> = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(15_000L)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LocalDateTime.now())

    init {
        status.start()
        loadApps()
    }

    override fun onCleared() {
        status.stop()
        super.onCleared()
    }

    private fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = catalog.loadLaunchable()
            _apps.value = list
            list.forEach { entry ->
                catalog.icon(entry.packageName)?.let { bitmap ->
                    _icons.update { it + (entry.packageName to bitmap) }
                }
            }
        }
    }

    fun labelFor(packageName: String): String =
        _apps.value.firstOrNull { it.packageName == packageName }?.label ?: packageName

    // ---- Overlays (menu Iniciar e configurações rápidas) ----

    fun toggleStart() {
        _quickOpen.value = false
        _startOpen.update { !it }
    }

    fun toggleQuick() {
        _startOpen.value = false
        _quickOpen.update { !it }
    }

    fun closeOverlays() {
        _startOpen.value = false
        _quickOpen.value = false
    }

    // ---- Janelas internas ----

    fun openBuiltin(kind: WindowKind, areaW: Float, areaH: Float, density: Float) {
        _windows.update { WindowLogic.open(it, kind, areaW, areaH, density) }
        closeOverlays()
    }

    fun focusWindow(id: Long) = _windows.update { WindowLogic.focus(it, id) }

    fun toggleWindow(id: Long) = _windows.update { WindowLogic.taskbarToggle(it, id) }

    fun minimizeWindow(id: Long) = _windows.update { WindowLogic.minimize(it, id) }

    fun closeWindow(id: Long) = _windows.update { WindowLogic.close(it, id) }

    fun closeAllWindows() = _windows.update { WindowLogic.closeAll(it) }

    fun cycleWindows() = _windows.update { WindowLogic.cycle(it) }

    fun closeTopWindow() = _windows.update { WindowLogic.closeTop(it) }

    fun minimizeTopWindow() = _windows.update { WindowLogic.minimizeTop(it) }

    fun moveWindow(id: Long, dx: Float, dy: Float, areaW: Float, areaH: Float, density: Float) =
        _windows.update { WindowLogic.move(it, id, dx, dy, areaW, areaH, minVisible = 40f * density) }

    fun resizeWindow(id: Long, dw: Float, dh: Float, areaW: Float, areaH: Float, density: Float) =
        _windows.update {
            WindowLogic.resize(
                it, id, dw, dh,
                minW = 240f * density, minH = 160f * density,
                areaW = areaW, areaH = areaH,
            )
        }

    // ---- Aplicativos ----

    /** Registra o app como recente e devolve a Intent de lançamento (ou null se não existir). */
    fun launch(packageName: String): Intent? {
        viewModelScope.launch {
            prefs.setRecent(RecentList.push(prefsState.value.recent, packageName))
        }
        return catalog.launchIntent(packageName)
    }

    fun togglePin(packageName: String) {
        val current = prefsState.value.pinned
        val next = if (packageName in current) current - packageName else current + packageName
        viewModelScope.launch { prefs.setPinned(next) }
    }

    fun clearRecent() {
        viewModelScope.launch { prefs.setRecent(emptyList()) }
    }

    // ---- Preferências ----

    fun setTheme(choice: ThemeChoice) {
        viewModelScope.launch { prefs.setTheme(choice) }
    }

    fun setWallpaper(index: Int) {
        viewModelScope.launch { prefs.setWallpaper(index) }
    }

    fun nextWallpaper() {
        setWallpaper((prefsState.value.wallpaper + 1) % WALLPAPER_COUNT)
    }
}
