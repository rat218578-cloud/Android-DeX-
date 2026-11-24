package com.example.desktopenv.data

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.example.desktopenv.model.NotificationHistory
import com.example.desktopenv.model.NotificationRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Histórico de notificações do processo atual. Alimentado pelo [DeskoraNotificationListener].
 * Não é persistido: ao encerrar o processo, o histórico some.
 */
object NotificationStore {

    private val _items = MutableStateFlow<List<NotificationRecord>>(emptyList())
    val items: StateFlow<List<NotificationRecord>> = _items.asStateFlow()

    /** Quantas notificações chegaram desde a última vez que a janela de notificações foi aberta. */
    private val _unread = MutableStateFlow(0)
    val unread: StateFlow<Int> = _unread.asStateFlow()

    fun add(record: NotificationRecord) {
        _items.update { NotificationHistory.add(it, record) }
        _unread.update { it + 1 }
    }

    fun markAllRead() {
        _unread.value = 0
    }

    fun clear() {
        _items.value = emptyList()
        _unread.value = 0
    }
}

/** Verificação e atalho para a permissão de acesso às notificações, concedida pelo usuário. */
object NotificationAccess {

    fun isGranted(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    fun settingsIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
}
