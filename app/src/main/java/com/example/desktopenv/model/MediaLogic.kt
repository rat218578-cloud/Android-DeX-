package com.example.desktopenv.model

/** Faixa em reprodução em algum app de mídia. Lógica pura, sem Android. */
data class MediaInfo(
    val title: String,
    val artist: String,
    val playing: Boolean,
    val packageName: String,
)

object MediaLogic {

    /** Converte volume bruto em porcentagem arredondada, limitada entre 0 e 100. */
    fun volumePercent(current: Int, max: Int): Int {
        if (max <= 0) return 0
        val clamped = current.coerceIn(0, max)
        return (clamped * 100 + max / 2) / max
    }
}
