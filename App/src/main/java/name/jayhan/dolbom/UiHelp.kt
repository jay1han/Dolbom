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
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun HelpDialog(
    onClose: () -> Unit
) {
    var showDump by remember { mutableStateOf(false) }
    
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
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.built) + Const.buildDateTime,
                        fontSize = Const.smallSize
                    )
                    Button(
                        onClick = { showDump = true }
                    ) {
                        Text(
                            text = "Dump",
                            fontSize = Const.smallSize
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun HelpDialogPreview() {
    HelpDialog({})
}
