package com.example.desktopenv.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaLogicTest {

    @Test
    fun volume_extremes_are_zero_and_full() {
        assertEquals(0, MediaLogic.volumePercent(0, 15))
        assertEquals(100, MediaLogic.volumePercent(15, 15))
    }

    @Test
    fun volume_is_rounded_to_the_nearest_percent() {
        assertEquals(47, MediaLogic.volumePercent(7, 15))
        assertEquals(50, MediaLogic.volumePercent(8, 16))
    }

    @Test
    fun volume_out_of_range_is_clamped() {
        assertEquals(100, MediaLogic.volumePercent(40, 15))
        assertEquals(0, MediaLogic.volumePercent(-3, 15))
    }

    @Test
    fun zero_max_volume_gives_zero_instead_of_dividing_by_zero() {
        assertEquals(0, MediaLogic.volumePercent(5, 0))
    }
}
