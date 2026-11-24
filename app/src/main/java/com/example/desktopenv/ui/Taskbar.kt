package com.example.desktopenv.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.desktopenv.data.AppEntry
import com.example.desktopenv.data.BatteryInfo
import com.example.desktopenv.data.NetworkKind
import com.example.desktopenv.model.DesktopWindow
import com.example.desktopenv.model.Placement
import com.example.desktopenv.model.WindowsState
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val TASKBAR_HEIGHT = 56.dp

/**
 * Barra de tarefas: Iniciar, apps fixados, janelas abertas (com menu de contexto),
 * e indicadores de rede, bateria e relógio.
 *
 * Toque no botão de uma janela alterna entre mostrar e minimizar. Segurar (ou clique
 * direito com mouse) abre o menu com maximizar, minimizar e fechar.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Taskbar(
    windows: WindowsState,
    pinnedApps: List<AppEntry>,
    icons: Map<String, ImageBitmap>,
    startOpen: Boolean,
    quickOpen: Boolean,
    battery: BatteryInfo,
    network: NetworkKind,
    clock: LocalDateTime,
    onStart: () -> Unit,
    onWindowTap: (Long) -> Unit,
    onQuick: () -> Unit,
    onLaunchPinned: (String) -> Unit,
    onToggleMaximize: (Long) -> Unit,
    onMinimize: (Long) -> Unit,
    onCloseWindow: (Long) -> Unit,
    unreadNotifications: Int,
    onNotifications: () -> Unit,
) {
    val timeFormat = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val dateFormat = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(TASKBAR_HEIGHT),
        color = colors.surface.copy(alpha = 0.94f),
        shadowElevation = 12.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Botão Iniciar
            Surface(
                onClick = onStart,
                shape = MaterialTheme.shapes.medium,
                color = if (startOpen) colors.primary else colors.surfaceVariant,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Iniciar",
                        tint = if (startOpen) colors.onPrimary else colors.onSurface,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Iniciar",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (startOpen) colors.onPrimary else colors.onSurface,
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Apps fixados: um toque abre o app
                items(pinnedApps, key = { "pin:${it.packageName}" }) { app ->
                    Surface(
                        onClick = { onLaunchPinned(app.packageName) },
                        shape = MaterialTheme.shapes.medium,
                        color = colors.surfaceVariant,
                        modifier = Modifier
                            .size(40.dp)
                            .semantics { contentDescription = "Abrir ${app.label}" },
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            val bitmap = icons[app.packageName]
                            if (bitmap != null) {
                                Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(28.dp))
                            } else {
                                Text(
                                    text = app.label.take(1).uppercase(),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                }

                // Janelas abertas
                items(windows.windows, key = { "win:${it.id}" }) { window ->
                    WindowTaskbarButton(
                        window = window,
                        focused = windows.topWindowId == window.id,
                        onTap = { onWindowTap(window.id) },
                        onToggleMaximize = { onToggleMaximize(window.id) },
                        onMinimize = { onMinimize(window.id) },
                        onClose = { onCloseWindow(window.id) },
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Notificações não lidas: toque abre a central
            Surface(
                onClick = onNotifications,
                shape = MaterialTheme.shapes.medium,
                color = if (unreadNotifications > 0) colors.primaryContainer else colors.surfaceVariant,
                modifier = Modifier.semantics {
                    contentDescription = if (unreadNotifications > 0) {
                        "Notificações: $unreadNotifications não lidas"
                    } else {
                        "Notificações"
                    }
                },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = if (unreadNotifications > 0) colors.onPrimaryContainer else colors.onSurface,
                    )
                    if (unreadNotifications > 0) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (unreadNotifications > 99) "99+" else unreadNotifications.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.onPrimaryContainer,
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            // Indicadores e relógio: abre as configurações rápidas
            Surface(
                onClick = onQuick,
                shape = MaterialTheme.shapes.medium,
                color = if (quickOpen) colors.primary else colors.surfaceVariant,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    val charging = if (battery.charging) " (carregando)" else ""
                    Text(
                        text = "${network.label}  ·  ${battery.level}%$charging",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (quickOpen) colors.onPrimary else colors.onSurfaceVariant,
                    )
                    Text(
                        text = "${clock.format(timeFormat)}  ${clock.format(dateFormat)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (quickOpen) colors.onPrimary else colors.onSurface,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WindowTaskbarButton(
    window: DesktopWindow,
    focused: Boolean,
    onTap: () -> Unit,
    onToggleMaximize: () -> Unit,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var menuOpen by remember(window.id) { mutableStateOf(false) }

    Box {
        Surface(
            modifier = Modifier.combinedClickable(
                onClick = onTap,
                onLongClick = { menuOpen = true },
            ),
            shape = MaterialTheme.shapes.medium,
            color = when {
                focused -> colors.primaryContainer
                window.minimized -> colors.surface
                else -> colors.surfaceVariant
            },
        ) {
            Text(
                text = window.kind.title,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                color = if (window.minimized) colors.onSurfaceVariant else colors.onSurface,
            )
        }

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = {
                    Text(if (window.placement == Placement.MAXIMIZED) "Restaurar" else "Maximizar")
                },
                onClick = { menuOpen = false; onToggleMaximize() },
            )
            DropdownMenuItem(
                text = { Text("Minimizar") },
                onClick = { menuOpen = false; onMinimize() },
            )
            DropdownMenuItem(
                text = { Text("Fechar") },
                onClick = { menuOpen = false; onClose() },
            )
        }
    }
}
