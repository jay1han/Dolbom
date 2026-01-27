package name.jayhan.dolbom

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.getBroadcast
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log

class PebbleService:
    Service()
{
    private lateinit var context: Context
    private lateinit var notiMan: NotificationManager
    private lateinit var batteryReceiver: BatteryReceiver
    private lateinit var bluetoothReceiver: BluetoothReceiver
    private lateinit var wifiCallback: WifiCallback
    private lateinit var internetCallback: InternetCallback
    private lateinit var phoneCallback: PhoneCallback
    private lateinit var zenRule: ZenRule

    private val receiver = Receiver()
    private lateinit var reviveIntent: PendingIntent
    private lateinit var launchIntent: PendingIntent
    private lateinit var phoneFinder: PhoneFinder
    private val protocol = Protocol()
    
    private lateinit var alarmMan: AlarmManager
    private val alarmListener = AlarmListener()

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.v(Const.TAG, "Service starting Id=$startId")
        if (!Permissions.canStartService()) {
            Log.v(Const.TAG, "Permissions missing")
            stopSelf()
            return START_NOT_STICKY
        }

        context = applicationContext

        launchIntent = PendingIntent.getActivity(
            context, Const.LAUNCH_REQUEST,
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_IMMUTABLE
        )
        reviveIntent = getBroadcast(
            context, Const.REVIVE_REQUEST,
            Intent(Const.INTENT_RESTART),
            PendingIntent.FLAG_IMMUTABLE
        )

        val filter = IntentFilter().apply {
            addAction(Const.INTENT_RESTART)
            addAction(Const.INTENT_STOP)
            addAction(Const.INTENT_UPDATE)
            addAction(Const.INTENT_REFRESH)
            addAction(Const.INTENT_FULLY_CHARGED)
            addAction(Const.INTENT_DND)
            addAction(Const.INTENT_CLEAR_STICKY)
            addAction(Const.INTENT_SEND_PEBBLE)
            addAction(Const.INTENT_PEBBLE_PONG)
        }
        context.registerReceiver(receiver, filter,RECEIVER_EXPORTED)

        notiMan = context.getSystemService(NOTIFICATION_SERVICE)
                as NotificationManager
        
        val ongoingChannel = NotificationChannel(
            Const.CHANNEL_ID,
            getString(R.string.app_title),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_SECRET
            description = getString(R.string.channel_description)
            importance = NotificationManager.IMPORTANCE_LOW
        }
        
        notiMan.createNotificationChannel(ongoingChannel)
        
        val alertChannel = NotificationChannel(
            Const.CHANNEL_ALERT,
            getString(R.string.app_title),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            description = "Dolbom Alert"
            importance = NotificationManager.IMPORTANCE_HIGH
        }
        
        notiMan.createNotificationChannel(alertChannel)
        
        startForeground(Const.NOTI_SERVICE, buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        )

        alarmMan = context.getSystemService(ALARM_SERVICE)
                as AlarmManager
        
        try {
            restartService()
        } catch (e: Exception) {
            Log.e(Const.TAG, e.toString())
            stopSelf()
        }

        return super.onStartCommand(intent, flags, startId)
    }
    
    override fun onDestroy() {
        Log.v(Const.TAG, "Destroy Service")
        stopService()
        stopModules()
        super.onDestroy()
    }
    
    fun updateNotification() {
        Log.v(Const.TAG, "Update Notification")
        notiMan.notify(Const.NOTI_SERVICE, buildNotification())
    }
    
    fun buildNotification(): Notification {
        return Notification.Builder(
            context,
            Const.CHANNEL_ID
        ).apply {
            setFlag(Notification.FLAG_FOREGROUND_SERVICE, true)
            setOngoing(true)
            setDeleteIntent(reviveIntent)
            setContentIntent(launchIntent)
            
            if (Pebble.isConnected.value) {
                val estimate =
                    if (History.historyData.cycleRate > 0f)
                        (Pebble.watchInfo.battery.toFloat() - 10f) / History.historyData.cycleRate
                    else 0f
                setContentTitle("${Pebble.watchInfo.modelString()} ${Pebble.watchInfo.battery}%")
                setContentText(
                    "\u2590%s\u258c %s".format(
                        Notifications.indicators,
                        if (Pebble.watchInfo.charging) "\u26a1" else "",
                    ) + "%.1f days".format(
                        estimate
                    )
                )
            } else {
                setContentTitle("Disconnected")
                setContentText("")
            }
            setSmallIcon(R.mipmap.ic_launcher)
            setVisibility(Notification.VISIBILITY_SECRET)
        }.build()
    }
    
    fun restartService() {
        Log.v(Const.TAG, "RestartService")
        
        Notifications.init(context)
        History.init(context)
        Pebble.init(context)
        
        stopModules()
        zenRule = ZenRule(context)
        phoneFinder = PhoneFinder(context)
        batteryReceiver = BatteryReceiver(context)
        bluetoothReceiver = BluetoothReceiver(context)
        wifiCallback = WifiCallback(context)
        internetCallback = InternetCallback(context)
        phoneCallback = PhoneCallback(context)
        alarmListener.startTimer()
        Log.v(Const.TAG, "Modules started")
    }
    
    fun refreshService() {
        Log.v(Const.TAG, "RefreshService")
        protocol.reset()
        zenRule.refresh()
        batteryReceiver.refresh()
        bluetoothReceiver.refresh()
        wifiCallback.refresh()
        internetCallback.refresh()
        phoneCallback.refresh()
        Notifications.refresh(context)
    }
    
    private fun stopService() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        if (this::context.isInitialized) {
            Pebble.deinit(context)
            notiMan.deleteNotificationChannel(Const.CHANNEL_ID)
            context.unregisterReceiver(receiver)
        }
    }
    
    private fun stopModules() {
        if (this::zenRule.isInitialized) zenRule.deinit()
        if (this::batteryReceiver.isInitialized) batteryReceiver.deinit()
        if (this::bluetoothReceiver.isInitialized) bluetoothReceiver.deinit()
        if (this::wifiCallback.isInitialized) wifiCallback.deinit()
        if (this::internetCallback.isInitialized) internetCallback.deinit()
        if (this::phoneCallback.isInitialized) phoneCallback.deinit()
        if (this::phoneFinder.isInitialized) phoneFinder.deinit()
        Log.v(Const.TAG, "Modules stopped")
    }
    
    inner class AlarmListener:
        AlarmManager.OnAlarmListener
    {
        private var countAlarmed = 0
        
        fun startTimer() {
            alarmMan.set(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + Const.PING_INTERVAL_S * 1000,
                null,
                this,
                null
            )
        }
        
        fun clearAlarm() {
            if (countAlarmed > 0) {
                if (countAlarmed > 1) refreshService()
                countAlarmed = 0
            }
            if (!Pebble.isConnected.value) {
                Pebble.isConnected.value = true
                updateNotification()
            }
            startTimer()
        }
        
        override fun onAlarm() {
            Pebble.sendIntent(context, MsgType.PING) {}
            if (countAlarmed < 10) countAlarmed += 1
            if (countAlarmed >= 2) {
                Pebble.isConnected.value = false
                updateNotification()
            }
            startTimer()
        }
    }

    inner class Receiver: BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (!Permissions.allGranted) {
                Log.v(Const.TAG, "Permissions revoked, stop")
                stopService()
                stopSelf()
            }
            
            when(intent.action) {
                Const.INTENT_RESTART -> {
                    Log.v(Const.TAG, "Intent: Restart service")
                    restartService()
                }

                Const.INTENT_UPDATE -> {
                    Log.v(Const.TAG, "Intent: Update notification")
                    updateNotification()
                }

                Const.INTENT_REFRESH -> {
                    Log.v(Const.TAG, "Intent: Refresh service")
                    refreshService()
                }
                
                Const.INTENT_FULLY_CHARGED -> {
                    Log.v(Const.TAG, "Intent: Fully charged")
                    notiMan.notify(
                        Const.NOTI_FULLY_CHARGED,
                        Notification.Builder(
                            context,
                            Const.CHANNEL_ALERT
                        ).apply {
                            setSmallIcon(R.mipmap.ic_launcher)
                            setContentTitle("Fully charged")
                            setContentText(Pebble.watchInfo.modelString())
                            setVisibility(Notification.VISIBILITY_PUBLIC)
                        }.build()
                    )
                    updateNotification()
                }
                
                Const.INTENT_STOP -> {
                    Log.v(Const.TAG, "Intent: Stop service")
                    stopSelf()
                }
                
                Const.INTENT_DND -> zenRule.toggle()
                
                Const.INTENT_CLEAR_STICKY -> {
                    Notifications.Accumulator.clearSticky()
                    Notifications.refresh(context)
                }
                
                Const.INTENT_PEBBLE_PONG -> alarmListener.clearAlarm()
                
                Const.INTENT_SEND_PEBBLE -> protocol.send(context, intent)
            }
        }
    }
}
