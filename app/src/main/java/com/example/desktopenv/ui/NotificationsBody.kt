package com.example.desktopenv.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.desktopenv.data.NotificationAccess
import com.example.desktopenv.data.NotificationStore
import com.example.desktopenv.model.NotificationRecord
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Texto exibido antes de o usuário conceder o acesso. Descreve exatamente o que o app faz. */
internal const val NOTIFICATION_ACCESS_EXPLANATION =
    "O Deskora lê o título e o texto das notificações de outros apps para mostrá-las aqui. " +
        "Esse processamento é local: nada é enviado a servidores. " +
        "O histórico fica só na memória e some quando o app é fechado. " +
        "Notificações fixas (como as de música) e as do próprio Deskora não entram no histórico. " +
        "O Deskora não responde nem remove notificações. " +
        "Você pode revogar o acesso a qualquer momento em Ajustes."

/** Janela de notificações: explica a permissão, mostra o histórico e permite limpá-lo. */
@Composable
fun NotificationsBody(
    labelOf: (String) -> String,
    onClear: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val history by NotificationStore.items.collectAsState()
    var granted by remember { mutableStateOf(NotificationAccess.isGranted(context)) }

    // Ao voltar de Ajustes, a permissão é verificada de novo.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = NotificationAccess.isGranted(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (granted) {
            Text(
                text = "Acesso concedido. As notificações aparecem aqui enquanto o Deskora estiver aberto.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text("Acesso às notificações não concedido", style = MaterialTheme.typography.titleSmall)
            Text(
                text = NOTIFICATION_ACCESS_EXPLANATION,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = {
                runCatching { context.startActivity(NotificationAccess.settingsIntent()) }
                    .onFailure { Toast.makeText(context, "Não foi possível abrir Ajustes", Toast.LENGTH_SHORT).show() }
            }) { Text("Abrir acesso às notificações") }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("${history.size} no histórico", style = MaterialTheme.typography.labelLarge)
            TextButton(onClick = onClear, enabled = history.isNotEmpty()) { Text("Limpar") }
        }

        if (history.isEmpty()) {
            Text(
                text = "Nenhuma notificação por enquanto.",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(history, key = { it.key }) { record ->
                    NotificationRow(record, labelOf(record.packageName))
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(record: NotificationRecord, appLabel: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "$appLabel  ·  ${formatTime(record.postedAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (record.title.isNotEmpty()) {
                Text(record.title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
            }
            if (record.text.isNotEmpty()) {
                Text(record.text, style = MaterialTheme.typography.bodySmall, maxLines = 3)
            }
        }
    }
}

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun formatTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalTime().format(timeFormatter)
