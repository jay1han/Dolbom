package name.jayhan.dolbom

import android.app.Notification
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import name.jayhan.dolbom.ui.theme.PebbleTheme
import kotlin.time.Clock

@Composable
fun HelpDialog(
    onClose: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp)
            ) {
                Image(
                    painterResource(R.drawable.dolbom),
                    contentDescription = "Dolbom",
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = Const.smallSize,
                    text = buildAnnotatedString {
                        pushStyle(SpanStyle(fontFamily = Const.condensedFont))
                        withLink(
                            LinkAnnotation.Url(Const.GITHUB)
                        ) {
                            append(Const.GITHUB)
                        }
                    }
                )
                
                Text(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    text = stringResource(R.string.built) + Const.buildDateTime,
                    textAlign = TextAlign.End,
                    fontSize = Const.smallSize
                )
            }
        }
    }
}

@Composable
fun DumpDialog(
    dump: List<NotificationDump>,
    onClose: () -> Unit
) {
    Dialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = {
            onClose()
        }
    ) {
        Card(
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(4.dp)
            ) {
                items(
                    items = dump,
                ) { notificationDump ->
                    Text(
                        notificationDump.packageName,
                        fontSize = Const.textSize,
                        textAlign = TextAlign.Start,
                        fontFamily = Const.condensedFont,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val flags = StringBuilder().apply {
                            if (notificationDump.flags.maskAll(Notification.FLAG_ONGOING_EVENT))
                                append("(ongoing)")
                            if (notificationDump.flags.maskAll(Notification.FLAG_LOCAL_ONLY))
                                append("(local)")
                        }.toString()

                        Text(
                            text = flags,
                            fontSize = Const.smallSize,
                            textAlign = TextAlign.Start,
                            fontFamily = Const.condensedFont,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Text(
                            notificationDump.channelId,
                            fontSize = Const.smallSize,
                            textAlign = TextAlign.End,
                            fontFamily = Const.condensedFont,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    notificationDump.extraMap.forEach { (filterType, extrasMap) ->
                        Text(
                            text = filterType.name,
                            fontSize = Const.smallSize,
                        )
                        extrasMap.forEach { (name, value) ->
                            if (value.isNotEmpty())
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(SpanStyle(
                                            fontWeight = FontWeight.Bold,
                                            textDecoration = TextDecoration.Underline,
                                        )) {
                                            append("$name : ")
                                        }
                                        append(value)
                                    },
                                    //lineHeight = Const.subSize * 1.2,
                                    fontSize = Const.subSize,
                                    fontFamily = Const.condensedFont
                                )
                        }
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                }
            }
        }
    }
}

@Composable
fun StatsDialog(
    onClose: () -> Unit,
    onReset: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose
    ){
        Card {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(20.dp)
            ) {
                Text(
                    text = "Stats since\n" +
                            PebbleStats.statsSince.formatDateTime() +
                            "\n(" +
                            (Clock.System.now() - PebbleStats.statsSince).formatDuration() +
                            ")",
                    fontSize = Const.textSize,
                )
                Text(
                    text = "%d packets sent\n%d received"
                        .format(PebbleStats.packetsSent, PebbleStats.packetsReceived),
                    fontSize = Const.textSize,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
                Text(
                    text = "%.1f packets/hour".format(PebbleStats.getAverage()),
                    fontSize = Const.textSize
                )
                
                Button(
                    onClick = onReset,
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Text(
                        text ="Reset data",
                        fontSize = Const.textSize
                    )
                }
            }
        }
    }
}

val PreviewDump = listOf(
    NotificationDump(
        "com.google",
        "channelId",
        mapOf(
            FilterType.Title to mapOf(
                "Title" to "Hello"
            ),
            FilterType.Subtitle to mapOf(
                "People" to "You"
            ),
            FilterType.Text to mapOf(
                "Info" to "Now"
            ),
            FilterType.Long to mapOf(
                "Text" to "World"
            ),
        ),
        Notification.FLAG_ONGOING_EVENT or Notification.FLAG_LOCAL_ONLY
    )
)

@Preview
@Composable
fun DumpDialogPreview() {
    DumpDialog(PreviewDump) {}
}

@Preview
@Composable
fun HelpDialogPreview() {
    HelpDialog({})
}

@Preview
@Composable
fun StatsDialogPreview() {
    PebbleTheme {
        StatsDialog({}, {})
    }
}
