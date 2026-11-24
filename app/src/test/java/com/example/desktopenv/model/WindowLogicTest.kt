package com.example.desktopenv.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowLogicTest {

    private val areaW = 1000f
    private val areaH = 1800f
    private val density = 1f

    private fun openedSettings(): WindowsState =
        WindowLogic.open(WindowsState(), WindowKind.SETTINGS, areaW, areaH, density)

    @Test
    fun opening_same_kind_twice_focuses_instead_of_duplicating() {
        val first = openedSettings()
        val second = WindowLogic.open(first, WindowKind.SETTINGS, areaW, areaH, density)

        assertEquals(1, second.windows.size)
        assertTrue(second.windows.single().z > first.windows.single().z)
    }

    @Test
    fun new_window_is_centered_inside_area_and_on_top() {
        val state = openedSettings()
        val window = state.windows.single()

        assertEquals(state.windows.single().id, state.topWindowId)
        assertTrue(window.x >= 0f && window.x + window.width <= areaW + 0.5f)
        assertTrue(window.y >= 0f && window.y + window.height <= areaH + 0.5f)
    }

    @Test
    fun taskbar_toggle_minimizes_the_top_window_and_restores_it_on_second_tap() {
        val withTwo = WindowLogic.open(openedSettings(), WindowKind.CALENDAR, areaW, areaH, density)
        val calendarId = withTwo.windows.first { it.kind == WindowKind.CALENDAR }.id

        val minimized = WindowLogic.taskbarToggle(withTwo, calendarId)
        assertTrue(minimized.windows.first { it.id == calendarId }.minimized)

        val restored = WindowLogic.taskbarToggle(minimized, calendarId)
        assertFalse(restored.windows.first { it.id == calendarId }.minimized)
        assertEquals(calendarId, restored.topWindowId)
    }

    @Test
    fun taskbar_toggle_focuses_a_background_window_instead_of_minimizing_it() {
        val withTwo = WindowLogic.open(openedSettings(), WindowKind.CALENDAR, areaW, areaH, density)
        val settingsId = withTwo.windows.first { it.kind == WindowKind.SETTINGS }.id

        val result = WindowLogic.taskbarToggle(withTwo, settingsId)

        assertFalse(result.windows.first { it.id == settingsId }.minimized)
        assertEquals(settingsId, result.topWindowId)
    }

    @Test
    fun close_removes_only_the_target_window() {
        val withTwo = WindowLogic.open(openedSettings(), WindowKind.RECENT, areaW, areaH, density)
        val settingsId = withTwo.windows.first { it.kind == WindowKind.SETTINGS }.id

        val result = WindowLogic.close(withTwo, settingsId)

        assertEquals(listOf(WindowKind.RECENT), result.windows.map { it.kind })
    }

    @Test
    fun cycle_sends_the_bottom_visible_window_to_front() {
        val withTwo = WindowLogic.open(openedSettings(), WindowKind.CALENDAR, areaW, areaH, density)
        val settingsId = withTwo.windows.first { it.kind == WindowKind.SETTINGS }.id

        val cycled = WindowLogic.cycle(withTwo)

        assertEquals(settingsId, cycled.topWindowId)
    }

    @Test
    fun cycle_does_nothing_with_fewer_than_two_visible_windows() {
        val single = openedSettings()
        assertEquals(single, WindowLogic.cycle(single))
    }

    @Test
    fun move_keeps_title_bar_inside_the_area() {
        val state = openedSettings()
        val id = state.windows.single().id

        val far = WindowLogic.move(state, id, dx = -5000f, dy = -5000f, areaW = areaW, areaH = areaH, minVisible = 40f)
        val window = far.windows.single()
        assertEquals(0f, window.x, 0.001f)
        assertEquals(0f, window.y, 0.001f)

        val down = WindowLogic.move(state, id, dx = 0f, dy = 99999f, areaW = areaW, areaH = areaH, minVisible = 40f)
        assertEquals(areaH - 40f, down.windows.single().y, 0.001f)
    }

    @Test
    fun resize_respects_minimum_size() {
        val state = openedSettings()
        val id = state.windows.single().id

        val shrunk = WindowLogic.resize(
            state, id, dw = -10000f, dh = -10000f,
            minW = 240f, minH = 160f, areaW = areaW, areaH = areaH,
        )
        assertEquals(240f, shrunk.windows.single().width, 0.001f)
        assertEquals(160f, shrunk.windows.single().height, 0.001f)
    }

    @Test
    fun close_all_leaves_an_empty_desktop_with_no_top_window() {
        val state = WindowLogic.closeAll(openedSettings())
        assertTrue(state.windows.isEmpty())
        assertEquals(null, state.topWindowId)
    }

    @Test
    fun recent_list_moves_repeated_app_to_front_without_duplicates() {
        val list = RecentList.push(listOf("a.app", "b.app", "c.app"), "b.app")
        assertEquals(listOf("b.app", "a.app", "c.app"), list)
    }

    @Test
    fun recent_list_is_capped() {
        var list: List<String> = emptyList()
        repeat(20) { list = RecentList.push(list, "pkg$it") }
        assertEquals(RecentList.MAX_ITEMS, list.size)
        assertEquals("pkg19", list.first())
    }
}
