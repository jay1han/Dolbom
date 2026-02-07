package name.jayhan.dolbom

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import name.jayhan.dolbom.ui.theme.PebbleTheme
import kotlin.time.Clock
import kotlin.time.isDistantPast

@Composable
fun BatteryDialog(
    watchInfo: WatchInfo,
    historyData: HistoryData,
    historyBackup: Backup,
    showToast: (String, Int) -> Unit,
    onClose: () -> Unit
) {
    fun loadSuccess(success: Boolean) {
        if (success) {
            showToast("History loaded", Toast.LENGTH_SHORT)
        } else showToast("Load failed", Toast.LENGTH_LONG)
    }

    fun saveSuccess(success: Boolean) {
        if (success) {
            showToast("History saved", Toast.LENGTH_SHORT)
        } else showToast("Error saving history", Toast.LENGTH_LONG)
    }

    Dialog(
        onDismissRequest = onClose
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                val batteryText =
                    if (watchInfo.plugged) {
                        stringResource(R.string.plugged) +
                                if (watchInfo.charging)
                                    stringResource(R.string.and_charging)
                                else ""
                    } else stringResource(R.string.unplugged)

                Text(
                    text = batteryText,
                    fontSize = Const.titleSize,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                val cycleText = StringBuilder()
                var clockNow by remember { mutableStateOf(Clock.System.now()) }
                
                if (!historyData.cycleDate.isDistantPast) {
                    cycleText.append(stringResource(R.string.format_cycle)
                        .format(historyData.cycleLevel,
                            historyData.cycleDate.formatTime(),
                            (clockNow - historyData.cycleDate).formatDurationMinutes()
                        ))
                    cycleText.append("\n")
                    val rate =
                        if (historyData.cycleRate > 0f) historyData.cycleRate
                        else historyData.historyRate

                    if (rate > 0f) {
                        cycleText.apply {
                            append(stringResource(R.string.format_rate)
                                .format(
                                    rate,
                                    90f / rate
                                ))
                            append("\n")
                        }
                        val estimate = (watchInfo.battery.toFloat() - 10f) / rate
                        if (estimate > 0) {
                            cycleText.append(stringResource(R.string.format_estimate).format(estimate))
                        } else {
                            cycleText.append(stringResource(R.string.low_estimate))
                        }
                    } else cycleText.append(stringResource(R.string.not_enough_data))
                } else cycleText.append(stringResource(R.string.no_past_data))

                Text(
                    text = cycleText.toString(),
                    fontSize = Const.smallSize,
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                if (historyData.historyCycles == 0) {
                    Button(
                        onClick = {
                            historyBackup.load(
                                onSuccess = { result ->
                                    loadSuccess(result)
                                }
                            )
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.restore_history),
                            fontSize = Const.textSize,
                        )
                    }

                } else {
                    Text(
                        text =
                            if (historyData.cycleRate > 0f) "History"
                            else stringResource(R.string.from_historical_data),
                        fontSize = Const.textSize,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val duration = Clock.System.now() - historyData.historyDate
                    val historyText = stringResource(R.string.format_data_since)
                        .format(
                            historyData.historyCycles,
                            historyData.historyDate.formatDate(),
                            duration.formatDuration()
                        ) +
                            if (historyData.cycleRate > 0f)
                                stringResource(R.string.history_format)
                                    .format(
                                        historyData.historyRate,
                                        90f / historyData.historyRate
                                    )
                            else ""
                    Text(
                        text = historyText,
                        fontSize = Const.smallSize,
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                historyBackup.save(
                                    onSuccess = { result ->
                                        saveSuccess(result)
                                    }
                                )
                            },
                        ) {
                            Text(
                                text = stringResource(R.string.history_backup),
                                fontSize = Const.textSize,
                            )
                        }
                        Button(
                            onClick = {
                                historyBackup.load(
                                    onSuccess = { result ->
                                        loadSuccess(result)
                                    }
                                )
                            },
                        ) {
                            Text(
                                text = stringResource(R.string.history_restore),
                                fontSize = Const.textSize,
                            )
                        }
                    }

                    Button(
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .align(Alignment.CenterHorizontally),
                        onClick = { History.clear() }
                    ) {
                        Text(
                            text = stringResource(R.string.history_clear_all),
                            fontSize = Const.textSize,
                        )
                    }
                }
            }
        }
    }
}

val PreviewHistoryData = HistoryData(
    historyDate = Clock.System.now(),
    historyCycles = 10,
    historyRate = 4.5f,
    cycleDate = Clock.System.now(),
    cycleLevel = 80,
    cycleRate =7.5f,
)

@Preview
@Composable
fun BatteryDialogPreview() {
    PebbleTheme {
        BatteryDialog(
            watchInfo = PreviewWatchInfo,
            historyData = PreviewHistoryData,
            historyBackup = Backup(History, LocalContext.current, ComponentActivity()),
            { _, _ -> {} }
        ) {}
    }
}

val EmptyHistoryData = HistoryData(
    historyDate = Clock.System.now(),
    historyCycles = 0,
    historyRate = 0f,
    cycleDate = Clock.System.now(),
    cycleLevel = 80,
    cycleRate =7.5f,
)

@Preview
@Composable
fun BatteryDialogEmpty() {
    PebbleTheme {
        BatteryDialog(
            watchInfo = PreviewWatchInfo,
            historyData = EmptyHistoryData,
            historyBackup = Backup(History, LocalContext.current, ComponentActivity()),
            { _, _ -> {} }
        ) {}
    }
}

val InsufficientHistoryData = HistoryData(
    historyDate = Clock.System.now(),
    historyCycles = 10,
    historyRate = 7.5f,
    cycleDate = Clock.System.now(),
    cycleLevel = 80,
    cycleRate = 0f
)

@Preview
@Composable
fun BatteryDialogInsufficient() {
    PebbleTheme {
        BatteryDialog(
            watchInfo = PreviewWatchInfo,
            historyData = InsufficientHistoryData,
            historyBackup = Backup(History, LocalContext.current, ComponentActivity()),
            { _, _ -> {} }
        ) {}
    }
}
