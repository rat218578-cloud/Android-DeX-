package com.example.desktopenv.data

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.desktopenv.model.NotificationRecord

/**
 * Recebe as notificações de outros apps quando o usuário concede o acesso em Ajustes.
 *
 * Regras de privacidade:
 * - Processamento local. Nada é enviado a servidores nem a outros apps.
 * - Não responde, não remove e não altera notificações de outros apps.
 * - Ignora as notificações do próprio Deskora e as fixas (ongoing), como as de reprodução de mídia.
 * - Guarda só título e texto, e só em memória (ver [NotificationStore]).
 */
class DeskoraNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        if (sbn.packageName == packageName || sbn.isOngoing) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty().trim()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty().trim()
        if (title.isEmpty() && text.isEmpty()) return

        NotificationStore.add(
            NotificationRecord(
                key = sbn.key,
                packageName = sbn.packageName,
                title = title,
                text = text,
                postedAt = sbn.postTime,
            ),
        )
    }
}
