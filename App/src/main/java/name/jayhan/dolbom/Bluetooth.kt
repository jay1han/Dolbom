package name.jayhan.dolbom

import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

@SuppressLint("MissingPermission")

class BluetoothReceiver(
    private val context: Context,
) : BroadcastReceiver() {
    private val blueMan = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val proxy = BluetoothProxy()
    private var displayDevice = ConnectedDevice()
    private var isA2dpActive = false
    private var isHeadsetActive = false
    
    init {
        try {
            blueMan.adapter.getProfileProxy(context, proxy, BluetoothProfile.A2DP)
            blueMan.adapter.getProfileProxy(context, proxy, BluetoothProfile.HEADSET)
        } catch (_: SecurityException) {
        }
        
        context.registerReceiver(
            this,
            IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)
                addAction(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED)
            },
            Context.RECEIVER_EXPORTED
        )
    }
    
    fun deinit() {
        context.unregisterReceiver(this)
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        proxy.rescan()
    }
    
    data class ConnectedDevice(
        val name: String = "",
        val battery: Int = 0,
        val active: Boolean = false,
    )
    
    fun refresh() {
        val activeByte =
            if (isHeadsetActive) BluetoothActive.HEADSET.code else {
                if (isA2dpActive) BluetoothActive.A2DP.code else 0
            }
        Pebble.sendIntent(context, MsgType.BT) {
            putExtra(Const.EXTRA_BTID, displayDevice.name)
            putExtra(Const.EXTRA_BTC, displayDevice.battery)
            putExtra(Const.EXTRA_BTON, activeByte)
        }
    }
    
    inner class BluetoothProxy :
        BluetoothProfile.ServiceListener {
        var a2dpProxy: BluetoothA2dp? = null
        var headsetProxy: BluetoothHeadset? = null
        
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile == BluetoothProfile.A2DP) a2dpProxy = proxy as BluetoothA2dp
            else headsetProxy = proxy as BluetoothHeadset
            rescan()
        }
        
        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.A2DP) a2dpProxy = null
            else headsetProxy = null
            rescan()
        }
        
        fun rescan() {
            var headsetDevice = ConnectedDevice()
            headsetProxy?.connectedDevices?.forEach {
                headsetDevice = ConnectedDevice(
                    it.name.cleanLE(),
                    it.getBatteryLevel(),
                    headsetProxy?.isAudioConnected(it) ?: false
                )
            }
            
            var a2dpDevice = ConnectedDevice()
            a2dpProxy?.connectedDevices?.forEach {
                a2dpDevice = ConnectedDevice(
                    it.name.cleanLE(),
                    it.getBatteryLevel(),
                    a2dpProxy?.isA2dpPlaying(it) ?: false
                )
            }

            // In order of priority
            displayDevice = when {
                headsetDevice.active -> headsetDevice
                a2dpDevice.active -> a2dpDevice
                headsetDevice.name.isNotEmpty() -> headsetDevice
                a2dpDevice.name.isNotEmpty() -> a2dpDevice
                else -> ConnectedDevice()
            }
            
            isHeadsetActive = headsetDevice.active
            isA2dpActive = a2dpDevice.active
            
            refresh()
        }
    }
    
    private fun BluetoothDevice.getBatteryLevel(): Int {
        try {
            val method = this.javaClass.getMethod("getBatteryLevel")
            var result = method.invoke(this) as Int
            if (result !in 1..100) result = 0
            return result
        } catch (_: Exception) {
            return 0
        }
    }
    
    private fun String.cleanLE(): String {
        return this.removePrefix("LE-")
    }
}
