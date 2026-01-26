package name.jayhan.dolbom

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import name.jayhan.dolbom.ui.theme.PebbleTheme
import kotlin.time.Clock
import kotlin.time.isDistantPast

@Composable
fun HistoryDialog(
    watchInfo: WatchInfo,
    historyData: HistoryData,
    onClose: () -> Unit
) {
    var confirmClear by remember { mutableStateOf(false) }

    if (confirmClear) {
        ClearBatteryDialog(
            historyData = historyData,
            onConfirm = { History.clear() }
        ) { confirmClear = false }
    }

    Dialog(
        onDismissRequest = onClose
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(20.dp)
            ) {
                val batteryText =
                    if (watchInfo.plugged) {
                        stringResource(R.string.plugged) +
                                if (watchInfo.charging)
                                    stringResource(R.string.and_charging)
                                else ""
                    }
                    else stringResource(R.string.unplugged)

                Text(
                    text = batteryText,
                    fontSize = Const.titleSize,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                val historyText = StringBuilder()
                var clockNow by remember { mutableStateOf(Clock.System.now()) }
                
                if (!historyData.cycleDate.isDistantPast) {
                    historyText.append(stringResource(R.string.format_cycle)
                        .format(historyData.cycleLevel,
                            historyData.cycleDate.formatTime(),
                            (clockNow - historyData.cycleDate).formatDurationMinutes()
                        ))
                    historyText.append("\n")
                    if (historyData.cycleRate > 0f) {
                        historyText.apply {
                            append(stringResource(R.string.format_rate)
                                .format(
                                    historyData.cycleRate,
                                    90f / historyData.cycleRate
                                ))
                            append("\n")
                        }
                        val estimate = (watchInfo.battery.toFloat() - 10f) / historyData.cycleRate
                        if (estimate > 0) {
                            historyText.append(stringResource(R.string.format_estimate).format(estimate))
                        } else {
                            historyText.append(stringResource(R.string.low_estimate))
                        }
                    } else historyText.append(stringResource(R.string.not_enough_data))
                } else historyText.append(stringResource(R.string.no_past_data))

                Text(
                    text = historyText.toString(),
                    fontSize = Const.smallSize,
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                if (historyData.historyDate.isDistantPast) {
                    Text(
                        text = stringResource(R.string.no_history),
                        textAlign = TextAlign.Center,
                        fontSize = Const.textSize,
                        modifier = Modifier.padding(8.dp)
                    )
                } else {
                    Button(
                        onClick = { confirmClear = true },
                    ) {
                        Text(
                            text = stringResource(R.string.clear_history),
                            fontSize = Const.textSize,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ClearBatteryDialog(
    historyData: HistoryData,
    onConfirm: () -> Unit,
    onExit: () -> Unit,
) {
    Dialog(
        onDismissRequest = { onExit() }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            ) {
                val duration = Clock.System.now() - historyData.historyDate
                val historyText = stringResource(R.string.format_data_since)
                    .format(
                        historyData.historyCycles,
                        historyData.historyDate.formatDate(),
                        duration.formatDuration()
                    )
                Text(
                    text = historyText,
                    fontSize = Const.textSize,
                    textAlign = TextAlign.Center,
                )
                if (historyData.historyRate > 0f) {
                    Text(
                        text = "Historical rate\n%.1f%%/day\n%.1f days/charge"
                            .format(historyData.historyRate,
                                90f/historyData.historyRate),
                        fontSize = Const.textSize,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }

                Text(
                    text = stringResource(R.string.clear_battery_history),
                    fontSize = Const.titleSize,
                    lineHeight = Const.titleSize * 1.2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            onConfirm()
                            onExit()
                        }
                    ) {
                        Text(
                            text = "Yes",
                            fontSize = Const.textSize
                        )
                    }
                    Button(
                        onClick = { onExit() }
                    ) {
                        Text(
                            text = "No",
                            fontSize = Const.textSize
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
fun HistoryDialogPreview() {
    PebbleTheme {
        HistoryDialog(
            watchInfo = PreviewWatchInfo,
            historyData = PreviewHistoryData,
        ) {}
    }
}

@Preview
@Composable
fun ConfirmClearPreview() {
    PebbleTheme {
        ClearBatteryDialog(
            historyData = PreviewHistoryData,
            onConfirm = {}
        ) { }
    }
}
