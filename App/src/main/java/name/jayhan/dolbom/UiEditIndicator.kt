package name.jayhan.dolbom

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun EditIndicator(
    context: Context,
    indicator: SingleIndicator,
    activeList: List<String>,
    allList: List<String>,
    onClose: () -> Unit
) {
    var newPackage by remember { mutableStateOf(indicator.packageName) }
    var newChannel by remember { mutableStateOf(indicator.channel) }
    var newText by remember { mutableStateOf(indicator.filterText) }
    var newType by remember { mutableStateOf(indicator.filterType) }
    var newLetter by remember { mutableStateOf(indicator.letter) }
    var showPackageList by remember { mutableStateOf(false) }
    var ignore by remember { mutableStateOf(indicator.ignore) }
    var indicatorError by remember { mutableStateOf(false) }
    var sticky by remember { mutableStateOf(indicator.sticky) }
    var ongoing by remember { mutableStateOf(indicator.ongoing) }

    if (showPackageList) {
        SelectPackage(
            activeList = activeList,
            allList = allList,
            onClose = { showPackageList = false }
        ) { name: String -> newPackage = name }
        return
    }
    
    if (indicatorError) {
        IndicatorError { indicatorError = false }
    }
    
    Dialog(
        onDismissRequest = onClose,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
            ) {
                // PackageName
                var editPackageName by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value =
                        if (newPackage.isEmpty() && !editPackageName)
                            stringResource(R.string.package_empty)
                        else newPackage,
                    onValueChange = { newPackage = it },
                    textStyle = TextStyle(
                        fontSize = Const.textSize,
                        fontFamily = Const.condensedFont,
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        autoCorrectEnabled = false,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .onFocusChanged { editPackageName = it.isFocused },
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ){
                    // App icon
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth(.3f)
                    ) {
                        val icon: ImageBitmap? =
                            if (activeList == PreviewActiveList) null
                            else getApplicationIcon(LocalContext.current, newPackage)
                        if (icon != null) {
                            Image(
                                bitmap = icon,
                                contentDescription = newPackage,
                                alignment = Alignment.Center,
                                modifier = Modifier
                                    .clickable { showPackageList = true }
                            )
                        } else {
                            IconButton(
                                modifier = Modifier,
                                onClick = { showPackageList = true }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.outline_search_24),
                                    contentDescription = "Search",
                                    modifier = Modifier.scale(1.5f),
                                )
                            }
                        }
                    }

                    // Checkboxes
                    Column(
                        verticalArrangement = Arrangement.spacedBy(-16.dp),
                        modifier = Modifier.padding(8.dp),
                    ) {
                        // Ignore
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(-8.dp),
                            modifier = Modifier.padding(0.dp)
                        ) {
                            Checkbox(
                                modifier = Modifier.padding(0.dp),
                                checked = ignore,
                                onCheckedChange = { state ->
                                    ignore = state
                                    if (ignore) {
                                        newLetter = ' '
                                        sticky = false
                                    }
                                }
                            )
                            Text(
                                modifier = Modifier.padding(0.dp),
                                text = stringResource(R.string.ignore_indication),
                                fontSize = Const.textSize,
                                fontFamily = Const.condensedFont,
                            )
                        }
                        
                        // Sticky
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(-8.dp),
                            modifier = Modifier.padding(0.dp)
                        ) {
                            Checkbox(
                                modifier = Modifier.padding(0.dp),
                                checked = sticky,
                                onCheckedChange = { state ->
                                    sticky = state
                                    if (sticky) ignore = false
                                }
                            )
                            Text(
                                modifier = Modifier.padding(0.dp),
                                text = stringResource(R.string.sticky_indication),
                                fontSize = Const.textSize,
                            )
                        }
                        
                        // Ongoing
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(-8.dp),
                            modifier = Modifier.padding(0.dp)
                        ) {
                            Checkbox(
                                modifier = Modifier.padding(0.dp),
                                checked = ongoing,
                                onCheckedChange = { state ->
                                    ongoing = state
                                }
                            )
                            Text(
                                modifier = Modifier.padding(0.dp),
                                text = stringResource(R.string.ongoing_indication),
                                fontSize = Const.textSize,
                                fontFamily = Const.condensedFont,
                            )
                        }
                    }
                    
                    // Letter
                    OutlinedTextField(
                        enabled = !ignore,
                        value = if (ignore) " " else newLetter.toString(),
                        onValueChange = {
                            newLetter = if (ignore) ' ' else acceptLetter(it)
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = Const.titleSize,
                            textAlign = TextAlign.Center,
                            fontFamily = Const.condensedFont,
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            autoCorrectEnabled = false,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
                
                }

                // Channel
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.channel_filter),
                        fontSize = Const.textSize,
                        modifier = Modifier.padding(end = 10.dp)
                    )

                    var editChannel by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value =
                            if (newChannel.isEmpty() && !editChannel)
                                stringResource(R.string.filter_empty)
                            else newChannel,
                        onValueChange = { newChannel = it },
                        textStyle = TextStyle(
                            fontSize = Const.textSize,
                            fontWeight = FontWeight.Bold,
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            autoCorrectEnabled = false,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { editChannel = it.isFocused },
                        maxLines = 1,
                    )
                }

                // Filters
                Spacer(Modifier.height(8.dp))
                var editFilter by remember { mutableStateOf(false) }

                val labels = FilterType.Strings
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    labels.forEachIndexed { index, label ->
                        SegmentedButton(
                            onClick = { newType = FilterType.index(index) },
                            selected = index == newType.ordinal,
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = labels.size
                            )
                        ) {
                            Text(
                                text = stringResource(label),
                                maxLines = 1,
                                autoSize = TextAutoSize.StepBased()
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value =
                        if (newText.isEmpty() && !editFilter)
                            stringResource(R.string.filter_empty)
                        else newText,
                    onValueChange = { newText = it },
                    textStyle = TextStyle(
                        fontSize = Const.textSize,
                        fontStyle = FontStyle.Italic,
                    ),
                    keyboardOptions = KeyboardOptions(
                        autoCorrectEnabled = false,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { editFilter = it.isFocused },
                    maxLines = 1,
                )

                // Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            if (newLetter == ' ' && !ignore) indicatorError = true
                            else {
                                if (newPackage.isNotEmpty()) {
                                    Indicators.remove(indicator)
                                    Indicators.add(
                                        SingleIndicator(
                                            packageName = newPackage,
                                            channel = newChannel,
                                            filterText = newText,
                                            filterType = newType,
                                            letter = if (ignore) ' ' else newLetter,
                                            ignore = ignore,
                                            sticky = sticky,
                                            ongoing = ongoing,
                                        )
                                    )
                                    Notifications.refresh(context)
                                }
                                onClose()
                            }
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.save),
                            fontSize = Const.textSize
                        )
                    }
                    Button(
                        onClick = {
                            Indicators.remove(indicator)
                            Notifications.refresh(context)
                            onClose()
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.remove),
                            fontSize = Const.textSize
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IndicatorError(
    onExit: () -> Unit
) {
    Dialog(
        onDismissRequest = { onExit() }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
            ) {
                Text(
                    text = "Indicator must be a visible letter",
                    fontSize = Const.textSize,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                )
                Button(
                    onClick = onExit
                ) {
                    Text(
                        text = "OK",
                        fontSize = Const.textSize
                    )
                }
            }
        }
    }
}

fun acceptLetter(input: String): Char {
    if (input.isEmpty()) return ' '

    val letter = try {
        input.removePrefix(" ").removeSuffix(" ").last()
    } catch (_: NoSuchElementException) {
        ' '
    }
    if (letter.code >= '!'.code && letter.code <= '~'.code) return letter

    return ' '
}

@Preview
@Composable
fun EditIndicatorPreview() {
    EditIndicator(
        context = LocalContext.current,
        indicator = SingleIndicator(
            packageName = "com.android.google.apps.messaging",
            channel = "jayhan.dev",
            filterText = "Subtitle",
            filterType = FilterType.Subtitle,
            letter = 'S',
        ),
        activeList = PreviewActiveList,
        allList = listOf()
    ) { }
}

@Preview
@Composable
fun EditIndicatorIgnore() {
    EditIndicator(
        context = LocalContext.current,
        indicator = SingleIndicator(
            packageName = "com.android.google.apps.messaging",
            filterText = "long",
            filterType = FilterType.Long,
            ignore = true
        ),
        activeList = PreviewActiveList,
        allList = listOf()
    ) { }
}

@Preview
@Composable
fun EditIndicatorOngoing() {
    EditIndicator(
        context = LocalContext.current,
        indicator = SingleIndicator(
            ongoing = true,
            sticky = true,
            letter = 'A',
        ),
        activeList = PreviewActiveList,
        allList = listOf(),
) { }
}

@Preview
@Composable
fun IndicatorErrorPreview() {
    IndicatorError {}
}
