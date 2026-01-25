package name.jayhan.dolbom

import android.app.Notification
import android.content.Context
import android.content.SharedPreferences
import android.service.notification.StatusBarNotification
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow

class SingleIndicator(
    val packageName: String = "",
    val channelId: String = "",
    val filterText: String = "",
    val filterType: FilterType = FilterType.Title,
    val letter: Char = ' ',
    val ignore: Boolean = false,
    val sticky: Boolean = false,
    val ongoing: Boolean = false,
    val relay: Boolean = false,
    var timeInfo: Long = 0L,
) {
    fun equals(
        other: SingleIndicator
    ): Boolean {
        return packageName == other.packageName &&
                channelId == other.channelId &&
                filterText == other.filterText &&
                filterType == other.filterType
    }

    fun storeKey(): String {
        return "$letter\n$packageName\n$channelId\n$filterText\n${filterType.name}"
    }
    
    fun storeValue(): String {
        return StringBuilder().apply {
            if (sticky) append("S")
            if (ongoing) append("O")
            if (ignore) append("I")
        }.toString()
    }
    
    fun matches(
        notification: Notification
    ): Boolean {
        return this.filterText.isNotEmpty() &&
                this.filterType.listExtrasOf(notification).any { (_, value) ->
                    value.contains(this.filterText)
                }
    }



    companion object {
        val Other = SingleIndicator(letter = '+')
        fun fromKeyValue(
            key: String,
            value: String
        ): SingleIndicator {
            val elements = key.split('\n', limit = 5)
            val sticky = value.contains("S")
            val ongoing = value.contains("O")
            val ignore = value.contains("I")
            return SingleIndicator(
                packageName = elements[1],
                channelId = elements[2],
                filterText = elements[3],
                filterType = FilterType.valueOf(elements[4]),
                letter = elements[0][0],
                ignore = ignore,
                sticky = sticky,
                ongoing = ongoing,
            )
        }
    }
}

object Indicators
{
    private var allIndicators = mutableListOf<SingleIndicator>()
    val allFlow = MutableStateFlow(mutableListOf<SingleIndicator>())
    private lateinit var savedSettings: SharedPreferences

    fun init(context: Context) {
        savedSettings = context.getSharedPreferences(
            Const.PREF_INDIC,
            Context.MODE_PRIVATE
        )

        val newList = mutableListOf<SingleIndicator>()
        savedSettings.all.forEach {
            try {
                newList.add(SingleIndicator.fromKeyValue(it.key, it.value as String))
            } catch (_: ClassCastException) {}
        }

        saveList(newList)
    }

    fun findIndicator(
        sbn: StatusBarNotification
    ): SingleIndicator? {
        val packageName = sbn.packageName
        val notification = sbn.notification
        val channelId = notification.channelId

        var found: SingleIndicator? = null
        var match = 0

        for (indicator in allIndicators) {
            if (indicator.packageName == packageName) {
                if (indicator.channelId.isEmpty()) {
                    if (indicator.filterText.isEmpty()) {
                        if (match < 10) {
                            found = indicator
                            match = 10
                        }
                    } else {
                        if (indicator.matches(notification)) {
                            if (match < 20) {
                                found = indicator
                                match = 20
                            }
                        }
                    }
                } else {
                    if (channelId.contains(indicator.channelId)) {
                        if (indicator.filterText.isEmpty()) {
                            if (match < 50) {
                                found = indicator
                                match = 50
                            }
                        } else {
                            if (indicator.matches(notification)) {
                                found = indicator
                                break
                            }
                        }
                    }
                }
            }
        }

        if (found == null) return SingleIndicator.Other
        if (found.ignore) return null
        return found
    }

    fun add(
        indicator: SingleIndicator
    ) {
        allIndicators.add(indicator)
        saveList(allIndicators)
    }

    private fun saveList(
        newList: List<SingleIndicator>
    ) {
        allIndicators = newList.sortedBy {
            Notifications.getApplicationName(it.packageName) + ":${it.channelId}:${it.filterText}"
        }.toMutableList()

        savedSettings.edit {
            clear()
            for (indicator in allIndicators) {
                putString(indicator.storeKey(), indicator.storeValue())
            }
            commit()
        }

        allFlow.value = allIndicators
    }

    fun remove(
        indicator: SingleIndicator
    ) {
        val newList = allIndicators.filterNot {
            it.equals(indicator)
        }
        saveList(newList)
    }

    fun reset() {
        saveList(listOf())
    }
}

enum class FilterType {
    Title { override val r = R.string.filter_title },
    Subtitle { override val r = R.string.filter_sub },
    Text { override val r = R.string.filter_text },
    Long { override val r = R.string.filter_long };
    abstract val r: Int
    
    fun listExtrasOf(
        notification: Notification,
    ): Map<String, String> {
        when (this) {
            Title -> return listExtrasOf(notification, listOf(
                Notification.EXTRA_TITLE,
                Notification.EXTRA_CONVERSATION_TITLE,
                Notification.EXTRA_TITLE_BIG,
            ))
            
            Subtitle -> return listExtrasOf(notification, listOf(
                Notification.EXTRA_PEOPLE_LIST,
                Notification.EXTRA_SUB_TEXT,
            ))
            
            Text -> return listExtrasOf(notification, listOf(
                Notification.EXTRA_SUMMARY_TEXT,
                Notification.EXTRA_INFO_TEXT,
                Notification.EXTRA_TEXT,
            ))
            
            Long -> return listExtrasOf(notification, listOf(
                Notification.EXTRA_TEXT_LINES,
                Notification.EXTRA_BIG_TEXT,
            ))
        }
    }

    companion object {
        val Strings = FilterType.entries.map { it.r }
        
        fun index(index: Int): FilterType {
            for (filterType in FilterType.entries) {
                if (index == filterType.ordinal) return filterType
            }
            return Text
        }
        
        private fun listExtrasOf(
            notification: Notification,
            extraList: List<String>
        ): Map<String, String> {
            // Kotlin's very sweet syntactic sugar
            return extraList.associateWith {
                notification.extras.getCharSequence(it, "").toString()
            }.filter { it.value.isNotEmpty() }
        }
    }
}

val PreviewIndicators = listOf(
    SingleIndicator("com.android.google.apps.dialer", letter = 'C'),
    SingleIndicator("com.android.google.apps.messaging", letter = 'T'),
    SingleIndicator("com.android.google.apps.gm", "jay", letter = 'j'),
    SingleIndicator("com.android.google.apps.gm", "pebble", letter = 'p'),
    SingleIndicator("com.android.google.apps.gm", letter = ' ', ignore = true),
    SingleIndicator("com.whatsapp", letter = 'W'),
    SingleIndicator("com.kakao.talk", letter = ' ', ignore = true),
    SingleIndicator("com.kakao.talk", filterText = "Bob", letter = 'b'),
    SingleIndicator("com.kakao.talk", "talk", "Alice", FilterType.Long, 'b'),
)

val PreviewActiveList = listOf(
    "com.android.google.apps.messaging",
    "com.android.google.apps.messaging",
    "com.whatsapp"
)

val PreviewAllList = listOf(
    "com.android.google.apps.messaging",
    "com.android.google.apps.gm",
    "com.whatsapp"
)
