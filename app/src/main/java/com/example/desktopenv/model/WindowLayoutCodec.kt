package com.example.desktopenv.model

import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Converte o estado das janelas em texto para persistência e de volta.
 *
 * Formato: a primeira linha é o cabeçalho `v1|nextId|nextZ`; cada janela ocupa uma linha
 * com campos separados por `|`. O payload é URL-codificado, então nunca contém `|`.
 * Linhas inválidas são descartadas: dados corrompidos não impedem o app de abrir.
 */
object WindowLayoutCodec {

    private const val VERSION = "v1"
    private const val SEP = "|"

    fun encode(state: WindowsState): String {
        val header = listOf(VERSION, state.nextId, state.nextZ).joinToString(SEP)
        val rows = state.windows.map { w ->
            val r = w.restoreBounds
            listOf(
                w.id,
                w.kind.name,
                w.x,
                w.y,
                w.width,
                w.height,
                w.z,
                if (w.minimized) 1 else 0,
                w.placement.name,
                if (r != null) 1 else 0,
                r?.x ?: 0f,
                r?.y ?: 0f,
                r?.width ?: 0f,
                r?.height ?: 0f,
                URLEncoder.encode(w.payload, Charsets.UTF_8.name()),
            ).joinToString(SEP)
        }
        return (listOf(header) + rows).joinToString("\n")
    }

    fun decode(raw: String): WindowsState {
        if (raw.isBlank()) return WindowsState()
        val lines = raw.lines()
        val header = lines.first().split(SEP)
        if (header.size != 3 || header[0] != VERSION) return WindowsState()

        val windows = lines.drop(1).mapNotNull { parseWindow(it) }
            .distinctBy { it.id }
        // Os contadores nunca podem ficar atrás das janelas salvas, senão ids colidiriam.
        val nextId = maxOf(header[1].toLongOrNull() ?: 1L, (windows.maxOfOrNull { it.id } ?: 0L) + 1L)
        val nextZ = maxOf(header[2].toIntOrNull() ?: 1, (windows.maxOfOrNull { it.z } ?: 0) + 1)
        return WindowsState(windows = windows, nextId = nextId, nextZ = nextZ)
    }

    private fun parseWindow(line: String): DesktopWindow? {
        val f = line.split(SEP)
        if (f.size != 15) return null
        return try {
            val kind = WindowKind.valueOf(f[1])
            val placement = Placement.valueOf(f[8])
            val restore = if (f[9] == "1") {
                Bounds(f[10].toFloat(), f[11].toFloat(), f[12].toFloat(), f[13].toFloat())
            } else {
                null
            }
            val window = DesktopWindow(
                id = f[0].toLong(),
                kind = kind,
                x = f[2].toFloat(),
                y = f[3].toFloat(),
                width = f[4].toFloat(),
                height = f[5].toFloat(),
                minimized = f[7] == "1",
                z = f[6].toInt(),
                payload = URLDecoder.decode(f[14], Charsets.UTF_8.name()),
                placement = placement,
                restoreBounds = restore,
            )
            // Janela que não é flutuante precisa do retângulo para restaurar.
            if (placement != Placement.FLOATING && restore == null) window.copy(placement = Placement.FLOATING) else window
        } catch (e: IllegalArgumentException) {
            null
        } catch (e: NumberFormatException) {
            null
        }
    }
}
