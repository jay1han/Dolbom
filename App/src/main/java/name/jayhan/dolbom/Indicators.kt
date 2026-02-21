package name.jayhan.dolbom

import android.app.Notification
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.service.notification.StatusBarNotification
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow

data class SingleIndicator(
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
    val timeInfo: Long = 0L,
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
    ): FilterType? {
        if (this.filterText.isNotEmpty() &&
            this.filterType.listNonEmptyExtrasOf(notification.extras).any { (_, value) ->
                value.contains(this.filterText)
            }
        ) return this.filterType
        else return null
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
        
        fun fromText(
            string: String,
        ): SingleIndicator? {
            val lines = string.split('\n', limit = 8)
            val elements = mutableMapOf<String, String>()
            lines.forEach {
                val keyvalue = it.split("=", limit = 2)
                if (keyvalue.size == 2) {
                    val (key, value) = keyvalue
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

    fun toText(): String {
        return StringBuilder().apply {
            append("letter=$letter\n")
            append("package=$packageName\n")
            append("channel=$channelId\n")
            append("filter=$filterText\n")
            append("type=${filterType.name}\n")
            append("flags=${flags()}\n")
        }.toString()
    }

}

object Indicators: Backupable
{
    // TODO: Should be unmutable
    private var allIndicators = mutableListOf<SingleIndicator>()
    val allFlow = MutableStateFlow(mutableListOf<SingleIndicator>())
    private lateinit var savedSettings: SharedPreferences
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

        var match: SingleIndicator? = null
        var score = 0

        for (indicator in allIndicators) {
            if (indicator.packageName == packageName) {
                if (indicator.channelId.isEmpty()) {
                    if (indicator.filterText.isEmpty()) {
                        if (score < 10) {
                            match = indicator
                            score = 10
                        }
                    } else {
                        val filterType = indicator.matches(notification)
                        if (filterType != null) {
                            val thisScore = 20 + filterType.r
                            if (score < thisScore) {
                                match = indicator
                                score = thisScore
                            }
                        }
                    }
                } else {
                    if (channelId.contains(indicator.channelId)) {
                        if (indicator.filterText.isEmpty()) {
                            if (score < 50) {
                                match = indicator
                                score = 50
                            }
                        } else {
                            val filterType = indicator.matches(notification)
                            if (filterType != null) {
                                val thisScore = 60 + filterType.r
                                match = indicator
                                if (score < thisScore) {
                                    match = indicator
                                    score = thisScore
                                }
                            }
                        }
                    }
                }
            }
        }

        if (match == null) {
            if (notification.flags.maskAny(
                    Notification.FLAG_ONGOING_EVENT or
                            Notification.FLAG_LOCAL_ONLY or
                            Notification.FLAG_FOREGROUND_SERVICE or
                            Notification.FLAG_NO_CLEAR
                )
            ) return null
            else return SingleIndicator.Other
        }

        if (match.ignore) return null
        if (notification.flags.maskAny(
                Notification.FLAG_LOCAL_ONLY or
                        Notification.FLAG_FOREGROUND_SERVICE
            ) && !match.local) return null
        if (notification.flags.maskAny(
                Notification.FLAG_ONGOING_EVENT or
                        Notification.FLAG_NO_CLEAR
            ) && !match.ongoing) return null
        return match
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
            Notifications.getApplicationName(it.packageName) + ":${it.packageName}:${it.channelId}:${it.filterType.r}:${it.filterText}"
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
        allFlow.value = allIndicators
    }
    
    override fun toText(): String {
        val stringBuilder = StringBuilder().apply {
            append(Const.BACKUP_INDICATORS + "\n")
            allIndicators.forEach {
                append(it.toText())
                append(Const.BACKUP_SEPARATOR + "\n")
            }
        }
        return stringBuilder.toString()
    }
    
    override fun fromText(text: String): Boolean {
        if (!text.startsWith(Const.BACKUP_INDICATORS)) return false

        val newList = mutableListOf<SingleIndicator>()
        for (multiline in text.removePrefix(Const.BACKUP_INDICATORS)
            .split(Const.BACKUP_SEPARATOR)) {
            val indicator = SingleIndicator.fromText(multiline)
            if (indicator != null) newList.add(indicator)
        }
        saveList(newList)
        return true
    }

    override val filenamePart = "Indicators"

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

    fun listNonEmptyExtrasOf(
        extras: Bundle
    ): Map<String, String> {
        return extrasMap[this]!!.associateWith {
            extras.getCharSequence(it, "").toString()
        }.filter { it.value.isNotEmpty() }
    }

    companion object {
        val Strings = FilterType.entries.map { it.r }
        private val extrasMap = mapOf(
            Title to listOf(
                Notification.EXTRA_TITLE,
                Notification.EXTRA_CONVERSATION_TITLE,
                Notification.EXTRA_TITLE_BIG,
            ),
            Subtitle to listOf(
                Notification.EXTRA_PEOPLE_LIST,
                Notification.EXTRA_SUB_TEXT,
            ),
            Text to listOf(
                Notification.EXTRA_SUMMARY_TEXT,
                Notification.EXTRA_INFO_TEXT,
                Notification.EXTRA_TEXT,
            ),
            Long to listOf(
                Notification.EXTRA_TEXT_LINES,
                Notification.EXTRA_BIG_TEXT,
            )
        )


        fun index(index: Int): FilterType {
            for (filterType in FilterType.entries) {
                if (index == filterType.ordinal) return filterType
            }
            return Text
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
