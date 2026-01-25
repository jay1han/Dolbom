@file:OptIn(ExperimentalMaterial3Api::class)

package name.jayhan.dolbom

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import name.jayhan.dolbom.Pebble.watchInfo
import name.jayhan.dolbom.ui.theme.PebbleTheme
import kotlin.time.Clock
import kotlin.time.Instant

@Composable
fun AppScaffold(
    context: Context
) {
    val watchInfo by Pebble.infoFlow.collectAsState(WatchInfo())
    val isConnected by Pebble.isConnected.collectAsState(false)
    val lastReceived by Pebble.lastReceived.collectAsState(Clock.System.now())
    val permissionsGranted by Permissions.grantFlow.collectAsState(Permissions.allGranted)
    val activeList by Notifications.activeFlow.collectAsState(emptyList())
    val allList by Notifications.allFlow.collectAsState(emptyList())
    val tzWatch by Timezone.tzFlow.collectAsState("")
    val indicators by Indicators.allFlow.collectAsState(listOf())
    val historyData by History.historyFlow.collectAsState(HistoryData())
    val dndActive by Pebble.dndStateFlow.collectAsState(false)
    val dndEnabled by Pebble.dndEnabledFlow.collectAsState(true)
    var showDnd by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var showWatch by remember { mutableStateOf(false) }

    if (!permissionsGranted) {
        PermissionsScaffold()

    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                MainTopBar(context, isConnected, watchInfo,
                    dndEnabled, dndActive,
                    onHelp = { showHelp = true },
                    onHistory = { showHistory = true },
                    onDnd = { showDnd = true },
                    onWatch = { showWatch = true }
                )
            },
        ) { innerPadding ->
            if (showHistory) {
                HistoryDialog(
                    watchInfo = watchInfo,
                    historyData = historyData,
                ) { showHistory = false }
            }
            
            if (showHelp) {
                HelpDialog { showHelp= false }
            }
            
            if (showDnd) {
                DndDialog(dndEnabled, dndActive,
                    onClose = { showDnd = false },
                    onJump = {
                        showDnd = false
                        context.startActivity(
                            Intent(Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                )
            }
            
            if (showWatch && isConnected) {
                WatchDialog(
                    context = context,
                    lastReceived = lastReceived,
                    tzWatch = tzWatch
                ) { showWatch = false }
            }

            MainPage(
                context = context,
                activeList = activeList,
                allList = allList,
                indicators = indicators,
                modifier = Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
            )
        }
    }
}

@Composable
fun MainTopBar(
    context: Context,
    isConnected: Boolean,
    watchInfo: WatchInfo,
    dndEnabled: Boolean,
    dndActive: Boolean,
    onHelp: ()-> Unit,
    onHistory: () -> Unit,
    onDnd: () -> Unit,
    onWatch:() -> Unit
) {
    TopAppBar(
        modifier = Modifier.fillMaxWidth(),
        navigationIcon = {
            Image(
                painterResource(R.drawable.dol),
                contentDescription = "Logo",
                modifier = Modifier
                    .height(50.dp)
                    .clickable { onHelp() }
            )
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp)
                    .clickable { onDnd() }
            ) {
                Icon(
                    painterResource(
                        if (!dndEnabled) R.drawable.outline_do_not_disturb_off_24
                        else if (dndActive) R.drawable.outline_do_not_disturb_on_total_silence_24
                        else R.drawable.outline_do_not_disturb_on_24
                    ),
                    contentDescription = "Do not disturb",
                    modifier = Modifier.padding(end = 12.dp).scale(1.5f)
                )
                
                Text(
                    text =
                        if (isConnected) watchInfo.modelString()
                        else stringResource(R.string.disconnected),
                    fontSize = Const.titleSize,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                        .clickable {
                            if(isConnected) onWatch()
                        }
                )
            }
        },
        actions = {
            if (isConnected) {
                Text(
                    text = "${watchInfo.battery}%",
                    fontSize = Const.titleSize,
                    modifier = Modifier.clickable { onHistory() }
                )
            } else {
                Icon(
                    painterResource(R.drawable.outline_refresh_24),
                    contentDescription = "Refresh",
                    modifier = Modifier.scale(1.5f)
                )
            }
        }
    )
}

@Composable
fun MainPage(
    context: Context,
    activeList: List<String>,
    allList: List<String>,
    indicators: List<SingleIndicator>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(8.dp)
    ){
        IndicatorList(
            context = context,
            activeList = activeList,
            allList = allList,
            indicators = indicators,
        )
    }
}

@Composable
fun WatchDialog(
    context: Context,
    tzWatch: String,
    lastReceived: Instant,
    onClose: () -> Unit
){
    Dialog(
        onDismissRequest = onClose
    ) {
        Card {
            Column(
                modifier = Modifier.fillMaxWidth().padding(10.dp)
            ) {
                Text(
                    text = watchInfo.modelString(),
                    fontSize = Const.titleSize,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "v" + watchInfo.versionString(),
                    fontSize = Const.textSize,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                var clockNow by remember { mutableStateOf(Clock.System.now()) }
                Text (
                    text = stringResource(R.string.format_last_contact)
                        .format(lastReceived.formatTimeSecond()),
                    fontSize = Const.textSize,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                Text (
                    text = stringResource(R.string.format_last_ago)
                        .format((clockNow - lastReceived).formatDurationSeconds()),
                    fontSize = Const.textSize,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                LaunchedEffect(clockNow) {
                    delay(1000)
                    clockNow = Clock.System.now()
                }

                UiTimezone(
                    tzWatch = tzWatch
                ) { tz ->
                    Timezone.fromString(context, tz)
                }
            }
        }
    }
}

val PreviewWatchInfo = WatchInfo(
    model = 1, version = 0x10000,
    battery = 100, plugged = true, charging = true
)

@Preview
@Composable
fun MainTopBarPreview() {
    PebbleTheme {
        MainTopBar(
            LocalContext.current,
            isConnected = true,
            watchInfo = PreviewWatchInfo,
            true, false,
            {},
            {},
            {},
            {}
        )
    }
}

@Preview
@Composable
fun MainTopBarDisconnected() {
    PebbleTheme {
        MainTopBar(
            LocalContext.current,
            isConnected = false,
            watchInfo = WatchInfo(),
            false, false,
            {},
            {},
            {},
            {}
        )
    }
}

@Preview
@Composable
fun MainPagePreview() {
    PebbleTheme {
        MainPage(
            context = LocalContext.current,
            activeList = PreviewActiveList,
            allList = PreviewAllList,
            indicators = PreviewIndicators,
        )
    }
}

@Preview
@Composable
fun ShowWatchPreview() {
    WatchDialog(
        context = LocalContext.current,
        lastReceived = Clock.System.now(),
        tzWatch = "+8.0"
    ) {}
}
