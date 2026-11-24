package com.example.desktopenv.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Casos de borda da lógica de janelas: ids inexistentes, janelas minimizadas e estado vazio. */
class WindowLogicEdgeCasesTest {

    private val areaW = 1000f
    private val areaH = 1800f
    private val density = 1f
    private val unknownId = 9_999L

    private fun open(state: WindowsState, kind: WindowKind): WindowsState =
        WindowLogic.open(state, kind, areaW, areaH, density)

    private fun idOf(state: WindowsState, kind: WindowKind): Long =
        state.windows.first { it.kind == kind }.id

    @Test
    fun focus_with_unknown_id_returns_the_same_state() {
        val state = open(WindowsState(), WindowKind.SETTINGS)
        assertEquals(state, WindowLogic.focus(state, unknownId))
    }

    @Test
    fun close_with_unknown_id_keeps_all_windows() {
        val state = open(open(WindowsState(), WindowKind.SETTINGS), WindowKind.CALENDAR)
        assertEquals(state.windows, WindowLogic.close(state, unknownId).windows)
    }

    @Test
    fun taskbar_toggle_with_unknown_id_changes_nothing() {
        val state = open(WindowsState(), WindowKind.SETTINGS)
        assertEquals(state, WindowLogic.taskbarToggle(state, unknownId))
    }

    @Test
    fun move_and_resize_with_unknown_id_change_nothing() {
        val state = open(WindowsState(), WindowKind.SETTINGS)
        val moved = WindowLogic.move(state, unknownId, 50f, 50f, areaW, areaH, minVisible = 40f)
        val resized = WindowLogic.resize(state, unknownId, 50f, 50f, 240f, 160f, areaW, areaH)
        assertEquals(state.windows, moved.windows)
        assertEquals(state.windows, resized.windows)
    }

    @Test
    fun minimize_top_and_close_top_do_nothing_when_no_window_is_visible() {
        val empty = WindowsState()
        assertEquals(empty, WindowLogic.minimizeTop(empty))
        assertEquals(empty, WindowLogic.closeTop(empty))

        val allMinimized = WindowLogic.minimize(open(WindowsState(), WindowKind.SETTINGS), 1L)
        assertEquals(allMinimized, WindowLogic.minimizeTop(allMinimized))
        assertEquals(allMinimized, WindowLogic.closeTop(allMinimized))
    }

    @Test
    fun close_top_ignores_minimized_windows_and_closes_the_visible_one() {
        var state = open(WindowsState(), WindowKind.SETTINGS)
        state = open(state, WindowKind.CALENDAR)
        val settingsId = idOf(state, WindowKind.SETTINGS)
        val calendarId = idOf(state, WindowKind.CALENDAR)
        state = WindowLogic.minimize(state, calendarId)

        val result = WindowLogic.closeTop(state)

        assertEquals(listOf(WindowKind.SETTINGS), result.windows.map { it.kind })
        assertEquals(null, result.topWindowId)
        assertTrue(result.windows.none { it.id == calendarId })
        assertTrue(result.windows.any { it.id == settingsId })
    }

    @Test
    fun cycle_skips_minimized_windows() {
        var state = open(WindowsState(), WindowKind.SETTINGS)
        state = open(state, WindowKind.CALENDAR)
        state = open(state, WindowKind.RECENT)
        state = WindowLogic.minimize(state, idOf(state, WindowKind.CALENDAR))
        val settingsId = idOf(state, WindowKind.SETTINGS)

        val cycled = WindowLogic.cycle(state)

        assertEquals(settingsId, cycled.topWindowId)
        assertTrue(cycled.windows.first { it.kind == WindowKind.CALENDAR }.minimized)
    }

    @Test
    fun close_all_removes_minimized_windows_too() {
        var state = open(WindowsState(), WindowKind.SETTINGS)
        state = WindowLogic.minimize(state, idOf(state, WindowKind.SETTINGS))

        val result = WindowLogic.closeAll(state)

        assertTrue(result.windows.isEmpty())
        assertNull(result.topWindowId)
    }

    @Test
    fun opening_a_minimized_kind_again_restores_it_without_duplicating() {
        var state = open(WindowsState(), WindowKind.SETTINGS)
        val id = idOf(state, WindowKind.SETTINGS)
        state = WindowLogic.minimize(state, id)

        val reopened = open(state, WindowKind.SETTINGS)

        assertEquals(1, reopened.windows.size)
        assertFalse(reopened.windows.single().minimized)
        assertEquals(id, reopened.topWindowId)
    }

    @Test
    fun ids_keep_increasing_after_windows_are_closed() {
        var state = open(WindowsState(), WindowKind.SETTINGS)
        val firstId = idOf(state, WindowKind.SETTINGS)
        state = WindowLogic.close(state, firstId)
        state = open(state, WindowKind.SETTINGS)

        assertTrue(idOf(state, WindowKind.SETTINGS) > firstId)
    }
}
