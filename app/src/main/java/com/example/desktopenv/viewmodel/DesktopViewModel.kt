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
import com.example.desktopenv.data.NotificationAccess
import com.example.desktopenv.data.NotificationStore
import com.example.desktopenv.data.SystemStatus
import com.example.desktopenv.data.ThemeChoice
import com.example.desktopenv.data.WALLPAPER_COUNT
import com.example.desktopenv.model.NotificationRecord
import com.example.desktopenv.model.Placement
import com.example.desktopenv.model.RecentList
import com.example.desktopenv.model.WindowKind
import com.example.desktopenv.model.WindowLayoutCodec
import com.example.desktopenv.model.WindowLogic
import com.example.desktopenv.model.WindowsState
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
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
        restoreAndPersistLayout()
    }

    /**
     * Restaura o layout salvo e, a partir daí, salva cada mudança de janelas com debounce.
     * Falhas de disco não derrubam o desktop: o layout só deixa de ser persistido.
     */
    private fun restoreAndPersistLayout() {
        viewModelScope.launch {
            val saved = runCatching { prefs.state.first().layout }.getOrDefault("")
            _windows.value = WindowLayoutCodec.decode(saved)
            _windows.debounce(400L).collect { state ->
                runCatching { prefs.setLayout(WindowLayoutCodec.encode(state)) }
            }
        }
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

    fun openBuiltin(kind: WindowKind, areaW: Float, areaH: Float, density: Float, payload: String = "") {
        _windows.update { WindowLogic.open(it, kind, areaW, areaH, density, payload) }
        if (kind == WindowKind.NOTIFICATIONS) NotificationStore.markAllRead()
        closeOverlays()
    }

    // ---- Notificações ----

    val notifications: StateFlow<List<NotificationRecord>> = NotificationStore.items
    val unreadNotifications: StateFlow<Int> = NotificationStore.unread

    fun clearNotifications() = NotificationStore.clear()

    fun hasNotificationAccess(): Boolean = NotificationAccess.isGranted(getApplication())

    fun maximizeWindow(id: Long) = _windows.update { WindowLogic.maximize(it, id) }

    fun toggleMaximizeWindow(id: Long) = _windows.update { WindowLogic.toggleMaximize(it, id) }

    fun restoreWindow(id: Long) = _windows.update { WindowLogic.restore(it, id) }

    fun snapWindow(id: Long, side: Placement) = _windows.update { WindowLogic.snap(it, id, side) }

    /** Chamado ao soltar uma janela arrastada: encaixa se ela parou numa borda. */
    fun endWindowDrag(id: Long, areaW: Float, areaH: Float, density: Float) =
        _windows.update { WindowLogic.edgeSnap(it, id, areaW, areaH, threshold = EDGE_SNAP_DP * density) }

    /** Ações da janela em foco, usadas pelos atalhos de teclado. */
    fun maximizeTopWindow() = updateTop { s, id -> WindowLogic.maximize(s, id) }

    fun toggleMaximizeTopWindow() = updateTop { s, id -> WindowLogic.toggleMaximize(s, id) }

    fun restoreTopWindow() = updateTop { s, id -> WindowLogic.restore(s, id) }

    fun snapTopWindow(side: Placement) = updateTop { s, id -> WindowLogic.snap(s, id, side) }

    private fun updateTop(transform: (WindowsState, Long) -> WindowsState) {
        _windows.update { s -> s.topWindowId?.let { transform(s, it) } ?: s }
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

    /**
     * Devolve a Intent de lançamento (ou null se o app não puder ser aberto).
     * Só registra o app como recente quando a Intent existe, para não listar apps inválidos.
     */
    fun launch(packageName: String): Intent? {
        val intent = catalog.launchIntent(packageName) ?: return null
        viewModelScope.launch {
            prefs.setRecent(RecentList.push(prefsState.value.recent, packageName))
        }
        return intent
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

    private companion object {
        /** Distância da borda, em dp, que dispara o encaixe ao soltar a janela. */
        const val EDGE_SNAP_DP = 12f
    }
}
