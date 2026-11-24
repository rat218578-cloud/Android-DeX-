package com.example.desktopenv.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RecentListTest {

    @Test
    fun pushing_into_an_empty_list_creates_a_single_item() {
        assertEquals(listOf("a.app"), RecentList.push(emptyList(), "a.app"))
    }

    @Test
    fun pushing_the_only_item_again_keeps_a_single_item() {
        assertEquals(listOf("a.app"), RecentList.push(listOf("a.app"), "a.app"))
    }

    @Test
    fun pushing_a_new_app_puts_it_first_and_keeps_the_others_in_order() {
        assertEquals(
            listOf("c.app", "a.app", "b.app"),
            RecentList.push(listOf("a.app", "b.app"), "c.app"),
        )
    }

    @Test
    fun the_cap_drops_the_oldest_item_not_the_newest() {
        val full = (0 until RecentList.MAX_ITEMS).map { "pkg$it" }
        val result = RecentList.push(full, "novo.app")

        assertEquals(RecentList.MAX_ITEMS, result.size)
        assertEquals("novo.app", result.first())
        assertEquals("pkg0", result[1])
        assertFalse("pkg${RecentList.MAX_ITEMS - 1}" in result)
    }
}
