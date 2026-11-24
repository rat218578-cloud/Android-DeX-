package com.example.desktopenv.model

/** Ações disparadas por atalho de teclado. */
enum class ShortcutAction {
    CLOSE_OVERLAYS,
    CYCLE_WINDOWS,
    CLOSE_TOP_WINDOW,
    MINIMIZE_TOP_WINDOW,
    TOGGLE_MAXIMIZE_TOP_WINDOW,
    MAXIMIZE_TOP_WINDOW,
    RESTORE_TOP_WINDOW,
    SNAP_LEFT_TOP_WINDOW,
    SNAP_RIGHT_TOP_WINDOW,
    SHOW_DESKTOP,
    TOGGLE_START,
    OPEN_FILES,
    OPEN_SETTINGS,
}

/**
 * Tabela de atalhos. Usa os códigos de tecla do Android (android.view.KeyEvent) para que
 * a lógica rode na JVM. Combinações reservadas pelo sistema não aparecem aqui.
 */
object ShortcutMap {

    private const val KEYCODE_D = 32
    private const val KEYCODE_E = 33
    private const val KEYCODE_M = 41
    private const val KEYCODE_W = 51
    private const val KEYCODE_COMMA = 55
    private const val KEYCODE_TAB = 61
    private const val KEYCODE_DPAD_UP = 19
    private const val KEYCODE_DPAD_DOWN = 20
    private const val KEYCODE_DPAD_LEFT = 21
    private const val KEYCODE_DPAD_RIGHT = 22
    private const val KEYCODE_META_LEFT = 117
    private const val KEYCODE_META_RIGHT = 118
    private const val KEYCODE_ESCAPE = 111
    private const val KEYCODE_F4 = 134
    private const val KEYCODE_F11 = 141

    /** Resolve a ação de uma tecla pressionada com os modificadores indicados. Retorna null se não houver atalho. */
    fun resolve(keyCode: Int, ctrl: Boolean, alt: Boolean, meta: Boolean, shift: Boolean): ShortcutAction? {
        // Atalhos com Windows (meta) e setas: encaixe e maximização.
        if (meta) {
            when (keyCode) {
                KEYCODE_DPAD_LEFT -> return ShortcutAction.SNAP_LEFT_TOP_WINDOW
                KEYCODE_DPAD_RIGHT -> return ShortcutAction.SNAP_RIGHT_TOP_WINDOW
                KEYCODE_DPAD_UP -> return ShortcutAction.MAXIMIZE_TOP_WINDOW
                KEYCODE_DPAD_DOWN -> return ShortcutAction.RESTORE_TOP_WINDOW
            }
        }
        return when {
            keyCode == KEYCODE_ESCAPE && !ctrl && !alt && !meta -> ShortcutAction.CLOSE_OVERLAYS
            keyCode == KEYCODE_TAB && alt -> ShortcutAction.CYCLE_WINDOWS
            keyCode == KEYCODE_F4 && alt -> ShortcutAction.CLOSE_TOP_WINDOW
            keyCode == KEYCODE_W && ctrl -> ShortcutAction.CLOSE_TOP_WINDOW
            keyCode == KEYCODE_M && ctrl -> ShortcutAction.MINIMIZE_TOP_WINDOW
            keyCode == KEYCODE_D && ctrl -> ShortcutAction.SHOW_DESKTOP
            keyCode == KEYCODE_E && ctrl -> ShortcutAction.OPEN_FILES
            keyCode == KEYCODE_COMMA && ctrl -> ShortcutAction.OPEN_SETTINGS
            keyCode == KEYCODE_F11 -> ShortcutAction.TOGGLE_MAXIMIZE_TOP_WINDOW
            keyCode == KEYCODE_META_LEFT || keyCode == KEYCODE_META_RIGHT -> ShortcutAction.TOGGLE_START
            else -> null
        }
    }

    /** Lista para exibir em Configurações: combinação e o que ela faz. */
    val descriptions: List<Pair<String, String>> = listOf(
        "Esc" to "Fecha menus abertos",
        "Alt + Tab" to "Alterna entre janelas abertas",
        "Alt + F4  ·  Ctrl + W" to "Fecha a janela em foco",
        "Ctrl + M" to "Minimiza a janela em foco",
        "Ctrl + D" to "Minimiza todas as janelas",
        "F11" to "Maximiza ou restaura a janela em foco",
        "Windows + ←  /  →" to "Encaixa a janela à esquerda ou à direita",
        "Windows + ↑  /  ↓" to "Maximiza ou restaura a janela em foco",
        "Windows" to "Abre ou fecha o menu Iniciar",
        "Ctrl + E" to "Abre uma nova janela de arquivos",
        "Ctrl + ," to "Abre as configurações do Deskora",
    )
}
