package com.example.desktopenv

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Testes de ciclo de vida, janelas e persistência, executados em emulador no CI.
 * Não cobrem dispositivos físicos: ver doc/PENDENCIAS_TESTE_FISICO.md.
 */
@RunWith(AndroidJUnit4::class)
class DesktopInstrumentedTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val calendarLabel = "Calendário"

    @Test
    fun taskbar_with_start_button_is_shown_on_launch() {
        rule.onNodeWithContentDescription("Iniciar").assertIsDisplayed()
    }

    @Test
    fun start_menu_opens_with_search_field() {
        rule.onNodeWithContentDescription("Iniciar").performClick()
        rule.onNodeWithText("Pesquisar apps e janelas").assertIsDisplayed()
    }

    @Test
    fun opening_a_single_instance_window_twice_does_not_duplicate_it() {
        // Calendário é tipo de instância única: a segunda abertura só foca a janela existente.
        val before = rule.onAllNodesWithText(calendarLabel).fetchSemanticsNodes().size

        rule.onAllNodesWithText(calendarLabel).onFirst().performClick()
        rule.waitForIdle()
        val afterFirst = rule.onAllNodesWithText(calendarLabel).fetchSemanticsNodes().size
        check(afterFirst > before) { "A janela de Calendário não abriu" }

        rule.onAllNodesWithText(calendarLabel).onFirst().performClick()
        rule.waitForIdle()
        val afterSecond = rule.onAllNodesWithText(calendarLabel).fetchSemanticsNodes().size
        check(afterSecond == afterFirst) { "Calendário foi duplicado: $afterFirst -> $afterSecond" }
    }

    @Test
    fun open_window_survives_activity_recreation() {
        rule.onAllNodesWithText(calendarLabel).onFirst().performClick()
        rule.waitForIdle()
        val opened = rule.onAllNodesWithText(calendarLabel).fetchSemanticsNodes().size

        // O layout é salvo com debounce de 400 ms; espera a gravação antes de recriar.
        Thread.sleep(1000)
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()

        val restored = rule.onAllNodesWithText(calendarLabel).fetchSemanticsNodes().size
        check(restored == opened) { "Janela não foi restaurada após recriar: $opened -> $restored" }
    }
}
