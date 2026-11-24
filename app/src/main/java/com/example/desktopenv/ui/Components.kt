package com.example.desktopenv.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.desktopenv.data.ThemeChoice
import com.example.desktopenv.model.WindowKind

/** Ícone vetorial de cada janela interna, usado no menu Iniciar e na área de trabalho. */
fun WindowKind.vector(): ImageVector = when (this) {
    WindowKind.SETTINGS -> Icons.Default.Settings
    WindowKind.CALENDAR -> Icons.Default.Info
    WindowKind.RECENT -> Icons.Default.Star
    WindowKind.NOTIFICATIONS -> Icons.Default.Notifications
    WindowKind.MEDIA -> Icons.Default.PlayArrow
    WindowKind.FILES -> Icons.Default.List
}

fun ThemeChoice.label(): String = when (this) {
    ThemeChoice.SYSTEM -> "Sistema"
    ThemeChoice.LIGHT -> "Claro"
    ThemeChoice.DARK -> "Escuro"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppTile(
    label: String,
    bitmap: ImageBitmap?,
    vector: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .width(84.dp)
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            when {
                bitmap != null -> Image(
                    bitmap = bitmap,
                    contentDescription = label,
                    modifier = Modifier.size(40.dp),
                )
                vector != null -> Icon(
                    imageVector = vector,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                )
                else -> Text(
                    text = label.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        Spacer(Modifier.size(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun ThemeChips(current: ThemeChoice, onSelect: (ThemeChoice) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemeChoice.entries.forEach { choice ->
            FilterChip(
                selected = current == choice,
                onClick = { onSelect(choice) },
                label = { Text(choice.label()) },
            )
        }
    }
}
