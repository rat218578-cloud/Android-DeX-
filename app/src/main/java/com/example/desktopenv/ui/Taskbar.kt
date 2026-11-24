package com.example.desktopenv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.desktopenv.data.BatteryInfo
import com.example.desktopenv.data.NetworkKind
import com.example.desktopenv.model.WindowsState
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val TASKBAR_HEIGHT = 56.dp

@Composable
fun Taskbar(
    windows: WindowsState,
    startOpen: Boolean,
    quickOpen: Boolean,
    battery: BatteryInfo,
    network: NetworkKind,
    clock: LocalDateTime,
    onStart: () -> Unit,
    onWindowTap: (Long) -> Unit,
    onQuick: () -> Unit,
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

            // Janelas abertas: toque alterna entre mostrar, focar e minimizar
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items(windows.windows, key = { it.id }) { window ->
                    val focused = windows.topWindowId == window.id
                    Surface(
                        onClick = { onWindowTap(window.id) },
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
