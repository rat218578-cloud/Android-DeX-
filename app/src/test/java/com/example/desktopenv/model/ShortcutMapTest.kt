package com.example.desktopenv.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShortcutMapTest {

    private val none = Modifiers()

    private data class Modifiers(val ctrl: Boolean = false, val alt: Boolean = false, val meta: Boolean = false, val shift: Boolean = false)

    private fun resolve(keyCode: Int, m: Modifiers = none) =
        ShortcutMap.resolve(keyCode, ctrl = m.ctrl, alt = m.alt, meta = m.meta, shift = m.shift)

    @Test
    fun escape_alone_closes_overlays() {
        assertEquals(ShortcutAction.CLOSE_OVERLAYS, resolve(111))
    }

    @Test
    fun escape_with_ctrl_is_not_the_overlay_shortcut() {
        assertNull(resolve(111, Modifiers(ctrl = true)))
    }

    @Test
    fun alt_tab_cycles_and_alt_f4_closes_the_top_window() {
        assertEquals(ShortcutAction.CYCLE_WINDOWS, resolve(61, Modifiers(alt = true)))
        assertEquals(ShortcutAction.CLOSE_TOP_WINDOW, resolve(134, Modifiers(alt = true)))
    }

    @Test
    fun ctrl_letters_map_to_window_and_desktop_actions() {
        assertEquals(ShortcutAction.CLOSE_TOP_WINDOW, resolve(51, Modifiers(ctrl = true)))
        assertEquals(ShortcutAction.MINIMIZE_TOP_WINDOW, resolve(41, Modifiers(ctrl = true)))
        assertEquals(ShortcutAction.SHOW_DESKTOP, resolve(32, Modifiers(ctrl = true)))
        assertEquals(ShortcutAction.OPEN_FILES, resolve(33, Modifiers(ctrl = true)))
        assertEquals(ShortcutAction.OPEN_SETTINGS, resolve(55, Modifiers(ctrl = true)))
    }

    @Test
    fun windows_with_arrows_snaps_and_maximizes() {
        val meta = Modifiers(meta = true)
        assertEquals(ShortcutAction.SNAP_LEFT_TOP_WINDOW, resolve(21, meta))
        assertEquals(ShortcutAction.SNAP_RIGHT_TOP_WINDOW, resolve(22, meta))
        assertEquals(ShortcutAction.MAXIMIZE_TOP_WINDOW, resolve(19, meta))
        assertEquals(ShortcutAction.RESTORE_TOP_WINDOW, resolve(20, meta))
    }

    @Test
    fun windows_key_alone_toggles_start() {
        assertEquals(ShortcutAction.TOGGLE_START, resolve(117, Modifiers(meta = true)))
        assertEquals(ShortcutAction.TOGGLE_START, resolve(118))
    }

    @Test
    fun f11_toggles_maximize() {
        assertEquals(ShortcutAction.TOGGLE_MAXIMIZE_TOP_WINDOW, resolve(141))
    }

    @Test
    fun unbound_keys_resolve_to_null() {
        assertNull(resolve(29))
        assertNull(resolve(21))
        assertNull(resolve(51))
    }
}
