package com.example.desktopenv.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowPayloadTest {

    @Test
    fun several_file_windows_can_be_open_at_the_same_time_with_their_own_payloads() {
        var state = WindowLogic.open(WindowsState(), WindowKind.FILES, 1000f, 1800f, 1f, payload = "pasta-a")
        state = WindowLogic.open(state, WindowKind.FILES, 1000f, 1800f, 1f, payload = "pasta-b")

        assertEquals(2, state.windows.size)
        assertEquals(listOf("pasta-a", "pasta-b"), state.windows.map { it.payload })
    }

    @Test
    fun single_instance_kinds_do_not_duplicate_even_when_a_payload_is_given() {
        var state = WindowLogic.open(WindowsState(), WindowKind.SETTINGS, 1000f, 1800f, 1f, payload = "x")
        state = WindowLogic.open(state, WindowKind.SETTINGS, 1000f, 1800f, 1f, payload = "y")

        assertEquals(1, state.windows.size)
    }

    @Test
    fun set_payload_changes_only_the_target_and_keeps_stacking_order() {
        var state = WindowLogic.open(WindowsState(), WindowKind.FILES, 1000f, 1800f, 1f)
        state = WindowLogic.open(state, WindowKind.FILES, 1000f, 1800f, 1f)
        val (first, second) = state.windows
        val zBefore = second.z

        val updated = WindowLogic.setPayload(state, first.id, "nova")

        assertEquals("nova", updated.windows.first { it.id == first.id }.payload)
        assertEquals("", updated.windows.first { it.id == second.id }.payload)
        assertEquals(zBefore, updated.windows.first { it.id == second.id }.z)
        assertTrue(updated.nextZ == state.nextZ)
    }
}
