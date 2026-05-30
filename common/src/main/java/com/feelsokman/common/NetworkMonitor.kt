package com.feelsokman.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.core.content.getSystemService
import com.feelsokman.logging.logDebug
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

interface NetworkMonitor {
    val isOnline: Flow<Boolean>
}

class ConnectivityManagerNetworkMonitor(
    private val context: Context,
) : NetworkMonitor {
    override val isOnline: Flow<Boolean> = callbackFlow {
        val connectivityManager = context.getSystemService<ConnectivityManager>()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                channel.trySend(connectivityManager.isCurrentlyConnected())
            }

            override fun onLost(network: Network) {
                channel.trySend(connectivityManager.isCurrentlyConnected())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                channel.trySend(connectivityManager.isCurrentlyConnected())
            }
        }

        logDebug { "fefef" }

        connectivityManager?.registerNetworkCallback(
            NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build(),
            callback,
        )

        channel.trySend(connectivityManager.isCurrentlyConnected())

        awaitClose {
            connectivityManager?.unregisterNetworkCallback(callback)
        }
    }
        .conflate()

    @Suppress("DEPRECATION")
    private fun ConnectivityManager?.isCurrentlyConnected() = when (this) {
        null -> false
        else -> when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                activeNetwork
                    ?.let(::getNetworkCapabilities)
                    ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    ?: false

            else -> activeNetworkInfo?.isConnected ?: false
        }
    }
}
