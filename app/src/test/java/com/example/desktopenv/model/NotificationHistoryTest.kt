package com.example.desktopenv.model

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationHistoryTest {

    private fun record(key: String) =
        NotificationRecord(key = key, packageName = "pkg.$key", title = "t$key", text = "x", postedAt = 0L)

    @Test
    fun newest_notification_goes_first() {
        val list = NotificationHistory.add(listOf(record("a")), record("b"))
        assertEquals(listOf("b", "a"), list.map { it.key })
    }

    @Test
    fun same_key_replaces_the_old_entry_instead_of_duplicating() {
        val list = listOf(record("a"), record("b"))
        val updated = NotificationHistory.add(list, record("b").copy(title = "novo"))

        assertEquals(listOf("b", "a"), updated.map { it.key })
        assertEquals("novo", updated.first().title)
    }

    @Test
    fun history_keeps_only_the_most_recent_items() {
        var list: List<NotificationRecord> = emptyList()
        repeat(NotificationHistory.MAX_ITEMS + 25) { list = NotificationHistory.add(list, record("n$it")) }

        assertEquals(NotificationHistory.MAX_ITEMS, list.size)
        assertEquals("n${NotificationHistory.MAX_ITEMS + 24}", list.first().key)
    }

    @Test
    fun remove_drops_only_the_matching_key() {
        val list = listOf(record("a"), record("b"), record("c"))
        assertEquals(listOf("a", "c"), NotificationHistory.remove(list, "b").map { it.key })
    }
}
