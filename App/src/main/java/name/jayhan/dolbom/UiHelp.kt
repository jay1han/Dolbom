package name.jayhan.dolbom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun HelpDialog(
    onClose: () -> Unit
) {
    var showDump by remember { mutableStateOf(false) }
    val dumpFlow by Notifications.dumpFlow.collectAsState(0)
    
    if (showDump) {
        DumpDialog(Notifications.dump) {
            showDump =false
        }
    }
    
    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.android_github),
                    fontSize = Const.smallSize,
                )
                Text(
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = Const.smallSize,
                    text = buildAnnotatedString {
                        withLink(
                            LinkAnnotation.Url(Const.GITHUB_ANDROID)
                        ) {
                            append(Const.GITHUB_ANDROID)
                        }
                    }
                )
                Text(
                    text = stringResource(R.string.pebble_github),
                    fontSize = Const.smallSize,
                )
                Text(
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = Const.smallSize,
                    text = buildAnnotatedString {
                        withLink(
                            LinkAnnotation.Url(Const.GITHUB_PEBBLE)
                        ) {
                            append(Const.GITHUB_PEBBLE)
                        }
                    }
                )
            
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.built) + Const.buildDateTime,
                        fontSize = Const.smallSize
                    )
                    Button(
                        onClick = { if (dumpFlow > 0) showDump = true }
                    ) {
                        Text(
                            text = "Dump %d".format(dumpFlow),
                            fontSize = Const.smallSize
                        )
                    }
                }
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
            modifier = Modifier.fillMaxSize().padding(0.dp)
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
                    Text(
                        notificationDump.channelId,
                        fontSize = Const.smallSize,
                        textAlign = TextAlign.End,
                        fontFamily = Const.condensedFont,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    notificationDump.extraMap.forEach { (filterType, extrasMap) ->
                        Text(
                            text = filterType.name,
                            fontSize = Const.smallSize,
                        )
                        extrasMap.forEach { (name, value) ->
                            if (value.isNotEmpty())
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                            append("$name : ")
                                        }
                                        append(value)
                                    },
                                    lineHeight = Const.subSize * 1.2,
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
            FilterType.Info to mapOf(
                "Info" to "Now"
            ),
            FilterType.Text to mapOf(
                "Text" to "World"
            ),
        )
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
