package com.example.desktopenv.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.desktopenv.data.AppEntry
import com.example.desktopenv.model.SearchIndex
import com.example.desktopenv.model.WindowKind

/** Palavras extras que ajudam a encontrar uma janela pela busca. */
private val WINDOW_KEYWORDS: Map<WindowKind, List<String>> = mapOf(
    WindowKind.SETTINGS to listOf("ajustes", "configuracoes", "tema", "wifi"),
    WindowKind.CALENDAR to listOf("agenda", "data"),
    WindowKind.RECENT to listOf("abertos", "historico"),
    WindowKind.NOTIFICATIONS to listOf("avisos", "alertas"),
    WindowKind.MEDIA to listOf("musica", "som", "player", "volume"),
    WindowKind.FILES to listOf("pastas", "documentos", "explorador"),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StartMenu(
    apps: List<AppEntry>,
    icons: Map<String, ImageBitmap>,
    recentApps: List<AppEntry>,
    onOpenBuiltin: (WindowKind) -> Unit,
    onLaunch: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onCloseAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }

    // Índice da busca: janelas do sistema e apps instalados. Chaves "window:" e "app:".
    val searchable = remember(apps) {
        WindowKind.entries.map { kind ->
            SearchIndex.Entry("window:${kind.name}", kind.title, WINDOW_KEYWORDS[kind].orEmpty())
        } + apps.map { app -> SearchIndex.Entry("app:${app.packageName}", app.label) }
    }
    val results = remember(query, searchable) { SearchIndex.search(query, searchable) }
    val searching = query.isNotBlank()

    fun run(entry: SearchIndex.Entry) {
        when {
            entry.key.startsWith("window:") ->
                onOpenBuiltin(WindowKind.valueOf(entry.key.removePrefix("window:")))
            entry.key.startsWith("app:") -> onLaunch(entry.key.removePrefix("app:"))
        }
    }

    Surface(
        modifier = modifier
            // Adapta-se a telas estreitas: no máximo 360 dp de largura e 80% da altura, limitado a 500 dp.
            .widthIn(min = 260.dp, max = 360.dp)
            .heightIn(max = 500.dp)
            .fillMaxHeight(0.8f)
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
                placeholder = { Text("Pesquisar apps e janelas") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    results.firstOrNull()?.let { run(it) }
                }),
                modifier = Modifier.fillMaxWidth(),
            )

            if (searching) {
                Spacer(Modifier.height(12.dp))
                if (results.isEmpty()) {
                    Text("Nada encontrado para \"$query\".", style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(results, key = { it.key }) { entry ->
                            Surface(
                                onClick = { run(entry) },
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(entry.label, style = MaterialTheme.typography.labelLarge)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (entry.key.startsWith("window:")) "Janela" else "Aplicativo",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
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

                if (recentApps.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text("Recentes", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(recentApps, key = { it.packageName }) { app ->
                            AppTile(
                                label = app.label,
                                bitmap = icons[app.packageName],
                                vector = null,
                                onClick = { onLaunch(app.packageName) },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    "Aplicativos  ·  toque longo fixa ou remove da área de trabalho",
                    style = MaterialTheme.typography.labelSmall,
                )
                Spacer(Modifier.height(4.dp))

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(84.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    items(apps, key = { it.packageName }) { app ->
                        AppTile(
                            label = app.label,
                            bitmap = icons[app.packageName],
                            vector = null,
                            onClick = { onLaunch(app.packageName) },
                            onLongClick = { onTogglePin(app.packageName) },
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (searching) "${results.size} resultados" else "${apps.size} aplicativos",
                    style = MaterialTheme.typography.labelSmall,
                )
                TextButton(onClick = onCloseAll) { Text("Fechar janelas") }
            }
        }
    }
}
