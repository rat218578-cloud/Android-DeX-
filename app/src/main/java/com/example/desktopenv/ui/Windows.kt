package com.example.desktopenv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.desktopenv.data.ThemeChoice
import com.example.desktopenv.model.Bounds
import com.example.desktopenv.model.CalendarGrid
import com.example.desktopenv.model.DesktopWindow
import com.example.desktopenv.model.Placement
import com.example.desktopenv.model.WindowKind
import com.example.desktopenv.ui.theme.Wallpapers
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Moldura de janela. A posição e o tamanho vêm de [frame], que já considera maximizar e encaixar.
 *
 * - Arrastar a barra de título move a janela; ao soltar numa borda, ela encaixa ou maximiza.
 * - Clique duplo na barra de título alterna entre maximizada e flutuante.
 * - A alça no canto inferior direito redimensiona apenas janelas flutuantes.
 */
@Composable
fun WindowHost(
    window: DesktopWindow,
    frame: Bounds,
    onFocus: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
    onResize: (Float, Float) -> Unit,
    onToggleMaximize: () -> Unit,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val floating = window.placement == Placement.FLOATING
    val corner = if (floating) 16.dp else 0.dp
    val shape = RoundedCornerShape(corner)
    val maximized = window.placement == Placement.MAXIMIZED
    val currentFocus by rememberUpdatedState(onFocus)
    val currentDrag by rememberUpdatedState(onDrag)
    val currentDragEnd by rememberUpdatedState(onDragEnd)
    val currentResize by rememberUpdatedState(onResize)
    val currentToggle by rememberUpdatedState(onToggleMaximize)

    Box(
        modifier = Modifier
            .offset { IntOffset(frame.x.roundToInt(), frame.y.roundToInt()) }
            .size(
                width = with(density) { frame.width.toDp() },
                height = with(density) { frame.height.toDp() },
            )
            .shadow(elevation = if (floating) 16.dp else 0.dp, shape = shape)
            .pointerInput(window.id) {
                detectTapGestures(onPress = {
                    currentFocus()
                    tryAwaitRelease()
                })
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), shape),
        ) {
            // Barra de título: arrastar move (e encaixa ao soltar); clique duplo maximiza/restaura
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .pointerInput(window.id) {
                        detectDragGestures(
                            onDragEnd = { currentDragEnd() },
                            onDragCancel = { currentDragEnd() },
                        ) { change, drag ->
                            change.consume()
                            currentDrag(drag.x, drag.y)
                        }
                    }
                    .pointerInput(window.id) {
                        detectTapGestures(onDoubleTap = { currentToggle() })
                    }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = window.kind.title,
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                )
                TextButton(
                    onClick = onToggleMaximize,
                    modifier = Modifier.semantics {
                        contentDescription = if (maximized) "Restaurar janela" else "Maximizar janela"
                    },
                ) { Text(if (maximized) "❐" else "□") }
                TextButton(
                    onClick = onMinimize,
                    modifier = Modifier.semantics { contentDescription = "Minimizar janela" },
                ) { Text("–") }
                TextButton(
                    onClick = onClose,
                    modifier = Modifier.semantics { contentDescription = "Fechar janela" },
                ) { Text("×") }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp),
            ) {
                content()
            }
        }

        // Alça de redimensionamento (canto inferior direito), só em janelas flutuantes
        if (floating) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(24.dp)
                    .pointerInput(window.id) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            currentResize(drag.x, drag.y)
                        }
                    },
            )
        }
    }
}

/** Conteúdo de cada janela interna. */
@Composable
fun WindowBody(
    kind: WindowKind,
    theme: ThemeChoice,
    wallpaper: Int,
    recent: List<String>,
    labelOf: (String) -> String,
    onTheme: (ThemeChoice) -> Unit,
    onWallpaper: (Int) -> Unit,
    onClearRecent: () -> Unit,
    onLaunch: (String) -> Unit,
) {
    when (kind) {
        WindowKind.SETTINGS -> SettingsBody(theme, wallpaper, onTheme, onWallpaper, onClearRecent)
        WindowKind.CALENDAR -> CalendarBody()
        WindowKind.RECENT -> RecentBody(recent, labelOf, onLaunch)
        // Corpos das fases 3 a 5; enquanto não existem, a janela mostra apenas que está em construção.
        WindowKind.NOTIFICATIONS, WindowKind.MEDIA, WindowKind.FILES -> Text(
            text = "Em construção.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun SettingsBody(
    theme: ThemeChoice,
    wallpaper: Int,
    onTheme: (ThemeChoice) -> Unit,
    onWallpaper: (Int) -> Unit,
    onClearRecent: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Tema", style = MaterialTheme.typography.titleSmall)
        ThemeChips(current = theme, onSelect = onTheme)

        Text("Papel de parede", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Wallpapers.forEachIndexed { index, brush ->
                val selectedBorder = if (index == wallpaper) 3.dp else 0.dp
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(brush)
                        .border(selectedBorder, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                        .clickable { onWallpaper(index) },
                )
            }
        }

        OutlinedButton(onClick = onClearRecent) { Text("Limpar recentes") }

        Text(
            text = "Janelas internas funcionam em qualquer celular com Android 12 ou superior. " +
                "Apps externos abrem em tela cheia ou em multijanela, conforme o sistema.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CalendarBody() {
    val locale = remember { Locale.forLanguageTag("pt-BR") }
    var month by remember { mutableStateOf(YearMonth.now()) }
    val today = remember { LocalDate.now() }
    val weeks = remember(month) { CalendarGrid.weeks(month.year, month.monthValue) }
    val monthTitle = remember(month) {
        month.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
            .replaceFirstChar { it.uppercaseChar() }
    }
    val weekdays = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { month = month.minusMonths(1) }) { Text("‹") }
            Text(
                text = monthTitle,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
            )
            TextButton(onClick = { month = month.plusMonths(1) }) { Text("›") }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdays.forEach { name ->
                Text(
                    text = name,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        weeks.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    val isToday = day != null &&
                        month.year == today.year &&
                        month.monthValue == today.monthValue &&
                        day == today.dayOfMonth
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (day != null) {
                            Text(
                                text = day.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (isToday) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentBody(
    recent: List<String>,
    labelOf: (String) -> String,
    onLaunch: (String) -> Unit,
) {
    if (recent.isEmpty()) {
        Text(
            text = "Nenhum aplicativo aberto ainda.",
            style = MaterialTheme.typography.bodyMedium,
        )
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(recent) { packageName ->
            Surface(
                onClick = { onLaunch(packageName) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    text = labelOf(packageName),
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
