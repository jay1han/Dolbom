package name.jayhan.dolbom

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow

class NotificationListener:
    NotificationListenerService() {
    private lateinit var context: Context
    private lateinit var notiMan: NotificationManager
    private lateinit var alarmMan: AlarmManager

    override fun onListenerConnected() {
        super.onListenerConnected()
        
        Log.v(Const.TAG, "Listener connected")
        context = applicationContext
        notiMan = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        
        val channel = NotificationChannel(
            Const.CHANNEL_RELAY,
            "Dolbom relay",
            NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                setShowBadge(false)
                enableVibration(true)
                setBypassDnd(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                description = "Dolbom relay for ongoing notification"
            }
            notiMan.createNotificationChannel(channel)
        
        alarmMan = context.getSystemService(ALARM_SERVICE) as AlarmManager
        
        Notifications.onNotification(context, activeNotifications)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        
        if (sbn != null) {
            val indicator = Indicators.findIndicator(sbn)
            if (indicator?.relay?:false) {
                val notification = sbn.notification
                
                if (indicator.repeat) {
                    RelayAlarm.new(
                        indicator.packageName,
                        alarmMan,
                        callback = { index ->
                            notiMan.notify(
                                indicator.packageName,
                                Const.NOTI_RELAY,
                                buildRelayNotification(
                                    context,
                                    notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString(),
                                    notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString(),
                                    notification.smallIcon,
                                    index
                                )
                            )
                        }
                    )
                }
            }
        }
        
        Notifications.onNotification(context, activeNotifications)
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)

        if (sbn != null) {
            val indicator = Indicators.findIndicator(sbn)
            if (indicator?.relay?:false) {
                notiMan.cancel(
                    indicator.packageName,
                    Const.NOTI_RELAY
                )
                
                if (indicator.repeat) {
                    RelayAlarm.cancel(indicator.packageName)
                }
            }
        }
        
        Notifications.onNotification(context, activeNotifications)
    }
    
    override fun onListenerDisconnected() {
        Log.v(Const.TAG, "Listener disconnected")
        super.onListenerDisconnected()
    }

}

fun buildRelayNotification(
    context: Context,
    title: String,
    text: String,
    icon: Icon,
    index: Int
): Notification {
    return Notification.Builder(
        context,
        Const.CHANNEL_RELAY
    ).apply {
        setContentTitle(title)
        setContentText(text + "...%d".format(index))
        setSmallIcon(icon)
        setOnlyAlertOnce(false)
    }.build()
}

class RelayAlarm(
    private val tag: String,
    private val alarmMan: AlarmManager,
    private val callback: (Int) -> Unit
): AlarmManager.OnAlarmListener
{
    private var active = true
    private var index = 0
    
    init {
        onAlarm()
    }
    
    override fun onAlarm() {
        if (!active) {
            cancel()
            return
        }
        
        callback(index)
        index += 1
        
        alarmMan.set(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + Const.RELAY_INTERVAL_S * 1000,
            tag,
            this,
            null,
        )
    }
    
    fun cancel() {
        alarmMan.cancel(this)
        active = false
    }
    
    companion object {
        private val alarms = mutableMapOf<String, RelayAlarm>()
        
        fun new(
            tag: String,
            alarmMan: AlarmManager,
            callback: (Int) -> Unit
        ): RelayAlarm {
            val alarm = RelayAlarm(tag, alarmMan, callback)
            alarms[tag] = alarm
            return alarm
        }
        
        fun cancel(tag: String) {
            alarms.remove(tag)?.cancel()
        }
    }
}

class NotificationDump(
    val packageName: String,
    val channelId: String,
    val extraMap: Map<FilterType, Map<String, String>>,
    val flags: Int
) {
    companion object {
        fun fromSBN(
            sbn: StatusBarNotification,
        ): NotificationDump {
            val notification = sbn.notification
            val extraMap = FilterType.entries.toList().associateWith {
                it.listNonEmptyExtrasOf(notification.extras)
            }
            
            return NotificationDump(
                sbn.packageName,
                notification.channelId,
                extraMap,
                notification.flags
            )
        }
    }
}

object Notifications : BroadcastReceiver()
{
    private lateinit var packMan: PackageManager

    val activeFlow = MutableStateFlow<List<String>>(mutableListOf())
    val allFlow = MutableStateFlow<List<String>>(mutableListOf())
    private var savedNotifications: Array<StatusBarNotification>? = null
    private var mapPackageToName = mapOf<String, String>()
    
    var dump = mutableListOf<NotificationDump>()
    val dumpFlow = MutableStateFlow(0)
    var indicators = ""

    fun onNotification(
        context: Context,
        activeNotifications: Array<StatusBarNotification>
    ) {
        savedNotifications = activeNotifications
        if (!this::packMan.isInitialized) return
        process(context)
    }
    
    private fun process(
        context: Context
    ) {
        val activeList = mutableListOf<String>()
        dump = mutableListOf()
        Accumulator.clear()
        
        if (savedNotifications != null) {
            for (sbn in savedNotifications) {
                if (sbn.packageName.startsWith(BuildConfig.APPLICATION_ID)) continue
                
                dump.add(NotificationDump.fromSBN(sbn))
                activeList.add(sbn.packageName)
                Accumulator.add(sbn)
            }
            activeFlow.value = activeList.dedup()
            dumpFlow.value = dump.size
    
            indicators = Accumulator.getCompact().take(Const.MAX_NOTI_INDICATORS)
    
            Pebble.sendIntent(context, MsgType.NOTI) {
                putExtra(Const.EXTRA_NOTI, indicators)
            }
        }
        
        Pebble.updateNotification(context)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_PACKAGE_ADDED ||
            intent?.action == Intent.ACTION_PACKAGE_FULLY_REMOVED) {
            updateAllList()
        }
    }

    fun init(
        context: Context
    ) {
        packMan = context.packageManager

        val filter = IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED)
            }
        context.registerReceiver(this, filter, Context.RECEIVER_EXPORTED)

        updateAllList()
        Indicators.init(context)
        process(context)
    }

    private fun updateAllList() {
        val packageList = packMan
            .queryIntentActivities(
                Intent(Intent.ACTION_MAIN)
                    .apply { addCategory(Intent.CATEGORY_LAUNCHER) },
                PackageManager.MATCH_ALL
            )
            .filter { !it.activityInfo.packageName.startsWith(BuildConfig.APPLICATION_ID)}
            .map { it.activityInfo.packageName }

        mapPackageToName = packageList.associateWith { packageName ->
            val appInfo = packMan.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            packMan.getApplicationLabel(appInfo).toString()
        }

        allFlow.value = mapPackageToName.toList()
            .sortedBy { it.second }
            .map { it.first }
    }

    fun refresh(context: Context) = process(context)
    
    fun getApplicationName(packageName: String): String {
        return mapPackageToName[packageName] ?: ""
    }
    
    object Accumulator {
        private var litList = mutableListOf<SingleIndicator>()
        var stickyCount = MutableStateFlow(0)
        
        fun clear() {
            litList = litList.filter { it.sticky }.toMutableList()
        }
        
        fun listSticky(): List<SingleIndicator> {
            return litList.filter { it.sticky }.map { it }
        }
    
        fun clearSticky() {
            litList = litList.filter { !it.sticky }.toMutableList()
            stickyCount.value = 0
        }
        
        fun add(sbn: StatusBarNotification) {
            val indicator = Indicators.findIndicator(sbn) ?: return
            
            litList = litList.filter { !indicator.equals(it) }.toMutableList()
            if (!sbn.isOngoing || indicator.ongoing) {
                indicator.timeInfo = sbn.notification.`when`
                litList.add(indicator)
            }
            stickyCount.value = litList.filter { it.sticky }.size
        }
        
        fun getCompact(): String {
            val letters = litList
                .sortedBy { it.timeInfo }
                .reversed()
                .map { it.letter }
                .distinct()
                .toMutableList()
            return letters.joinToString("")
        }
    }
}

fun List<String>.dedup(): List<String> {
    val newList = mutableListOf<String>()
    for (item in this) {
        if (!newList.contains(item)) newList.add(item)
    }
    return newList
}
