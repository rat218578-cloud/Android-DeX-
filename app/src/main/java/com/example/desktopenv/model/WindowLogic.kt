package com.example.desktopenv.model

import kotlin.math.max

/**
 * Tipos de janela interna. [allowsMultiple] indica se podem existir várias instâncias
 * ao mesmo tempo (cada uma com seu próprio [DesktopWindow.payload]).
 */
enum class WindowKind(val title: String, val allowsMultiple: Boolean = false) {
    SETTINGS("Configurações"),
    CALENDAR("Calendário"),
    RECENT("Recentes"),
    NOTIFICATIONS("Notificações"),
    MEDIA("Mídia"),
    FILES("Arquivos", allowsMultiple = true),
}

/** Como a janela está posicionada. Maximizada e encaixada derivam o retângulo da área atual. */
enum class Placement { FLOATING, MAXIMIZED, SNAP_LEFT, SNAP_RIGHT }

data class Bounds(val x: Float, val y: Float, val width: Float, val height: Float)

data class DesktopWindow(
    val id: Long,
    val kind: WindowKind,
    /** Posição e tamanho só valem para janelas [Placement.FLOATING]. */
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val minimized: Boolean,
    val z: Int,
    /** Dado extra da janela, como a pasta raiz de uma janela de arquivos. Vazio se não usado. */
    val payload: String = "",
    val placement: Placement = Placement.FLOATING,
    /** Retângulo flutuante guardado enquanto a janela está maximizada ou encaixada. */
    val restoreBounds: Bounds? = null,
) {
    /** Retângulo efetivo na área de trabalho atual. */
    fun frame(areaW: Float, areaH: Float): Bounds = when (placement) {
        Placement.FLOATING -> {
            val w = width.coerceAtMost(areaW)
            val h = height.coerceAtMost(areaH)
            Bounds(
                x = x.coerceIn(0f, max(0f, areaW - w)),
                y = y.coerceIn(0f, max(0f, areaH - h)),
                width = w,
                height = h,
            )
        }
        Placement.MAXIMIZED -> Bounds(0f, 0f, areaW, areaH)
        Placement.SNAP_LEFT -> Bounds(0f, 0f, areaW / 2f, areaH)
        Placement.SNAP_RIGHT -> Bounds(areaW / 2f, 0f, areaW / 2f, areaH)
    }
}

data class WindowsState(
    val windows: List<DesktopWindow> = emptyList(),
    val nextId: Long = 1L,
    val nextZ: Int = 1,
) {
    /** Janela visível mais ao topo, ou null se todas estiverem minimizadas. */
    val topWindowId: Long?
        get() = windows.filter { !it.minimized }.maxByOrNull { it.z }?.id
}

/**
 * Lógica pura de gerenciamento de janelas. Não depende de Android,
 * por isso pode ser testada na JVM.
 */
object WindowLogic {

    fun open(
        state: WindowsState,
        kind: WindowKind,
        areaW: Float,
        areaH: Float,
        density: Float,
        payload: String = "",
    ): WindowsState {
        // Tipos únicos: abrir de novo apenas foca/restaura a existente.
        if (!kind.allowsMultiple) {
            state.windows.firstOrNull { it.kind == kind }?.let { return focus(state, it.id) }
        }

        val w = minOf(areaW * 0.8f, 560f * density).coerceAtLeast(260f * density)
        val h = minOf(areaH * 0.7f, 520f * density).coerceAtLeast(220f * density)
        val cascade = (state.windows.size % 6) * 28f * density
        val x = ((areaW - w) / 2f + cascade).coerceIn(0f, max(0f, areaW - w))
        val y = ((areaH - h) / 2f + cascade).coerceIn(0f, max(0f, areaH - h))

        val window = DesktopWindow(
            id = state.nextId,
            kind = kind,
            x = x,
            y = y,
            width = w,
            height = h,
            minimized = false,
            z = state.nextZ,
            payload = payload,
        )
        return state.copy(
            windows = state.windows + window,
            nextId = state.nextId + 1,
            nextZ = state.nextZ + 1,
        )
    }

    /** Traz a janela ao topo e a restaura se estiver minimizada. */
    fun focus(state: WindowsState, id: Long): WindowsState {
        if (state.windows.none { it.id == id }) return state
        return state.copy(
            windows = state.windows.map {
                if (it.id == id) it.copy(z = state.nextZ, minimized = false) else it
            },
            nextZ = state.nextZ + 1,
        )
    }

    fun minimize(state: WindowsState, id: Long): WindowsState =
        state.copy(windows = state.windows.map { if (it.id == id) it.copy(minimized = true) else it })

    fun close(state: WindowsState, id: Long): WindowsState =
        state.copy(windows = state.windows.filterNot { it.id == id })

    fun closeAll(state: WindowsState): WindowsState = state.copy(windows = emptyList())

    /** Comportamento da barra de tarefas: se a janela já está no topo, minimiza; senão, foca. */
    fun taskbarToggle(state: WindowsState, id: Long): WindowsState {
        val target = state.windows.firstOrNull { it.id == id } ?: return state
        return if (!target.minimized && state.topWindowId == id) minimize(state, id) else focus(state, id)
    }

    /** Mostrar área de trabalho: minimiza todas as janelas visíveis. Restaurar é feito pela barra de tarefas. */
    fun minimizeAll(state: WindowsState): WindowsState =
        state.copy(windows = state.windows.map { if (it.minimized) it else it.copy(minimized = true) })

    fun minimizeTop(state: WindowsState): WindowsState =
        state.topWindowId?.let { minimize(state, it) } ?: state

    fun closeTop(state: WindowsState): WindowsState =
        state.topWindowId?.let { close(state, it) } ?: state

    /** Alt+Tab: envia a janela visível mais ao fundo para o topo, alternando entre todas. */
    fun cycle(state: WindowsState): WindowsState {
        val visible = state.windows.filter { !it.minimized }.sortedBy { it.z }
        if (visible.size < 2) return state
        return focus(state, visible.first().id)
    }

    /** Define o payload de uma janela, como a pasta raiz de uma janela de arquivos. Não altera a ordem Z. */
    fun setPayload(state: WindowsState, id: Long, payload: String): WindowsState =
        updateWindow(state, id) { it.copy(payload = payload) }

    /** Maximiza a janela (ou a restaura, se já estiver maximizada). */
    fun toggleMaximize(state: WindowsState, id: Long): WindowsState {
        val target = state.windows.firstOrNull { it.id == id } ?: return state
        return if (target.placement == Placement.MAXIMIZED) restore(state, id) else maximize(state, id)
    }

    fun maximize(state: WindowsState, id: Long): WindowsState =
        focus(updateWindow(state, id) { toPlaced(it, Placement.MAXIMIZED) }, id)

    /** Encaixa a janela na metade esquerda ou direita. Outros valores de [side] são ignorados. */
    fun snap(state: WindowsState, id: Long, side: Placement): WindowsState {
        if (side != Placement.SNAP_LEFT && side != Placement.SNAP_RIGHT) return state
        return focus(updateWindow(state, id) { toPlaced(it, side) }, id)
    }

    /** Volta a janela ao modo flutuante, com o tamanho e a posição anteriores. */
    fun restore(state: WindowsState, id: Long): WindowsState =
        focus(updateWindow(state, id) { toFloating(it) }, id)

    /**
     * Decide o encaixe ao soltar uma janela arrastada: borda esquerda ou direita encaixa,
     * topo maximiza. Fora das bordas, a janela permanece flutuante.
     */
    fun edgeSnap(
        state: WindowsState,
        id: Long,
        areaW: Float,
        areaH: Float,
        threshold: Float,
    ): WindowsState {
        val target = state.windows.firstOrNull { it.id == id } ?: return state
        val f = target.frame(areaW, areaH)
        return when {
            f.x <= threshold -> snap(state, id, Placement.SNAP_LEFT)
            f.x + f.width >= areaW - threshold -> snap(state, id, Placement.SNAP_RIGHT)
            f.y <= threshold -> maximize(state, id)
            else -> state
        }
    }

    /** Move a janela, mantendo pelo menos [minVisible] px da barra de título dentro da área. */
    fun move(
        state: WindowsState,
        id: Long,
        dx: Float,
        dy: Float,
        areaW: Float,
        areaH: Float,
        minVisible: Float,
    ): WindowsState = updateWindow(state, id) { raw ->
        val w = toFloating(raw)
        w.copy(
            x = (w.x + dx).coerceIn(0f, max(0f, areaW - w.width)),
            y = (w.y + dy).coerceIn(0f, max(0f, areaH - minVisible)),
        )
    }

    fun resize(
        state: WindowsState,
        id: Long,
        dw: Float,
        dh: Float,
        minW: Float,
        minH: Float,
        areaW: Float,
        areaH: Float,
    ): WindowsState = updateWindow(state, id) { raw ->
        val w = toFloating(raw)
        w.copy(
            width = (w.width + dw).coerceIn(minW, max(minW, areaW - w.x)),
            height = (w.height + dh).coerceIn(minH, max(minH, areaH - w.y)),
        )
    }

    /** Converte para flutuante, usando o retângulo guardado. Não faz nada se já for flutuante. */
    private fun toFloating(w: DesktopWindow): DesktopWindow {
        if (w.placement == Placement.FLOATING) return w
        val b = w.restoreBounds ?: Bounds(w.x, w.y, w.width, w.height)
        return w.copy(
            x = b.x,
            y = b.y,
            width = b.width,
            height = b.height,
            placement = Placement.FLOATING,
            restoreBounds = null,
        )
    }

    /** Converte para [placement], guardando o retângulo flutuante para restaurar depois. */
    private fun toPlaced(w: DesktopWindow, placement: Placement): DesktopWindow {
        if (w.placement == placement) return w
        val base = if (w.placement == Placement.FLOATING) {
            Bounds(w.x, w.y, w.width, w.height)
        } else {
            w.restoreBounds ?: Bounds(w.x, w.y, w.width, w.height)
        }
        return w.copy(placement = placement, restoreBounds = base)
    }

    private fun updateWindow(
        state: WindowsState,
        id: Long,
        transform: (DesktopWindow) -> DesktopWindow,
    ): WindowsState = state.copy(
        windows = state.windows.map { if (it.id == id) transform(it) else it },
    )
}

/** Lista de apps abertos recentemente: sem duplicatas, mais recente primeiro, com limite. */
object RecentList {
    const val MAX_ITEMS = 8

    fun push(current: List<String>, packageName: String): List<String> =
        (listOf(packageName) + current.filter { it != packageName }).take(MAX_ITEMS)
}
