package name.jayhan.dolbom

import android.content.Context
import android.telephony.ServiceState
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager

class PhoneCallback(
    private val context: Context,
):
    TelephonyCallback(), TelephonyCallback.ServiceStateListener {

    private val teleMan = context.getSystemService(Context.TELEPHONY_SERVICE)
            as TelephonyManager
    private val subsMan = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
            as SubscriptionManager
    private var mobileGen = 0
    private var hasDataConnection = false
    private var activeSim = 0
    private var isRoaming = false
    private var operator = ""

    init {
        scan()

        try {
            teleMan.registerTelephonyCallback(
                TelephonyManager.INCLUDE_LOCATION_DATA_FINE,
                context.mainExecutor,
                this
            )
        } catch (_: SecurityException) {}
    }
    
    override fun onServiceStateChanged(serviceState: ServiceState) {
        scan()
    }

    fun deinit() {
        teleMan.unregisterTelephonyCallback(this)
    }

    private fun scan() {
        try {
            activeSim = 1
            if (teleMan.isMultiSimSupported == TelephonyManager.MULTISIM_ALLOWED) {
                val simMccMnc = teleMan.simOperator
                val subsList = subsMan.activeSubscriptionInfoList
                subsList?.forEach {
                    val simIndex = it.simSlotIndex
                    val mcc = it.mccString
                    val mnc = it.mncString
                    if (simMccMnc == mcc + mnc) {
                        activeSim = simIndex + 1
                    }
                }
            }
            isRoaming = teleMan.isNetworkRoaming
            operator = teleMan.networkOperatorName
            hasDataConnection = true
            mobileGen =
                if (teleMan.dataState == TelephonyManager.DATA_CONNECTED)
                    getCellGen(teleMan.dataNetworkType)
                else 0
            if (mobileGen == 0) {
                hasDataConnection = false
                mobileGen = getCellGen(teleMan.voiceNetworkType)
            }
            refresh()

        } catch (_: SecurityException) { }
    }

    private fun getCellGen(gen: Int): Int {
        return when (gen) {
            TelephonyManager.NETWORK_TYPE_GSM,
            TelephonyManager.NETWORK_TYPE_GPRS,
            TelephonyManager.NETWORK_TYPE_EDGE,
                -> 2
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_UMTS,
                -> 3
            TelephonyManager.NETWORK_TYPE_LTE
                -> 4
            TelephonyManager.NETWORK_TYPE_NR
                -> 5
            else -> 0
        }
    }

    fun refresh() {
        Pebble.sendIntent(context, MsgType.CELL) {
            putExtra(Const.EXTRA_CELL, mobileGen or if (hasDataConnection) 0x10 else 0)
            putExtra(Const.EXTRA_SIM, activeSim or if (isRoaming) 0x10 else 0)
            putExtra(Const.EXTRA_CARRIER, operator)
        }
    }
}

class WifiCallback(
    private val context: Context,
) :
    ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO)
{
    private val connMan = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as ConnectivityManager
    private var networks = mutableMapOf<Network, String>()

    init {
        refresh()
        
        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connMan.registerNetworkCallback(networkRequest, this)
    }
    
    fun deinit() {
        connMan.unregisterNetworkCallback(this)
    }

    override fun onAvailable(network: Network) {
        super.onAvailable(network)
        updateSsid(network)
    }

    override fun onLost(network: Network) {
        super.onLost(network)
        networks.remove(network)
        refresh()
    }

    override fun onCapabilitiesChanged(
        network: Network,
        capabilities: NetworkCapabilities
    ) {
        super.onCapabilitiesChanged(network, capabilities)
        updateSsid(network, capabilities)
    }
    
    private fun updateSsid(
        network: Network,
        capabilities: NetworkCapabilities? = null
    ) {
        val capa = capabilities ?: connMan.getNetworkCapabilities(network)
        val ssid =
            if (capa != null) {
                val info = capa.transportInfo as WifiInfo
                val ssid = info.ssid.removeSurrounding("\"")
                if (ssid != WifiManager.UNKNOWN_SSID) ssid
                else ""
            } else ""
        networks[network] = ssid
        refresh()
    }

    fun refresh() {
        val ssid = networks.map { it.value }.firstOrNull { it != "" } ?: ""
        Pebble.sendIntent(context, MsgType.WIFI) {
            putExtra(Const.EXTRA_WIFI, ssid)
        }
    }
}

class InternetCallback(
    private val context: Context,
) :
    ConnectivityManager.NetworkCallback()
{
    private val connMan = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as ConnectivityManager
    val networks = mutableMapOf<Network, Boolean>()

    init {
        refresh()
        
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()
        connMan.registerNetworkCallback(networkRequest, this)
    }
    
    fun deinit() {
        connMan.unregisterNetworkCallback(this)
    }

    override fun onAvailable(network: Network) {
        super.onAvailable(network)
        updateNetwork(network)
    }

    override fun onLost(network: Network) {
        super.onLost(network)
        networks.remove(network)
        refresh()
    }

    override fun onCapabilitiesChanged(
        network: Network,
        capabilities: NetworkCapabilities
    ) {
        super.onCapabilitiesChanged(network, capabilities)
        updateNetwork(network, capabilities)
    }
    
    fun updateNetwork(
        network: Network,
        capabilities: NetworkCapabilities? = null
    ) {
        val capa = capabilities ?: connMan.getNetworkCapabilities(network)
        val hasInternet =
            if (capa != null) capa.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capa.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            else false
        networks[network] = hasInternet

        refresh()
    }

    fun refresh() {
        val hasInternet = networks.any { it.value }
        Pebble.sendIntent(context, MsgType.NET) {
            putExtra(Const.EXTRA_NET, hasInternet)
        }
    }
}
