package com.example.desktopenv.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.desktopenv.data.AppEntry
import com.example.desktopenv.model.WindowKind

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StartMenu(
    apps: List<AppEntry>,
    icons: Map<String, ImageBitmap>,
    onOpenBuiltin: (WindowKind) -> Unit,
    onLaunch: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onCloseAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, query) {
        apps.filter { query.isBlank() || it.label.contains(query, ignoreCase = true) }
    }

    Surface(
        modifier = modifier
            .width(360.dp)
            .height(500.dp)
            // Consome toques internos para que o fundo não feche o menu.
            .pointerInput(Unit) { detectTapGestures(onTap = {}) },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        shadowElevation = 16.dp,
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("Pesquisar aplicativos") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))
            Text("Sistema", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            // Rolável na horizontal: são seis itens e nem todos cabem em telas estreitas.
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(WindowKind.entries) { kind ->
                    AppTile(
                        label = kind.title,
                        bitmap = null,
                        vector = kind.vector(),
                        onClick = { onOpenBuiltin(kind) },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Aplicativos  ·  toque longo fixa ou remove da área de trabalho", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(4.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(84.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    AppTile(
                        label = app.label,
                        bitmap = icons[app.packageName],
                        vector = null,
                        onClick = { onLaunch(app.packageName) },
                        onLongClick = { onTogglePin(app.packageName) },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${filtered.size} aplicativos", style = MaterialTheme.typography.labelSmall)
                TextButton(onClick = onCloseAll) { Text("Fechar janelas") }
            }
        }
    }
}
