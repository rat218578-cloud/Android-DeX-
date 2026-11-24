package com.example.desktopenv.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.desktopenv.data.FileRepository
import com.example.desktopenv.model.FileClipboard
import com.example.desktopenv.model.FileEntry
import com.example.desktopenv.model.FileKind
import com.example.desktopenv.model.FileRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Janela de arquivos.
 *
 * - Sem [rootUri], mostra as pastas autorizadas e permite adicionar ou remover uma.
 * - Com [rootUri], navega dentro dessa pasta. Nada fora das pastas autorizadas é acessível.
 */
@Composable
fun FilesBody(
    rootUri: String,
    folders: List<String>,
    repo: FileRepository,
    clipboard: FileClipboard?,
    onRootChange: (String) -> Unit,
    onAddFolder: (String) -> Unit,
    onRemoveFolder: (String) -> Unit,
    onClipboardChange: (FileClipboard?) -> Unit,
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { onAddFolder(it.toString()) }
    }

    if (rootUri.isEmpty()) {
        FolderRoots(
            folders = folders,
            repo = repo,
            onOpen = onRootChange,
            onRemove = onRemoveFolder,
            onPick = { picker.launch(null) },
        )
    } else {
        FileBrowser(
            rootUri = rootUri,
            repo = repo,
            clipboard = clipboard,
            onLeaveRoot = { onRootChange("") },
            onClipboardChange = onClipboardChange,
        )
    }
}

@Composable
private fun FolderRoots(
    folders: List<String>,
    repo: FileRepository,
    onOpen: (String) -> Unit,
    onRemove: (String) -> Unit,
    onPick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Pastas autorizadas", style = MaterialTheme.typography.titleSmall)
        Text(
            text = "O Deskora só acessa as pastas que você escolher aqui. Para remover o acesso, use Remover.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (folders.isEmpty()) {
            Text("Nenhuma pasta autorizada ainda.", style = MaterialTheme.typography.bodyMedium)
        }
        folders.forEach { uri ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = { onOpen(uri) },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = repo.rootName(uri),
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                    )
                }
                TextButton(onClick = { onRemove(uri) }) { Text("Remover") }
            }
        }
        Button(onClick = onPick) { Text("Adicionar pasta") }
    }
}

/** Dialogs da janela de arquivos. */
private sealed interface FileDialog {
    class Rename(val entry: FileEntry) : FileDialog
    object NewFolder : FileDialog
    class Delete(val entry: FileEntry) : FileDialog
    class Picture(val title: String, val bitmap: ImageBitmap) : FileDialog
    class TextPreview(val title: String, val content: String) : FileDialog
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileBrowser(
    rootUri: String,
    repo: FileRepository,
    clipboard: FileClipboard?,
    onLeaveRoot: () -> Unit,
    onClipboardChange: (FileClipboard?) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Pilha de pastas abertas dentro da raiz. A raiz é sempre a primeira.
    var path by remember(rootUri) { mutableStateOf(listOf(rootUri)) }
    val current = path.last()
    var entries by remember(current) { mutableStateOf<List<FileEntry>>(emptyList()) }
    var loadError by remember(current) { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }
    var selected by remember(current) { mutableStateOf<FileEntry?>(null) }
    var dialog by remember { mutableStateOf<FileDialog?>(null) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(current, reload) {
        val result = withContext(Dispatchers.IO) { repo.list(current) }
        result
            .onSuccess {
                entries = FileRules.sortForDisplay(it)
                loadError = null
            }
            .onFailure { loadError = it.message ?: "Não foi possível abrir a pasta" }
    }

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    /** Executa uma operação de escrita fora da thread principal e recarrega a pasta ao terminar. */
    fun perform(success: String, work: () -> Result<Unit>) {
        scope.launch {
            busy = true
            val result = withContext(Dispatchers.IO) { work() }
            busy = false
            result
                .onSuccess {
                    toast(success)
                    selected = null
                    reload++
                }
                .onFailure { toast(it.message ?: "A operação falhou") }
        }
    }

    fun openPicture(entry: FileEntry) {
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) { repo.loadImage(entry.uri).getOrNull() }
            if (bitmap == null) toast("Não foi possível exibir a imagem") else {
                dialog = FileDialog.Picture(entry.name, bitmap.asImageBitmap())
            }
        }
    }

    fun openText(entry: FileEntry) {
        scope.launch {
            val text = withContext(Dispatchers.IO) { repo.readText(entry.uri).getOrNull() }
            dialog = FileDialog.TextPreview(entry.name, text ?: "Não foi possível ler este arquivo.")
        }
    }

    fun open(entry: FileEntry) {
        if (entry.isDirectory) {
            path = path + entry.uri
            selected = null
            return
        }
        when (FileRules.kindOf(entry)) {
            FileKind.IMAGE -> openPicture(entry)
            FileKind.TEXT -> openText(entry)
            FileKind.FOLDER, FileKind.OTHER -> {
                if (!repo.openExternal(entry)) toast("Nenhum app disponível para este arquivo")
            }
        }
    }

    fun paste(clip: FileClipboard) {
        scope.launch {
            busy = true
            val failures = withContext(Dispatchers.IO) {
                clip.uris.count { source ->
                    val result = if (clip.move) repo.move(source, current) else repo.copy(source, current)
                    result.isFailure
                }
            }
            busy = false
            if (clip.move && failures == 0) onClipboardChange(null)
            toast(if (failures == 0) "Itens colados" else "$failures item(ns) não puderam ser colados")
            reload++
        }
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Cabeçalho: navegação
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = if (path.size == 1) repo.rootName(rootUri) else "${repo.rootName(rootUri)} · ${path.size - 1} nível(is)",
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onLeaveRoot) { Text("Pastas") }
        }

        // Ações sobre o item selecionado e a pasta atual
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                OutlinedButton(onClick = { path = path.dropLast(1); selected = null }, enabled = path.size > 1) {
                    Text("Acima")
                }
            }
            item {
                OutlinedButton(onClick = { selected?.let { open(it) } }, enabled = selected != null && !busy) {
                    Text("Abrir")
                }
            }
            item {
                OutlinedButton(onClick = { selected?.let { dialog = FileDialog.Rename(it) } }, enabled = selected != null && !busy) {
                    Text("Renomear")
                }
            }
            item {
                OutlinedButton(
                    onClick = { selected?.let { onClipboardChange(FileClipboard(listOf(it.uri), move = false)) } },
                    enabled = selected != null && !busy,
                ) { Text("Copiar") }
            }
            item {
                OutlinedButton(
                    onClick = { selected?.let { onClipboardChange(FileClipboard(listOf(it.uri), move = true)) } },
                    enabled = selected != null && !busy,
                ) { Text("Recortar") }
            }
            item {
                OutlinedButton(
                    onClick = { clipboard?.let { paste(it) } },
                    enabled = clipboard != null && !busy,
                ) { Text("Colar") }
            }
            item {
                OutlinedButton(onClick = { dialog = FileDialog.NewFolder }, enabled = !busy) { Text("Nova pasta") }
            }
            item {
                OutlinedButton(onClick = { selected?.let { dialog = FileDialog.Delete(it) } }, enabled = selected != null && !busy) {
                    Text("Excluir")
                }
            }
        }

        loadError?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }

        if (entries.isEmpty() && loadError == null) {
            Text("Pasta vazia.", style = MaterialTheme.typography.bodyMedium)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            items(entries, key = { it.uri }) { entry ->
                val isSelected = selected?.uri == entry.uri
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { selected = entry },
                            onDoubleClick = { open(entry) },
                        ),
                    shape = MaterialTheme.shapes.small,
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = if (entry.isDirectory) "${entry.name}/" else entry.name,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                        )
                        Text(
                            text = if (entry.isDirectory) "Pasta" else formatSize(entry.sizeBytes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    // Diálogos
    when (val d = dialog) {
        is FileDialog.Rename -> NameDialog(
            title = "Renomear",
            initial = d.entry.name,
            confirmLabel = "Renomear",
            onDismiss = { dialog = null },
            onConfirm = { newName ->
                dialog = null
                perform("Renomeado") { repo.rename(d.entry.uri, newName) }
            },
        )
        FileDialog.NewFolder -> NameDialog(
            title = "Nova pasta",
            initial = "",
            confirmLabel = "Criar",
            onDismiss = { dialog = null },
            onConfirm = { name ->
                dialog = null
                perform("Pasta criada") { repo.createFolder(current, name) }
            },
        )
        is FileDialog.Delete -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Excluir ${d.entry.name}?") },
            text = {
                Text(
                    if (d.entry.isDirectory) "A pasta e tudo que ela contém serão excluídos. Esta ação não pode ser desfeita."
                    else "O arquivo será excluído. Esta ação não pode ser desfeita.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = d.entry
                    dialog = null
                    perform("Excluído") { repo.delete(target.uri) }
                }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancelar") } },
        )
        is FileDialog.Picture -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(d.title) },
            text = { Image(bitmap = d.bitmap, contentDescription = d.title, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text("Fechar") } },
        )
        is FileDialog.TextPreview -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(d.title) },
            text = {
                Column(modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    Text(d.content, style = MaterialTheme.typography.bodySmall)
                    if (d.content.length >= FileRepository.MAX_TEXT_PREVIEW_BYTES / 2) {
                        Text(
                            "Visualização limitada aos primeiros 64 KB.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text("Fechar") } },
        )
        null -> Unit
    }
}

@Composable
private fun NameDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    val valid = FileRules.isValidName(name)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                isError = name.isNotEmpty() && !valid,
                supportingText = {
                    if (name.isNotEmpty() && !valid) Text("Use um nome sem / ou \\ e não use . ou ..")
                },
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim()) }, enabled = valid) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

/** Tamanho legível em pt-BR: B, KB, MB ou GB. */
internal fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = listOf("KB", "MB", "GB")
    var value = bytes / 1024.0
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return String.format(Locale("pt", "BR"), "%.1f %s", value, units[unit])
}
