package com.example.desktopenv.model

import java.time.YearMonth

/** Monta a grade mensal. Semanas começam na segunda-feira; células vazias são null. */
object CalendarGrid {

    fun weeks(year: Int, month: Int): List<List<Int?>> {
        val ym = YearMonth.of(year, month)
        val leadingBlanks = ym.atDay(1).dayOfWeek.value - 1 // segunda = 0 ... domingo = 6
        val cells = MutableList<Int?>(leadingBlanks) { null }
        for (day in 1..ym.lengthOfMonth()) cells.add(day)
        while (cells.size % 7 != 0) cells.add(null)
        return cells.chunked(7)
    }
}
