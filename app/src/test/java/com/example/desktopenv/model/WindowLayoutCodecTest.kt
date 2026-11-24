package com.example.desktopenv.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowLayoutCodecTest {

    private fun sample(): WindowsState {
        var state = WindowLogic.open(WindowsState(), WindowKind.SETTINGS, 1000f, 1800f, 1f)
        state = WindowLogic.open(state, WindowKind.FILES, 1000f, 1800f, 1f, payload = "content://tree/a%7Cb")
        state = WindowLogic.open(state, WindowKind.FILES, 1000f, 1800f, 1f, payload = "content://tree/c")
        state = WindowLogic.snap(state, state.windows[1].id, Placement.SNAP_RIGHT)
        state = WindowLogic.minimize(state, state.windows[2].id)
        return state
    }

    @Test
    fun encode_then_decode_keeps_windows_placement_payload_and_counters() {
        val original = sample()
        val decoded = WindowLayoutCodec.decode(WindowLayoutCodec.encode(original))

        assertEquals(original, decoded)
    }

    @Test
    fun payload_with_separator_characters_survives_the_roundtrip() {
        val original = sample()
        val decoded = WindowLayoutCodec.decode(WindowLayoutCodec.encode(original))

        assertEquals("content://tree/a%7Cb", decoded.windows[1].payload)
    }

    @Test
    fun blank_or_unknown_version_gives_an_empty_desktop() {
        assertEquals(WindowsState(), WindowLayoutCodec.decode(""))
        assertEquals(WindowsState(), WindowLayoutCodec.decode("v9|1|1\n1|SETTINGS|0|0|1|1|1|0|FLOATING|0|0|0|0|0|"))
    }

    @Test
    fun corrupted_lines_are_skipped_and_valid_ones_are_kept() {
        val valid = WindowLayoutCodec.encode(sample())
        val corrupted = valid + "\nnão é uma janela\n99|NAO_EXISTE|0|0|1|1|1|0|FLOATING|0|0|0|0|0|\n" +
            "5|CALENDAR|x|0|1|1|1|0|FLOATING|0|0|0|0|0|"

        val decoded = WindowLayoutCodec.decode(corrupted)

        assertEquals(sample().windows.size, decoded.windows.size)
    }

    @Test
    fun counters_are_raised_above_saved_windows_to_avoid_id_clashes() {
        val raw = "v1|1|1\n7|CALENDAR|0|0|300|300|4|0|FLOATING|0|0|0|0|0|"

        val decoded = WindowLayoutCodec.decode(raw)

        assertTrue(decoded.nextId > 7L)
        assertTrue(decoded.nextZ > 4)
    }

    @Test
    fun duplicate_ids_keep_only_the_first_window() {
        val raw = "v1|5|5\n1|CALENDAR|0|0|300|300|1|0|FLOATING|0|0|0|0|0|\n" +
            "1|RECENT|0|0|300|300|2|0|FLOATING|0|0|0|0|0|"

        val decoded = WindowLayoutCodec.decode(raw)

        assertEquals(1, decoded.windows.size)
        assertEquals(WindowKind.CALENDAR, decoded.windows.single().kind)
    }

    @Test
    fun snapped_window_without_restore_bounds_falls_back_to_floating() {
        val raw = "v1|2|2\n1|CALENDAR|10|10|300|300|1|0|SNAP_LEFT|0|0|0|0|0|"

        val window = WindowLayoutCodec.decode(raw).windows.single()

        assertEquals(Placement.FLOATING, window.placement)
    }
}
