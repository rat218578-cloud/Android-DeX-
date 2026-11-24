package com.example.desktopenv.model

/** Uma notificação recebida de outro app. Existe apenas em memória; nunca é gravada em disco. */
data class NotificationRecord(
    val key: String,
    val packageName: String,
    val title: String,
    val text: String,
    val postedAt: Long,
)

/** Histórico limitado de notificações, mais recente primeiro. Lógica pura, sem Android. */
object NotificationHistory {
    const val MAX_ITEMS = 100

    /** Insere a notificação no topo. Se a mesma chave já existir, a versão antiga é substituída. */
    fun add(current: List<NotificationRecord>, record: NotificationRecord): List<NotificationRecord> =
        (listOf(record) + current.filter { it.key != record.key }).take(MAX_ITEMS)

    fun remove(current: List<NotificationRecord>, key: String): List<NotificationRecord> =
        current.filterNot { it.key == key }
}
