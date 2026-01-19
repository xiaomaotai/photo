package com.ruolijianzhen.app.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 网络状态
 */
enum class NetworkStatus {
    /**
     * 网络可用
     */
    AVAILABLE,
    
    /**
     * 网络不可用
     */
    UNAVAILABLE,
    
    /**
     * 网络状态未知
     */
    UNKNOWN
}

/**
 * 网络类型
 */
enum class NetworkType {
    WIFI,
    CELLULAR,
    ETHERNET,
    UNKNOWN,
    NONE
}

/**
 * 网络信息
 */
data class NetworkInfo(
    val status: NetworkStatus,
    val type: NetworkType,
    val isMetered: Boolean = false, // 是否计费网络
    val isRoaming: Boolean = false  // 是否漫游
) {
    val isAvailable: Boolean
        get() = status == NetworkStatus.AVAILABLE
    
    val isWifi: Boolean
        get() = type == NetworkType.WIFI
    
    val isCellular: Boolean
        get() = type == NetworkType.CELLULAR
    
    companion object {
        val UNAVAILABLE = NetworkInfo(
            status = NetworkStatus.UNAVAILABLE,
            type = NetworkType.NONE
        )
        
        val UNKNOWN = NetworkInfo(
            status = NetworkStatus.UNKNOWN,
            type = NetworkType.UNKNOWN
        )
    }
}

/**
 * 网络工具类
 * 提供网络状态检测和监听功能
 */
@Singleton
class NetworkUtils @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "NetworkUtils"
    }
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _networkInfo = MutableStateFlow(getCurrentNetworkInfo())
    val networkInfo: StateFlow<NetworkInfo> = _networkInfo.asStateFlow()
    
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    
    /**
     * 获取当前网络信息
     */
    fun getCurrentNetworkInfo(): NetworkInfo {
        return try {
            val network = connectivityManager.activeNetwork
            if (network == null) {
                return NetworkInfo.UNAVAILABLE
            }
            
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            if (capabilities == null) {
                return NetworkInfo.UNAVAILABLE
            }
            
            val type = when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
                else -> NetworkType.UNKNOWN
            }
            
            val isMetered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            val isRoaming = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)
            
            NetworkInfo(
                status = NetworkStatus.AVAILABLE,
                type = type,
                isMetered = isMetered,
                isRoaming = isRoaming
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get network info", e)
            NetworkInfo.UNKNOWN
        }
    }
    
    /**
     * 检查网络是否可用
     */
    fun isNetworkAvailable(): Boolean {
        return getCurrentNetworkInfo().isAvailable
    }
    
    /**
     * 检查是否是WiFi网络
     */
    fun isWifiConnected(): Boolean {
        return getCurrentNetworkInfo().isWifi
    }
    
    /**
     * 检查是否是移动网络
     */
    fun isCellularConnected(): Boolean {
        return getCurrentNetworkInfo().isCellular
    }
    
    /**
     * 检查是否是计费网络
     */
    fun isMeteredNetwork(): Boolean {
        return getCurrentNetworkInfo().isMetered
    }
    
    /**
     * 开始监听网络变化
     */
    fun startMonitoring() {
        if (networkCallback != null) return
        
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "Network available")
                updateNetworkInfo()
            }
            
            override fun onLost(network: Network) {
                Log.d(TAG, "Network lost")
                _networkInfo.value = NetworkInfo.UNAVAILABLE
            }
            
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                Log.d(TAG, "Network capabilities changed")
                updateNetworkInfo()
            }
        }
        
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        try {
            connectivityManager.registerNetworkCallback(request, networkCallback!!)
            Log.d(TAG, "Network monitoring started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback", e)
        }
    }
    
    /**
     * 停止监听网络变化
     */
    fun stopMonitoring() {
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
                Log.d(TAG, "Network monitoring stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister network callback", e)
            }
        }
        networkCallback = null
    }
    
    /**
     * 更新网络信息
     */
    private fun updateNetworkInfo() {
        _networkInfo.value = getCurrentNetworkInfo()
    }
    
    /**
     * 获取网络状态Flow
     */
    fun observeNetworkStatus(): Flow<NetworkInfo> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(getCurrentNetworkInfo())
            }
            
            override fun onLost(network: Network) {
                trySend(NetworkInfo.UNAVAILABLE)
            }
            
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                trySend(getCurrentNetworkInfo())
            }
        }
        
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(request, callback)
        
        // 发送初始状态
        trySend(getCurrentNetworkInfo())
        
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
}