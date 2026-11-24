package com.example.desktopenv.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.desktopenv.data.BatteryInfo
import com.example.desktopenv.data.NetworkKind
import com.example.desktopenv.data.ThemeChoice

@Composable
fun QuickSettingsPanel(
    theme: ThemeChoice,
    battery: BatteryInfo,
    network: NetworkKind,
    onTheme: (ThemeChoice) -> Unit,
    onNextWallpaper: () -> Unit,
    onOpenSettings: () -> Unit,
    onCloseAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .width(300.dp)
            .pointerInput(Unit) { detectTapGestures(onTap = {}) },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        shadowElevation = 16.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Configurações rápidas", style = MaterialTheme.typography.titleMedium)
            val charging = if (battery.charging) " (carregando)" else ""
            Text(
                text = "Rede: ${network.label}   ·   Bateria: ${battery.level}%$charging",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text("Tema", style = MaterialTheme.typography.labelLarge)
            ThemeChips(current = theme, onSelect = onTheme)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onNextWallpaper) { Text("Papel de parede") }
                OutlinedButton(onClick = onOpenSettings) { Text("Ajustes") }
            }
            Button(onClick = onCloseAll, modifier = Modifier.fillMaxWidth()) {
                Text("Fechar todas as janelas")
            }
        }
    }
}
