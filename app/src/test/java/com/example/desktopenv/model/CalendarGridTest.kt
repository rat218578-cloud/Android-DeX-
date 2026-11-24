package com.example.desktopenv.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarGridTest {

    @Test
    fun november_2026_starts_on_sunday_so_first_row_has_six_blanks() {
        // 1 de novembro de 2026 é domingo.
        val weeks = CalendarGrid.weeks(2026, 11)
        assertEquals(6, weeks.first().count { it == null })
        assertEquals(1, weeks.first()[6])
    }

    @Test
    fun every_week_has_seven_cells_and_all_days_appear_once() {
        val weeks = CalendarGrid.weeks(2026, 2)
        assertTrue7(weeks)
        val days = weeks.flatten().filterNotNull()
        assertEquals((1..28).toList(), days)
    }

    @Test
    fun month_starting_on_monday_has_no_leading_blanks() {
        // 1 de junho de 2026 é segunda-feira.
        val weeks = CalendarGrid.weeks(2026, 6)
        assertEquals(1, weeks.first()[0])
        assertNull(weeks.last().last())
    }

    private fun assertTrue7(weeks: List<List<Int?>>) {
        weeks.forEach { assertEquals(7, it.size) }
    }
}
