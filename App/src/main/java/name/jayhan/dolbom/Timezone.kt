package name.jayhan.dolbom

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.absoluteValue

object Timezone
{
    private var minutes: Int = 0
    val tzFlow = MutableStateFlow("")
    
    fun fromString(
        context: Context,
        text: String
    ): String {
        if (text.isEmpty()) return makeString()
        val negative = (text[0] == '-')
        val split = (if (negative) text.substring(1) else text)
            .split('.')
    
        if (split.isNotEmpty()) {
            minutes =
                try {
                    if (split[0].isNotEmpty()) (split[0].toInt() * 60) else 0
                } catch (_: NumberFormatException) { 0 }
        }
        if (split.size >= 2) {
            if (split[1].isNotEmpty()) {
                try {
                    val decimal = split[1].toFloat() / 100f
                    minutes += (decimal * 60).toInt()
                } catch (_: NumberFormatException) {}
            }
        }
        if (minutes >= 60 * 24) minutes = 0
        if (negative) minutes = -minutes
    
        Pebble.sendIntent(context, MsgType.TZ) {
            putExtra(Const.EXTRA_TZ_MIN, minutes)
        }
    
        return makeString()
    }
    
    fun fromMinutes(tzMinutes: Int) {
        minutes = tzMinutes
        makeString()
    }
    
    private fun makeString(): String {
        val sign = if (minutes < 0) "-" else "+"
        val hours = minutes.absoluteValue / 60
        val frac = 100 * (minutes.absoluteValue - hours * 60) / 60
        val string = "$sign${hours}.$frac"
        tzFlow.value = string
        return string
    }
}

@Composable
fun UiTimezone(
    tzWatch: String,
    onApply: (String) -> String
) {
    var tz by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    BackHandler(editing) {
        focusManager.clearFocus()
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.timezone),
            fontSize = Const.titleSize,
        )

        Row (
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures { focusManager.clearFocus() }
                }
        ) {
            Box(
                modifier = Modifier.weight(1f)
            ) {
                BasicTextField(
                    readOnly = !editing,
                    value = if (editing) tz else tzWatch,
                    onValueChange = { tz = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                        .focusRequester(focusRequester)
                        .focusProperties { canFocus = editing }
                        .onFocusChanged { editing = it.hasFocus || it.isFocused },
                    textStyle = TextStyle(
                        fontSize = Const.titleSize,
                        lineHeight = Const.titleSize
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
    
                if (!editing) {
                    Text(
                        text = "",
                        fontSize = Const.titleSize,
                        lineHeight = Const.titleSize,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                            .alpha(0f)
                            .clickable {
                                editing = true
                                tz = ""
                                focusRequester.requestFocus()
                            }
                    )
                }
            }
    
            Button(
                onClick = {
                    editing = !editing
                    if (editing) {
                        tz = ""
                        focusRequester.requestFocus()
                    } else {
                        tz = onApply(tz)
                        focusManager.clearFocus()
                    }
                },
            ) {
                Text(
                    text =
                        if (editing) stringResource(R.string.apply)
                        else stringResource(R.string.edit),
                    fontSize = Const.textSize,
                )
            }
        }
    }
}

@Preview
@Composable
fun UiTimezonePreview(){
    UiTimezone("+8.0") { it }
}
