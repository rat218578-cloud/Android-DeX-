package com.example.desktopenv.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Maximizar, encaixar, restaurar, arrastar até a borda e frame calculado na área atual. */
class WindowPlacementTest {

    private val areaW = 1000f
    private val areaH = 1800f
    private val density = 1f

    private fun settingsWindow(): Pair<WindowsState, Long> {
        val state = WindowLogic.open(WindowsState(), WindowKind.SETTINGS, areaW, areaH, density)
        return state to state.windows.single().id
    }

    private fun DesktopWindow.bounds(): Bounds = frame(areaW, areaH)

    @Test
    fun maximize_then_restore_returns_the_original_floating_bounds() {
        val (state, id) = settingsWindow()
        val before = state.windows.single().bounds()

        val maximized = WindowLogic.maximize(state, id)
        assertEquals(Placement.MAXIMIZED, maximized.windows.single().placement)
        assertEquals(Bounds(0f, 0f, areaW, areaH), maximized.windows.single().bounds())

        val restored = WindowLogic.restore(maximized, id)
        assertEquals(Placement.FLOATING, restored.windows.single().placement)
        assertEquals(before, restored.windows.single().bounds())
        assertEquals(null, restored.windows.single().restoreBounds)
    }

    @Test
    fun toggle_maximize_switches_between_maximized_and_floating() {
        val (state, id) = settingsWindow()
        val once = WindowLogic.toggleMaximize(state, id)
        val twice = WindowLogic.toggleMaximize(once, id)

        assertEquals(Placement.MAXIMIZED, once.windows.single().placement)
        assertEquals(Placement.FLOATING, twice.windows.single().placement)
    }

    @Test
    fun snap_left_and_right_take_half_of_the_area() {
        val (state, id) = settingsWindow()

        val left = WindowLogic.snap(state, id, Placement.SNAP_LEFT).windows.single()
        assertEquals(Bounds(0f, 0f, areaW / 2f, areaH), left.bounds())

        val right = WindowLogic.snap(state, id, Placement.SNAP_RIGHT).windows.single()
        assertEquals(Bounds(areaW / 2f, 0f, areaW / 2f, areaH), right.bounds())
    }

    @Test
    fun snap_ignores_non_snap_placements() {
        val (state, id) = settingsWindow()
        assertEquals(state, WindowLogic.snap(state, id, Placement.MAXIMIZED))
    }

    @Test
    fun dragging_a_maximized_window_restores_it_before_moving() {
        val (state, id) = settingsWindow()
        val floating = state.windows.single().bounds()
        val maximized = WindowLogic.maximize(state, id)

        val moved = WindowLogic.move(maximized, id, dx = 10f, dy = 10f, areaW = areaW, areaH = areaH, minVisible = 40f)
        val window = moved.windows.single()

        assertEquals(Placement.FLOATING, window.placement)
        assertEquals(floating.width, window.width, 0.001f)
        assertEquals(floating.x + 10f, window.x, 0.001f)
    }

    @Test
    fun resizing_a_snapped_window_restores_it_first() {
        val (state, id) = settingsWindow()
        val snapped = WindowLogic.snap(state, id, Placement.SNAP_LEFT)

        val resized = WindowLogic.resize(
            snapped, id, dw = 5f, dh = 5f, minW = 240f, minH = 160f, areaW = areaW, areaH = areaH,
        )
        assertEquals(Placement.FLOATING, resized.windows.single().placement)
    }

    @Test
    fun edge_snap_left_right_and_top_are_decided_by_drop_position() {
        val threshold = 8f
        val (state, id) = settingsWindow()
        val w = state.windows.single()

        val toLeft = WindowLogic.move(state, id, dx = -5000f, dy = 0f, areaW = areaW, areaH = areaH, minVisible = 40f)
        assertEquals(
            Placement.SNAP_LEFT,
            WindowLogic.edgeSnap(toLeft, id, areaW, areaH, threshold).windows.single().placement,
        )

        val toRight = WindowLogic.move(state, id, dx = 5000f, dy = 0f, areaW = areaW, areaH = areaH, minVisible = 40f)
        assertEquals(
            Placement.SNAP_RIGHT,
            WindowLogic.edgeSnap(toRight, id, areaW, areaH, threshold).windows.single().placement,
        )

        val toTop = WindowLogic.move(state, id, dx = 0f, dy = -5000f, areaW = areaW, areaH = areaH, minVisible = 40f)
        assertEquals(
            Placement.MAXIMIZED,
            WindowLogic.edgeSnap(toTop, id, areaW, areaH, threshold).windows.single().placement,
        )

        // Solto no meio da tela: continua flutuante.
        assertEquals(Placement.FLOATING, WindowLogic.edgeSnap(state, id, areaW, areaH, threshold).windows.single().placement)
        assertTrue(w.width > 0f)
    }

    @Test
    fun maximized_frame_follows_the_current_area_after_rotation() {
        val (state, id) = settingsWindow()
        val maximized = WindowLogic.maximize(state, id).windows.single()

        // Rotação: a área muda de retrato para paisagem sem alterar o estado salvo.
        assertEquals(Bounds(0f, 0f, 1800f, 1000f), maximized.frame(1800f, 1000f))
        assertNotEquals(maximized.frame(1000f, 1800f), maximized.frame(1800f, 1000f))
    }

    @Test
    fun floating_frame_is_clamped_when_the_area_shrinks() {
        val (state, _) = settingsWindow()
        val w = state.windows.single().copy(x = 900f, y = 1500f)

        val frame = w.frame(500f, 800f)
        assertTrue(frame.x + frame.width <= 500f + 0.001f)
        assertTrue(frame.y + frame.height <= 800f + 0.001f)
    }
}
