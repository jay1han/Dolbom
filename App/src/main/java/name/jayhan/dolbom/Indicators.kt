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
    val repeat: Boolean = false,
    val local: Boolean = false,
    var timeInfo: Long = 0L,
) {
    constructor(
        packageName: String = "",
        channelId: String = "",
        filterText: String = "",
        filterType: FilterType = FilterType.Title,
        letter: Char = ' ',
        flags: String = "",
        timeInfo: Long = 0L,
    ): this(
        packageName = packageName,
        channelId = channelId,
        filterText = filterText,
        filterType = filterType,
        letter = letter,
        ignore = flags.contains("I"),
        sticky = flags.contains("S"),
        ongoing = flags.contains("O"),
        relay = flags.contains("R"),
        repeat = flags.contains("r"),
        local = flags.contains("L"),
        timeInfo = timeInfo,
    )
    
    fun flags(): String {
        return listOf(
            if (ignore) "I" else "",
            if (sticky) "S" else "",
            if (ongoing) "O" else "",
            if (relay) "R" else "",
            if (repeat) "r" else "",
            if (local) "L" else "",
        ).joinToString(separator = "")
    }
    
    fun equals(
        other: SingleIndicator
    ): Boolean {
        return packageName == other.packageName &&
                channelId == other.channelId &&
                filterText == other.filterText &&
                filterType == other.filterType
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
            return SingleIndicator(
                packageName = elements[1],
                channelId = elements[2],
                filterText = elements[3],
                filterType = FilterType.valueOf(elements[4]),
                letter = elements[0][0],
                flags = value
            )
        }
        
        fun fromString(
            string: String,
        ): SingleIndicator? {
            val lines = string.split('\n', limit = 8)
            val elements = mutableMapOf<String, String>()
            lines.forEach {
                val key_value = it.split("=", limit = 2)
                if (key_value.size == 2) {
                    val (key, value) = key_value
                    elements[key] = value
                }
            }
            return if (elements.size > 1)
                SingleIndicator(
                    packageName = elements["package"] ?: "",
                    channelId = elements["channel"] ?: "",
                    filterText = elements["filter"] ?: "",
                    filterType = FilterType.valueOf(elements["type"] ?: ""),
                    letter = elements["letter"]?.get(0) ?: ' ',
                    flags = elements["flags"] ?: "",
                )
            else null
        }
    }
}

object Indicators
{
    // TODO: Should be unmutable
    private var allIndicators = mutableListOf<SingleIndicator>()
    val allFlow = MutableStateFlow(mutableListOf<SingleIndicator>())
    private lateinit var savedSettings: SharedPreferences
    val backedUp = MutableStateFlow(false)
    val count = MutableStateFlow(0)

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
        if (found.ignore ||
            (found.local && !notification.flags.maskAll(Notification.FLAG_LOCAL_ONLY))
            )
            return null
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
                putString(
                    with(indicator) {
                        "$letter\n$packageName\n$channelId\n$filterText\n${filterType.name}"
                    },
                    indicator.flags()
                )
            }
            commit()
        }

        count.value = allIndicators.size
        backedUp.value = false
        allFlow.value = allIndicators
    }
    
    fun toText(): String {
        val stringBuilder = StringBuilder().apply {
            allIndicators.forEachIndexed { index, indicator ->
                append("[$index]\n")
                with(indicator) {
                    append("letter=$letter\n")
                    append("package=$packageName\n")
                    append("channel=$channelId\n")
                    append("filter=$filterText\n")
                    append("type=${filterType.name}\n")
                    append("flags=${flags()}\n")
                    append("END\n")
                }
            }
        }
        return stringBuilder.toString()
    }
    
    fun fromText(text: String) {
        val newList = mutableListOf<SingleIndicator>()
        for (multiline in text.split("END\n")) {
            val indicator = SingleIndicator.fromString(multiline)
            if (indicator != null) newList.add(indicator)
        }
        saveList(newList)
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
    SingleIndicator("com.android.google.apps.messaging", letter = 'T', sticky = true, relay = true),
    SingleIndicator("com.android.google.apps.gm", "jay", letter = 'j', relay = true),
    SingleIndicator("com.android.google.apps.gm", "pebble", letter = 'p', relay = true, repeat = true),
    SingleIndicator("com.android.google.apps.gm", letter = ' ', flags = "I"),
    SingleIndicator("com.whatsapp", letter = 'W', sticky = true),
    SingleIndicator("com.kakao.talk", letter = ' ', flags = "I"),
    SingleIndicator("com.kakao.talk", filterText = "Bob", letter = 'b', flags = "SrR"),
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
