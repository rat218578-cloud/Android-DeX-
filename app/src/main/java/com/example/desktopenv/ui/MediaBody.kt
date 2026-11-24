package com.example.desktopenv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.unit.dp
import com.example.desktopenv.data.NotificationAccess
import com.example.desktopenv.model.MediaInfo

/** Texto exibido antes de o usuário conceder o acesso. */
internal const val MEDIA_ACCESS_EXPLANATION =
    "Para controlar a música e os vídeos de outros apps, o Deskora precisa do mesmo acesso " +
        "usado pelas notificações. Com ele, o app só vê a faixa em reprodução e envia comandos de " +
        "tocar, pausar e trocar de faixa. Nada é enviado a servidores. " +
        "O volume funciona sem essa permissão."

/**
 * Janela de mídia. A tela só mostra e envia comandos; o estado vem de [com.example.desktopenv.data.MediaRepository].
 */
@Composable
fun MediaBody(
    media: MediaInfo?,
    volumePercent: Int,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    onResume: () -> Unit,
) {
    val context = LocalContext.current
    val granted = rememberNotificationAccess(onResume = onResume)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!granted) {
            Text("Controle de mídia indisponível", style = MaterialTheme.typography.titleSmall)
            Text(
                text = MEDIA_ACCESS_EXPLANATION,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = {
                runCatching { context.startActivity(NotificationAccess.settingsIntent()) }
                    .onFailure { Toast.makeText(context, "Não foi possível abrir Ajustes", Toast.LENGTH_SHORT).show() }
            }) { Text("Abrir acesso") }
        }

        if (media == null) {
            Text("Nenhuma mídia em reprodução.", style = MaterialTheme.typography.bodyMedium)
        } else {
            Text(
                text = media.title.ifEmpty { "Sem título" },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
            )
            if (media.artist.isNotEmpty()) {
                Text(
                    text = media.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = onPrevious) { Text("⏮ Anterior") }
                Button(onClick = onPlayPause) { Text(if (media.playing) "⏸ Pausar" else "▶ Tocar") }
                OutlinedButton(onClick = onNext) { Text("⏭ Próxima") }
            }
        }

        Text("Volume: $volumePercent%", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            OutlinedButton(onClick = onVolumeDown) { Text("−") }
            OutlinedButton(onClick = onVolumeUp) { Text("+") }
        }
    }
}
