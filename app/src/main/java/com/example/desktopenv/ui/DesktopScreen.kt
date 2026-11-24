package com.example.desktopenv.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.desktopenv.model.Placement
import com.example.desktopenv.model.ShortcutAction
import com.example.desktopenv.model.ShortcutMap
import com.example.desktopenv.model.WindowKind
import com.example.desktopenv.ui.theme.Wallpapers
import com.example.desktopenv.viewmodel.DesktopViewModel
import kotlin.math.roundToInt

/**
 * Tela principal do desktop: papel de parede, área de trabalho com ícones,
 * janelas internas, barra de tarefas e sobreposições.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DesktopScreen(viewModel: DesktopViewModel, onLaunch: (String) -> Unit) {
    val prefs by viewModel.prefsState.collectAsState()
    val apps by viewModel.apps.collectAsState()
    val icons by viewModel.icons.collectAsState()
    val windows by viewModel.windows.collectAsState()
    val startOpen by viewModel.startOpen.collectAsState()
    val quickOpen by viewModel.quickOpen.collectAsState()
    val battery by viewModel.battery.collectAsState()
    val network by viewModel.network.collectAsState()
    val clock by viewModel.clock.collectAsState()
    val unreadNotifications by viewModel.unreadNotifications.collectAsState()
    val mediaInfo by viewModel.mediaInfo.collectAsState()
    val mediaVolume by viewModel.mediaVolume.collectAsState()
    val fileClipboard by viewModel.fileClipboard.collectAsState()

    val density = LocalDensity.current.density
    var areaPx by remember { mutableStateOf(IntSize.Zero) }
    var contextAt by remember { mutableStateOf<Offset?>(null) }
    val focusRequester = remember { FocusRequester() }

    // Teclado e mouse precisam de foco no contêiner raiz para receber atalhos.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val areaW = areaPx.width.toFloat()
    val areaH = areaPx.height.toFloat()
    val pinnedApps = prefs.pinned.mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg } }
    val recentApps = prefs.recent.mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg } }.take(6)
    val wallpaper = Wallpapers[prefs.wallpaper.coerceIn(0, Wallpapers.lastIndex)]

    fun openBuiltin(kind: WindowKind) = viewModel.openBuiltin(kind, areaW, areaH, density)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(wallpaper)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event -> handleShortcut(event, viewModel, ::openBuiltin) }
            // Toque em área vazia fecha menus; pressão longa abre o menu de contexto.
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        viewModel.closeOverlays()
                        contextAt = null
                    },
                    onLongPress = { contextAt = it },
                )
            }
            // Clique direito com mouse abre o menu de contexto.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                            contextAt = event.changes.first().position
                        }
                    }
                }
            },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Área de trabalho e janelas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onSizeChanged { areaPx = it },
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(96.dp),
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(WindowKind.entries) { kind ->
                        AppTile(
                            label = kind.title,
                            bitmap = null,
                            vector = kind.vector(),
                            onClick = { openBuiltin(kind) },
                        )
                    }
                    items(pinnedApps, key = { it.packageName }) { app ->
                        AppTile(
                            label = app.label,
                            bitmap = icons[app.packageName],
                            vector = null,
                            onClick = { viewModel.closeOverlays(); onLaunch(app.packageName) },
                            onLongClick = { viewModel.togglePin(app.packageName) },
                        )
                    }
                }

                windows.windows
                    .filter { !it.minimized }
                    .sortedBy { it.z }
                    .forEach { window ->
                        key(window.id) {
                            // Entrada suave ao abrir; ao fechar, a janela sai da lista sem animação de saída.
                            var entered by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) { entered = true }
                            val enterProgress by animateFloatAsState(
                                targetValue = if (entered) 1f else 0f,
                                animationSpec = tween(180),
                                label = "windowEnter",
                            )
                            WindowHost(
                                window = window,
                                enterProgress = enterProgress,
                                frame = window.frame(areaW, areaH),
                                onFocus = { viewModel.focusWindow(window.id) },
                                onDrag = { dx, dy ->
                                    viewModel.moveWindow(window.id, dx, dy, areaW, areaH, density)
                                },
                                onDragEnd = {
                                    viewModel.endWindowDrag(window.id, areaW, areaH, density)
                                },
                                onResize = { dw, dh ->
                                    viewModel.resizeWindow(window.id, dw, dh, areaW, areaH, density)
                                },
                                onToggleMaximize = { viewModel.toggleMaximizeWindow(window.id) },
                                onSnap = { side -> viewModel.snapWindow(window.id, side) },
                                onMinimize = { viewModel.minimizeWindow(window.id) },
                                onClose = { viewModel.closeWindow(window.id) },
                            ) {
                                WindowBody(
                                    kind = window.kind,
                                    theme = prefs.theme,
                                    wallpaper = prefs.wallpaper,
                                    recent = prefs.recent,
                                    labelOf = viewModel::labelFor,
                                    onTheme = viewModel::setTheme,
                                    onWallpaper = viewModel::setWallpaper,
                                    onClearRecent = viewModel::clearRecent,
                                    onCloseAll = viewModel::closeAllWindows,
                                    onLaunch = { pkg -> viewModel.closeOverlays(); onLaunch(pkg) },
                                    onClearNotifications = viewModel::clearNotifications,
                                    mediaContent = {
                                        MediaBody(
                                            media = mediaInfo,
                                            volumePercent = mediaVolume,
                                            onPlayPause = viewModel::mediaPlayPause,
                                            onNext = viewModel::mediaNext,
                                            onPrevious = viewModel::mediaPrevious,
                                            onVolumeUp = viewModel::mediaVolumeUp,
                                            onVolumeDown = viewModel::mediaVolumeDown,
                                            onResume = viewModel::refreshMedia,
                                        )
                                    },
                                    filesContent = {
                                        FilesBody(
                                            rootUri = window.payload,
                                            folders = prefs.folders,
                                            repo = viewModel.fileRepository,
                                            clipboard = fileClipboard,
                                            onRootChange = { viewModel.setWindowPayload(window.id, it) },
                                            onAddFolder = viewModel::addFolder,
                                            onRemoveFolder = viewModel::removeFolder,
                                            onClipboardChange = viewModel::setClipboard,
                                        )
                                    },
                                )
                            }
                        }
                    }
            }

            Taskbar(
                windows = windows,
                pinnedApps = pinnedApps,
                icons = icons,
                startOpen = startOpen,
                quickOpen = quickOpen,
                battery = battery,
                network = network,
                clock = clock,
                onStart = viewModel::toggleStart,
                onWindowTap = viewModel::toggleWindow,
                onQuick = viewModel::toggleQuick,
                onLaunchPinned = { pkg -> viewModel.closeOverlays(); onLaunch(pkg) },
                onToggleMaximize = viewModel::toggleMaximizeWindow,
                onMinimize = viewModel::minimizeWindow,
                onCloseWindow = viewModel::closeWindow,
                unreadNotifications = unreadNotifications,
                onNotifications = { openBuiltin(WindowKind.NOTIFICATIONS) },
            )
        }

        AnimatedVisibility(
            visible = startOpen,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = TASKBAR_HEIGHT + 8.dp),
            enter = fadeIn(tween(160)) + slideInVertically(tween(160)) { it / 6 },
            exit = fadeOut(tween(120)) + slideOutVertically(tween(120)) { it / 6 },
        ) {
            StartMenu(
                apps = apps,
                icons = icons,
                recentApps = recentApps,
                onOpenBuiltin = ::openBuiltin,
                onLaunch = { pkg -> viewModel.closeOverlays(); onLaunch(pkg) },
                onTogglePin = viewModel::togglePin,
                onCloseAll = viewModel::closeAllWindows,
            )
        }

        if (quickOpen) {
            QuickSettingsPanel(
                theme = prefs.theme,
                battery = battery,
                network = network,
                onTheme = viewModel::setTheme,
                onNextWallpaper = viewModel::nextWallpaper,
                onOpenSettings = { openBuiltin(WindowKind.SETTINGS) },
                onCloseAll = viewModel::closeAllWindows,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 8.dp, bottom = TASKBAR_HEIGHT + 8.dp),
            )
        }

        contextAt?.let { position ->
            Box(modifier = Modifier.offset { IntOffset(position.x.roundToInt(), position.y.roundToInt()) }) {
                DropdownMenu(expanded = true, onDismissRequest = { contextAt = null }) {
                    DropdownMenuItem(
                        text = { Text("Trocar papel de parede") },
                        onClick = { viewModel.nextWallpaper(); contextAt = null },
                    )
                    DropdownMenuItem(
                        text = { Text("Abrir ajustes") },
                        onClick = { openBuiltin(WindowKind.SETTINGS); contextAt = null },
                    )
                    DropdownMenuItem(
                        text = { Text("Fechar todas as janelas") },
                        onClick = { viewModel.closeAllWindows(); contextAt = null },
                    )
                }
            }
        }
    }
}

/**
 * Traduz a tecla em ação pela tabela [ShortcutMap] e a executa. Retorna true se a tecla foi consumida.
 * Os atalhos são internos ao Deskora: combinações reservadas pelo sistema não chegam aqui.
 */
private fun handleShortcut(
    event: KeyEvent,
    viewModel: DesktopViewModel,
    openBuiltin: (WindowKind) -> Unit,
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    val action = ShortcutMap.resolve(
        keyCode = event.nativeKeyEvent.keyCode,
        ctrl = event.isCtrlPressed,
        alt = event.isAltPressed,
        meta = event.isMetaPressed,
        shift = event.isShiftPressed,
    ) ?: return false

    when (action) {
        ShortcutAction.CLOSE_OVERLAYS -> viewModel.closeOverlays()
        ShortcutAction.CYCLE_WINDOWS -> viewModel.cycleWindows()
        ShortcutAction.CLOSE_TOP_WINDOW -> viewModel.closeTopWindow()
        ShortcutAction.MINIMIZE_TOP_WINDOW -> viewModel.minimizeTopWindow()
        ShortcutAction.TOGGLE_MAXIMIZE_TOP_WINDOW -> viewModel.toggleMaximizeTopWindow()
        ShortcutAction.MAXIMIZE_TOP_WINDOW -> viewModel.maximizeTopWindow()
        ShortcutAction.RESTORE_TOP_WINDOW -> viewModel.restoreTopWindow()
        ShortcutAction.SNAP_LEFT_TOP_WINDOW -> viewModel.snapTopWindow(Placement.SNAP_LEFT)
        ShortcutAction.SNAP_RIGHT_TOP_WINDOW -> viewModel.snapTopWindow(Placement.SNAP_RIGHT)
        ShortcutAction.SHOW_DESKTOP -> viewModel.minimizeAllWindows()
        ShortcutAction.TOGGLE_START -> viewModel.toggleStart()
        ShortcutAction.OPEN_FILES -> openBuiltin(WindowKind.FILES)
        ShortcutAction.OPEN_SETTINGS -> openBuiltin(WindowKind.SETTINGS)
    }
    return true
}
