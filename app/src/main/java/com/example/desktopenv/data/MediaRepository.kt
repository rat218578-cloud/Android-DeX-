package com.example.desktopenv.data

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.desktopenv.model.MediaInfo
import com.example.desktopenv.model.MediaLogic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Integração com a mídia do sistema.
 *
 * - Sessões de mídia de outros apps só aparecem com o acesso às notificações concedido,
 *   porque o Android exige um NotificationListenerService para listá-las.
 * - Play, pausa e faixa seguinte usam [MediaController.getTransportControls] da sessão ativa.
 * - Volume usa [AudioManager] no fluxo de música, o que o Android permite a qualquer app.
 * - Nada é gravado em disco nem enviado a servidores.
 */
class MediaRepository(private val context: Context) {

    private val audio = context.getSystemService(AudioManager::class.java)
    private val sessionManager = context.getSystemService(MediaSessionManager::class.java)
    private val listenerComponent = ComponentName(context, DeskoraNotificationListener::class.java)
    private val handler = Handler(Looper.getMainLooper())

    private val _media = MutableStateFlow<MediaInfo?>(null)
    val media: StateFlow<MediaInfo?> = _media.asStateFlow()

    private val _volumePercent = MutableStateFlow(0)
    val volumePercent: StateFlow<Int> = _volumePercent.asStateFlow()

    private var controller: MediaController? = null
    private var listening = false
    private var receiverRegistered = false

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = refreshMedia()

        override fun onMetadataChanged(metadata: MediaMetadata?) = refreshMedia()

        override fun onSessionDestroyed() {
            bind(firstActiveController())
        }
    }

    private val sessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        bind(list.orEmpty().firstOrNull())
    }

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshVolume()
        }
    }

    /** Inicia o monitoramento do volume e, se houver acesso, das sessões de mídia. */
    fun start() {
        if (!receiverRegistered) {
            receiverRegistered = true
            ContextCompat.registerReceiver(
                context,
                volumeReceiver,
                IntentFilter(VOLUME_CHANGED_ACTION),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        }
        refresh()
    }

    /**
     * Reavalia a permissão. Chamado ao iniciar e ao voltar de Ajustes, porque o usuário
     * pode ter concedido ou revogado o acesso às notificações.
     */
    fun refresh() {
        refreshVolume()
        val granted = NotificationAccess.isGranted(context)
        when {
            granted && !listening -> {
                listening = true
                runCatching { sessionManager.addOnActiveSessionsChangedListener(sessionsChangedListener, listenerComponent) }
                bind(firstActiveController())
            }
            !granted && listening -> {
                listening = false
                runCatching { sessionManager.removeOnActiveSessionsChangedListener(sessionsChangedListener) }
                bind(null)
            }
        }
    }

    fun stop() {
        if (receiverRegistered) {
            receiverRegistered = false
            runCatching { context.unregisterReceiver(volumeReceiver) }
        }
        if (listening) {
            listening = false
            runCatching { sessionManager.removeOnActiveSessionsChangedListener(sessionsChangedListener) }
        }
        bind(null)
    }

    fun playPause() {
        val c = controller ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) {
            c.transportControls?.pause()
        } else {
            c.transportControls?.play()
        }
    }

    fun next() {
        controller?.transportControls?.skipToNext()
    }

    fun previous() {
        controller?.transportControls?.skipToPrevious()
    }

    fun volumeUp() {
        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
        refreshVolume()
    }

    fun volumeDown() {
        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
        refreshVolume()
    }

    private fun firstActiveController(): MediaController? {
        if (!NotificationAccess.isGranted(context)) return null
        return runCatching {
            sessionManager.getActiveSessions(listenerComponent).firstOrNull()
        }.getOrNull()
    }

    private fun bind(next: MediaController?) {
        controller?.unregisterCallback(controllerCallback)
        controller = next
        next?.registerCallback(controllerCallback, handler)
        refreshMedia()
    }

    private fun refreshMedia() {
        val c = controller
        if (c == null) {
            _media.value = null
            return
        }
        val metadata = c.metadata
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty()
        val playing = c.playbackState?.state == PlaybackState.STATE_PLAYING
        _media.value = if (title.isEmpty() && artist.isEmpty() && !playing) {
            null
        } else {
            MediaInfo(title = title, artist = artist, playing = playing, packageName = c.packageName.orEmpty())
        }
    }

    private fun refreshVolume() {
        val current = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        _volumePercent.value = MediaLogic.volumePercent(current, max)
    }

    private companion object {
        const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
    }
}
