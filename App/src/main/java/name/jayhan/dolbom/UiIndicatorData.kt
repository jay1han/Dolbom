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

@Composable
fun DataDialog(
    indicatorCount: Int,
    onSave: () -> Unit,
    onLoad: () -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit
) {
    var doLoad by remember { mutableStateOf(false) }
    var doClear by remember { mutableStateOf(false) }

    if (doClear) {
        DataClearConfirmDialog(
            indicatorCount = indicatorCount,
            onConfirm = {
                onClear()
                doClear = false
            },
            onClose = { doClear = false }
        )
    }
    
    if (doLoad) {
        DataClearConfirmDialog(
            indicatorCount = indicatorCount,
            onConfirm = {
                onLoad()
                doLoad = false
            },
            onClose = { doLoad = false }
        )
    }
    
    Dialog(
        onDismissRequest = onClose
    ) {
        Card {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.indicators_configured).format(indicatorCount),
                    fontSize = Const.textSize,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                
                if (indicatorCount > 0) {
                    Button(
                        onClick = onSave,
                    ) {
                        Text(
                            text = stringResource(R.string.save_to_file),
                            fontSize = Const.textSize,
                        )
                    }
                }

                Button(
                    onClick = {
                        if (indicatorCount > 0) doLoad = true
                        else onLoad()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.load_from_file),
                        fontSize = Const.textSize,
                    )
                }

                if (indicatorCount > 0) {
                    Button(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .align(Alignment.End),
                        onClick = { doClear = true }
                    ) {
                        Text(
                            text = stringResource(R.string.clear_all),
                            fontSize = Const.textSize,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DataClearConfirmDialog(
    indicatorCount: Int,
    onConfirm: () -> Unit,
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose
    ) {
        Card {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.will_overwrite),
                    fontSize = Const.textSize,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                
                if (indicatorCount > 0) {
                    Text(
                        text = stringResource(R.string.indicators_configured).format(indicatorCount),
                        fontSize = Const.textSize,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }
                
                Text(
                    text = stringResource(R.string.data_clear_confirm),
                    fontSize = Const.titleSize,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(10.dp)
                        .align(Alignment.CenterHorizontally)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = onClose
                    ) {
                        Text(
                            text = stringResource(R.string.overwrite_no),
                            fontSize = Const.textSize,
                        )
                    }
                    Button(
                        onClick = {
                            onConfirm()
                            onClose()
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.overwrite_yes),
                            fontSize = Const.textSize,
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun DataDialogPreview() {
    DataDialog(
        10,
        {},
        {},
        {}
    ) {}
}

@Preview
@Composable
fun DataDialogEmpty() {
    DataDialog(
        0,
        {},
        {},
        {}
) {}
}

@Preview
@Composable
fun DataClearConfirmDialogPreview() {
    DataClearConfirmDialog(10, {}, {})
}
