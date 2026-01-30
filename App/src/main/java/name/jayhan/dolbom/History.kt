package name.jayhan.dolbom

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.isDistantPast

class HistoryData(
    val historyDate: Instant = Instant.DISTANT_PAST,
    val historyCycles: Int = 0,
    val historyRate: Float = 0f,
    val cycleDate: Instant = Instant.DISTANT_PAST,
    val cycleLevel: Int = 0,
    val cycleRate: Float = 0f,
    val nowPlugged: Boolean = false,
) {
    fun set(
        historyDate: Instant? = null,
        historyCycles: Int? = null,
        historyRate: Float? = null,
        cycleDate: Instant? = null,
        cycleLevel: Int? = null,
        cycleRate: Float? = null,
        nowPlugged: Boolean? = null,
    ): HistoryData {
        return HistoryData(
            historyDate = historyDate ?: this.historyDate,
            historyCycles = historyCycles ?: this.historyCycles,
            historyRate = historyRate ?: this.historyRate,
            cycleDate = cycleDate ?:this.cycleDate,
            cycleLevel = cycleLevel ?: this.cycleLevel,
            cycleRate = cycleRate ?: this.cycleRate,
            nowPlugged = nowPlugged ?: this.nowPlugged,
        )
    }
    
    companion object {
        fun read(prefs: SharedPreferences): HistoryData {
            return HistoryData(
                historyDate = longToDate(prefs.getLong(Const.HIST_INIT_DATE, 0)),
                historyCycles = prefs.getInt(Const.HIST_CYCLES, 0),
                historyRate = prefs.getFloat(Const.HIST_RATE, 0f),
                cycleDate = longToDate(prefs.getLong(Const.HIST_CYCLE_TIME, 0L)),
                cycleLevel = prefs.getInt(Const.HIST_CYCLE_LEVEL, 0),
                cycleRate = prefs.getFloat(Const.CYCLE_RATE, 0f),
                nowPlugged = prefs.getBoolean(Const.HIST_PLUG_STATE, false),
            )
        }
    }
}

private fun longToDate(dateLong: Long): Instant {
    return (
            if (dateLong == 0L) Instant.DISTANT_PAST
            else Instant.fromEpochSeconds(dateLong)
            )
}

object History: Backupable {
    var historyData = HistoryData()
    val historyFlow = MutableStateFlow(HistoryData())
    private lateinit var savedHistory: SharedPreferences

    fun init(context: Context) {
        savedHistory = context.getSharedPreferences(
            Const.PREF_HISTORY,
            Context.MODE_PRIVATE
        )

        historyData = HistoryData.read(savedHistory)
        Log.v(
            Const.TAG,
            "History init ${historyData.historyCycles} " +
                    "since ${historyData.historyDate.formatDate()} rate=${historyData.historyRate}, " +
                    "unplugged ${historyData.cycleDate.formatDateTime()} at ${historyData.cycleLevel}%"
        )

        historyFlow.value = historyData
    }

    fun event(
        level: Int,
        plugged: Boolean
    ) {
        Log.v(Const.TAG, "History event ($level,$plugged)" +
                " now (${historyData.cycleLevel},${historyData.nowPlugged},${historyData.cycleDate.formatDateTime()})")

        val now = Clock.System.now()
        when {
            // Plugging in: recalculate historical rate
            plugged && !historyData.nowPlugged ->
                if (!historyData.cycleDate.isDistantPast) {
                    val discharge = historyData.cycleLevel - level
                    val duration = now - historyData.cycleDate
                    Log.v(Const.TAG, "History cycle $discharge% in ${duration.inWholeSeconds}s")
                    if (discharge >= 10 && duration.inWholeSeconds > 3600) {
                        val inDays = duration.inWholeSeconds.toFloat() / (3600 * 24)
                        val dischargeRate = discharge.toFloat() / inDays
                        val newRate =
                            if (historyData.historyDate.isDistantPast) dischargeRate
                            else (historyData.historyRate * historyData.historyCycles + dischargeRate) /
                                    (historyData.historyCycles + 1)
                        
                        historyData = historyData.set(
                            historyCycles = historyData.historyCycles + 1,
                            historyRate = newRate,
                            cycleRate = newRate,
                        )
                        
                        savedHistory.edit {
                            if (historyData.historyDate.isDistantPast) {
                                historyData = historyData.set(historyDate = historyData.cycleDate)
                                putLong(Const.HIST_INIT_DATE, historyData.cycleDate.epochSeconds)
                            }
                            putInt(Const.HIST_CYCLES, historyData.historyCycles)
                            putFloat(Const.HIST_RATE, historyData.historyRate)
                            putFloat(Const.CYCLE_RATE, historyData.cycleRate)
                            commit()
                        }
                        Log.v(Const.TAG, "History saved ${historyData.historyCycles+1} cycles rate=$newRate")
    
                        historyFlow.value = historyData
                    }
                }

            !plugged                           ->
                // Unplugged, re-starting cycle
                if (historyData.nowPlugged || level > historyData.cycleLevel) {
                    Log.v(Const.TAG, "History unplugged at $level > ${historyData.cycleLevel}")
                    
                    historyData = historyData.set(
                        cycleDate = now,
                        cycleLevel = level,
                    )
                    
                    savedHistory.edit {
                        putLong(Const.HIST_CYCLE_TIME, historyData.cycleDate.epochSeconds)
                        putInt(Const.HIST_CYCLE_LEVEL, historyData.cycleLevel)
                        commit()
                    }
    
                    historyFlow.value = historyData
                }
                
                // Continuing unplugged state
                else {
                    val discharge = historyData.cycleLevel - level
                    val duration = now - historyData.cycleDate
                    if (discharge >= 5 && duration.inWholeSeconds > 12 * 3600) {
                        val inDays = duration.inWholeSeconds.toFloat() / (3600 * 24)
                        val dischargeRate = discharge.toFloat() / inDays
                        
                        historyData = historyData.set(
                            cycleRate =
                                if (historyFlow.value.historyCycles > 0)
                                    (historyFlow.value.historyRate + dischargeRate) / 2f
                                else dischargeRate
                        )
                        
                        savedHistory.edit {
                            putFloat(Const.CYCLE_RATE, historyData.cycleRate)
                            commit()
                        }
                        
                        historyFlow.value = historyData
                    }
                }
        }
        
        // Changed plugged state
        if (historyData.nowPlugged != plugged) {
            Log.v(Const.TAG, "History plugged=$plugged")
            historyData = historyData.set(nowPlugged = plugged)
            savedHistory.edit {
                putBoolean(Const.HIST_PLUG_STATE, historyData.nowPlugged)
                commit()
            }
            historyFlow.value = historyData
        }
    }

    fun clear() {
        savedHistory.edit {
            putInt(Const.HIST_CYCLES, 0)
            putFloat(Const.HIST_RATE, 1f)
            putLong(Const.HIST_INIT_DATE, Instant.DISTANT_PAST.epochSeconds)
            commit()
        }
        historyFlow.value = historyFlow.value.set(
            historyDate = Instant.DISTANT_PAST,
            historyCycles = 0,
            historyRate = 0f,
        )
    }

    override fun toText(): String {
        return StringBuilder().apply {
            append("historyDate=${historyData.historyDate.formatDateTime()}\n")
            append("historyCycles=${historyData.historyCycles}\n")
            append("historyRate=${historyData.historyRate}\n")
            append("cycleDate=${historyData.cycleDate.formatDateTime()}\n")
            append("cycleLevel=${historyData.cycleLevel}\n")
            append("cycleRate=${historyData.cycleRate}\n")
            append("nowPlugged=${historyData.nowPlugged}\n")
        }.toString()
    }

    override fun fromText(text: String) {
        val elements = text.split("\n")
        elements.forEach {
            val keyvalue = it.split("=")
            if (keyvalue.size == 2) {
                when (keyvalue[0]) {
                    "historyDate" -> historyData = historyData.set(historyDate = parseDateTime(keyvalue[1]))
                    "historyCycles" -> historyData = historyData.set(historyCycles = keyvalue[1].toInt())
                    "historyRate" -> historyData = historyData.set(historyRate = keyvalue[1].toFloat())
                    "cycleDate" -> historyData = historyData.set(cycleDate = parseDateTime(keyvalue[1]))
                    "cycleLevel" -> historyData = historyData.set(cycleLevel = keyvalue[1].toInt())
                    "cycleRate" -> historyData = historyData.set(cycleRate = keyvalue[1].toFloat())
                    "nowPlugged" -> historyData = historyData.set(nowPlugged = keyvalue[1].toBoolean())
                }
            }
        }
        historyFlow.value = historyData
    }

    override val filenamePart = "History"
}
