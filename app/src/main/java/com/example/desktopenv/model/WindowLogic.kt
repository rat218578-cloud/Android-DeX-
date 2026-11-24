package com.example.desktopenv.model

import kotlin.math.max

/** Janelas internas do desktop. Apps externos são abertos pelo Android, não embutidos. */
enum class WindowKind(val title: String) {
    SETTINGS("Configurações"),
    CALENDAR("Calendário"),
    RECENT("Recentes"),
}

data class DesktopWindow(
    val id: Long,
    val kind: WindowKind,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val minimized: Boolean,
    val z: Int,
)

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
    ): WindowsState {
        // Uma instância por tipo: abrir de novo apenas foca/restaura a existente.
        state.windows.firstOrNull { it.kind == kind }?.let { return focus(state, it.id) }

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

    /** Move a janela, mantendo pelo menos [minVisible] px da barra de título dentro da área. */
    fun move(
        state: WindowsState,
        id: Long,
        dx: Float,
        dy: Float,
        areaW: Float,
        areaH: Float,
        minVisible: Float,
    ): WindowsState = updateWindow(state, id) { w ->
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
    ): WindowsState = updateWindow(state, id) { w ->
        w.copy(
            width = (w.width + dw).coerceIn(minW, max(minW, areaW - w.x)),
            height = (w.height + dh).coerceIn(minH, max(minH, areaH - w.y)),
        )
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
