package name.jayhan.dolbom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

class BatteryReceiver(
    private val context: Context
):
    BroadcastReceiver()
{
    private var pluggedTo = 0
    private var percent = 0
    private var isCharging = false

    init {
        refresh()

        val batteryFilter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
//            addAction(Intent.ACTION_POWER_CONNECTED)
//            addAction(Intent.ACTION_POWER_DISCONNECTED)
//            addAction(BatteryManager.ACTION_CHARGING)
//            addAction(BatteryManager.ACTION_DISCHARGING)
        }
        context.registerReceiver(this, batteryFilter, Context.RECEIVER_EXPORTED)
    }
    
    fun deinit() {
        context.unregisterReceiver(this)
    }

    override fun onReceive(context: Context, intent: Intent) {
        when(intent.action) {
            Intent.ACTION_BATTERY_CHANGED -> {
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
                pluggedTo = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 1)
                percent = (100.0 * level.toFloat() / scale).toInt()
            }

            Intent.ACTION_POWER_CONNECTED -> {
                pluggedTo = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
            }

            Intent.ACTION_POWER_DISCONNECTED -> {
                pluggedTo = 0
                isCharging = false
            }
            BatteryManager.ACTION_CHARGING -> isCharging = true
            BatteryManager.ACTION_DISCHARGING -> isCharging = false
        }
        refresh()
    }

    fun refresh() {
        Pebble.sendIntent(context, MsgType.PHONE_CHG) {
            putExtra(Const.EXTRA_PHONE_BATT, percent)
            putExtra(Const.EXTRA_PHONE_CHG, if (isCharging) 1 else 0)
            putExtra(Const.EXTRA_PHONE_PLUG, pluggedTo)
        }
    }
}

