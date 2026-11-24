package com.example.desktopenv.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BatteryInfo(val level: Int = 0, val charging: Boolean = false)

enum class NetworkKind(val label: String) {
    WIFI("Wi-Fi"),
    CELLULAR("Dados"),
    ETHERNET("Ethernet"),
    NONE("Offline"),
}

/** Bateria e rede via broadcasts e callbacks padrão. Não requer permissões de localização. */
class SystemStatus(private val context: Context) {

    private val connectivity: ConnectivityManager =
        context.getSystemService(ConnectivityManager::class.java)

    private val _battery = MutableStateFlow(BatteryInfo())
    val battery: StateFlow<BatteryInfo> = _battery.asStateFlow()

    private val _network = MutableStateFlow(NetworkKind.NONE)
    val network: StateFlow<NetworkKind> = _network.asStateFlow()

    private var started = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            intent ?: return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val percent = if (level >= 0 && scale > 0) level * 100 / scale else 0
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
            _battery.value = BatteryInfo(percent, charging)
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            _network.value = kindOf(capabilities)
        }

        override fun onLost(network: Network) {
            _network.value = NetworkKind.NONE
        }
    }

    fun start() {
        if (started) return
        started = true

        // ACTION_BATTERY_CHANGED é um broadcast protegido do sistema e pode ser registrado sem exportação.
        ContextCompat.registerReceiver(
            context,
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )?.let { batteryReceiver.onReceive(context, it) }

        connectivity.registerDefaultNetworkCallback(networkCallback)
        connectivity.activeNetwork?.let { net ->
            connectivity.getNetworkCapabilities(net)?.let { _network.value = kindOf(it) }
        }
    }

    fun stop() {
        if (!started) return
        started = false
        runCatching { context.unregisterReceiver(batteryReceiver) }
        runCatching { connectivity.unregisterNetworkCallback(networkCallback) }
    }

    private fun kindOf(caps: NetworkCapabilities): NetworkKind = when {
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkKind.WIFI
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkKind.CELLULAR
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkKind.ETHERNET
        else -> NetworkKind.NONE
    }
}
