package com.example.desktopenv.model

import com.example.desktopenv.model.SearchIndex.Entry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchIndexTest {

    private val entries = listOf(
        Entry("a", "Calendário"),
        Entry("b", "Agenda Calendário"),
        Entry("c", "Configurações", listOf("wifi", "ajustes")),
        Entry("d", "Mapas"),
    )

    @Test
    fun blank_query_returns_the_entries_in_their_original_order() {
        assertEquals(entries.map { it.key }, SearchIndex.search("  ", entries).map { it.key })
    }

    @Test
    fun match_ignores_case_and_accents() {
        val result = SearchIndex.search("CONFIGURACOES", entries)
        assertEquals(listOf("c"), result.map { it.key })
    }

    @Test
    fun label_prefix_ranks_before_a_word_start_match() {
        val result = SearchIndex.search("cal", entries)
        assertEquals(listOf("a", "b"), result.map { it.key })
    }

    @Test
    fun keyword_match_finds_the_entry_even_when_the_label_does_not_contain_it() {
        assertEquals(listOf("c"), SearchIndex.search("wifi", entries).map { it.key })
    }

    @Test
    fun no_match_returns_an_empty_list() {
        assertTrue(SearchIndex.search("xyzzy", entries).isEmpty())
    }

    @Test
    fun results_are_capped_by_limit() {
        val many = (1..100).map { Entry("k$it", "App $it") }
        assertEquals(5, SearchIndex.search("app", many, limit = 5).size)
    }
}
