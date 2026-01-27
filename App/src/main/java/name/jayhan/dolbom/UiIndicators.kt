package name.jayhan.dolbom

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import name.jayhan.dolbom.ui.theme.PebbleTheme

@Composable
fun IndicatorList(
    context: Context,
    fileMan: FileManager,
    activeList: List<String>,
    allList: List<String>,
    indicators: List<SingleIndicator>,
    hasSticky: Boolean,
) {
    var editIndicator by remember { mutableStateOf(false) }
    var editedIndicator by remember { mutableStateOf(SingleIndicator()) }
    val scrollState = rememberScrollState()
    var dataDialog by remember { mutableStateOf(false) }
    var showDump by remember { mutableStateOf(false) }
    val dumpFlow by Notifications.dumpFlow.collectAsState(0)
    var showSticky by remember { mutableStateOf(false) }
    val backedUp by Indicators.backedUp.collectAsState(false)
    val indicatorCount by Indicators.count.collectAsState(0)

    if (showDump) {
        DumpScreen(Notifications.dump) {
            showDump = false
        }
    }
    
    if (editIndicator) {
        EditIndicator(
            context = context,
            indicator = editedIndicator,
            activeList = activeList,
            allList = allList
        ) {
            editIndicator = false
            editedIndicator = SingleIndicator()
        }
    }

    if (dataDialog) {
        DataDialog(
            indicatorCount = indicatorCount,
            backedUp = backedUp,
            onLoad = { fileMan.loadIndicators() },
            onSave = { fileMan.saveIndicators() },
            onClear = { Indicators.reset() },
        ) {
            dataDialog = false
        }
    }
    
    if (showSticky) {
        StickyDialog(
            stickies = Notifications.Accumulator.listSticky(),
            onClose = { showSticky = false },
            onClear = {
                context.sendBroadcast(Intent(Const.INTENT_CLEAR_STICKY))
            },
        )
    }
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            Button(
                onClick = { dataDialog = true },
            ) {
                Icon(
                    painterResource(R.drawable.outline_compare_arrows_24),
                    contentDescription = "Backup"
                )
            }
            
            OutlinedButton(
                onClick = { if (dumpFlow > 0) showDump = true },
                border = BorderStroke(1.dp,LocalContentColor.current),
                modifier = Modifier.padding(horizontal = 20.dp).weight(1f)
            ) {
                Row {
                    Icon(
                        painterResource(R.drawable.outline_lists_24),
                        contentDescription = "Dump",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = dumpFlow.toString(),
                        fontSize = Const.textSize
                    )
                }
            }
            
            Icon(
                painterResource(R.drawable.outline_check_circle_24),
                contentDescription = "Sticky",
                tint = if (hasSticky) LocalContentColor.current else Color.Transparent,
                modifier = Modifier.padding(horizontal = 24.dp)
                    .scale(1.5f)
                    .clickable {
                    if (hasSticky) showSticky = true
                }
            )
            
            Button(
                onClick = {
                    editedIndicator = SingleIndicator()
                    editIndicator = true
                },
            ) {
                Text(
                    text = "+",
                    fontSize = Const.textSize
                )
            }
        }

        if (indicators.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .verticalScroll(scrollState),
            ) {
                HorizontalDivider(thickness = 1.dp)

                for (indicator in indicators) {
                    IndicatorItem(
                        indicator,
                        true
                    ) {
                        editedIndicator = indicator
                        editIndicator = true
                    }
                    HorizontalDivider(thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
fun IndicatorItem(
    indicator: SingleIndicator,
    withFlags: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(.1f).padding(horizontal = 4.dp)
        ) {
            val icon = getApplicationIcon(LocalContext.current, indicator.packageName)
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = indicator.packageName,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = "?",
                    fontSize = Const.titleSize,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 8.dp).weight(1f)
        ) {
            val appName = Notifications.getApplicationName(indicator.packageName)
            if (appName != "") {
                Text(
                    text = appName,
                    fontSize = Const.textSize,
                    lineHeight = Const.textSize,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Text(
                text = indicator.packageName,
                fontSize = Const.subSize,
                lineHeight = Const.subSize,
                fontFamily = Const.condensedFont,
                modifier = Modifier.fillMaxWidth()
            )

            if (indicator.channelId.isNotEmpty()) {
                Text(
                    text = indicator.channelId,
                    fontSize = Const.subSize,
                    lineHeight = Const.subSize,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (indicator.filterText.isNotEmpty()) {
                val filterName = stringResource(indicator.filterType.r)
                Text(
                    text = "[$filterName] ${indicator.filterText}",
                    fontSize = Const.subSize,
                    fontStyle = FontStyle.Italic,
                    fontFamily = Const.condensedFont,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (withFlags &&
            (indicator.sticky || indicator.relay)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.outline_circle_28),
                    contentDescription = "",
                    tint = if (indicator.sticky) LocalContentColor.current else Color.Transparent
                )
                if (indicator.relay) {
                    if (indicator.repeat)
                        IndicatorFlagIcon(R.drawable.outline_keyboard_double_arrow_right_24)
                    else
                        IndicatorFlagIcon(R.drawable.outline_keyboard_arrow_right_24)
                }
            }
        }

        Spacer(modifier = Modifier.width(4.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.background(color = Const.colorIndicatorBack)
                .width(32.dp)
        ) {
            Text(
                text = if (indicator.ignore) "" else indicator.letter.toString(),
                fontSize = Const.titleSize,
                fontWeight = FontWeight.Bold,
                modifier = Modifier,
                textAlign = TextAlign.Center,
                color = Const.colorIndicatorLetter,
            )
        }
    }
}

@Composable
fun IndicatorFlagIcon(res: Int) {
    Icon(
        painter = painterResource(res),
        contentDescription = "Flag",
    )
}

@Composable
fun StickyDialog(
    stickies: List<SingleIndicator>,
    onClose: () -> Unit,
    onClear: () -> Unit,
) {
    val scrollState = rememberScrollState()
    
    Dialog(
        onDismissRequest = onClose,
    ) {
        Card(
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = "Sticky indicators",
                    fontSize = Const.titleSize,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                
                HorizontalDivider(thickness = 1.dp)
                Column(
                    modifier = Modifier.verticalScroll(scrollState)
                ){
                    stickies.forEach {
                        IndicatorItem(indicator = it, withFlags = false, onClick = {})
                    }
                }
                HorizontalDivider(thickness = 1.dp)
                
                Button(
                    onClick = {
                        onClear()
                        onClose()
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = "Clear all",
                        fontSize = Const.textSize
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun IndicatorItemPreview() {
    PebbleTheme {
        IndicatorItem(
            SingleIndicator(
                packageName = "com.google.android.apps.messaging",
                channelId = "jayhan.dev",
                filterText = "text",
                letter = 'S',
            ),
            true,
        ) { }
    }
}

@Preview
@Composable
fun IndicatorListPreview() {
    PebbleTheme {
        IndicatorList(
            context = LocalContext.current,
            fileMan = FileManager(LocalContext.current),
            activeList = PreviewActiveList,
            allList = PreviewAllList,
            indicators = PreviewIndicators,
            hasSticky = true,
        )
    }
}

@Preview
@Composable
fun IndicatorListEmpty() {
    PebbleTheme {
        IndicatorList(
            context = LocalContext.current,
            fileMan = FileManager(LocalContext.current),
            activeList = PreviewActiveList,
            allList = PreviewAllList,
            indicators = listOf(),
            hasSticky = false,
        )
    }
}

@Preview
@Composable
fun StickyDialogPreview() {
    StickyDialog(PreviewIndicators, {}) {}
}
