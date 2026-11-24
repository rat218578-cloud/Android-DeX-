package com.example.desktopenv.model

import java.text.Normalizer

/** Busca de apps e janelas do desktop. Lógica pura, sem Android. */
object SearchIndex {

    /** Item pesquisável. [key] identifica o alvo para quem chamou (ex.: "app:pkg" ou "window:CALENDAR"). */
    data class Entry(val key: String, val label: String, val keywords: List<String> = emptyList())

    /** Minúsculas, sem acentos e sem espaços nas pontas. Permite buscar "config" e achar "Configurações". */
    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase()
            .trim()

    /**
     * Ordena por relevância:
     * 1. o rótulo começa com a busca;
     * 2. alguma palavra do rótulo começa com a busca;
     * 3. o texto aparece dentro do rótulo;
     * 4. alguma palavra-chave contém a busca.
     * Empates seguem o rótulo em ordem alfabética. Busca vazia devolve os primeiros itens na ordem recebida.
     */
    fun search(query: String, entries: List<Entry>, limit: Int = 50): List<Entry> {
        val q = normalize(query)
        if (q.isEmpty()) return entries.take(limit)

        return entries
            .mapNotNull { entry ->
                val label = normalize(entry.label)
                val score = when {
                    label.startsWith(q) -> 0
                    label.split(' ', '-', '·').any { it.startsWith(q) } -> 1
                    label.contains(q) -> 2
                    entry.keywords.any { normalize(it).contains(q) } -> 3
                    else -> return@mapNotNull null
                }
                Triple(score, label, entry)
            }
            .sortedWith(compareBy<Triple<Int, String, Entry>>({ it.first }, { it.second }))
            .map { it.third }
            .take(limit)
    }
}
