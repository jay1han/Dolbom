package name.jayhan.dolbom

import android.app.Activity
import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.service.notification.Condition
import android.service.notification.ZenPolicy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

class ZenRuleActivity: Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }
}

class ZenRule(
    private val context: Context
) {
    private val notiMan = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    private val zenUri = Uri.Builder()
        .scheme(Condition.SCHEME)
        .path("name.jayhan.dolbom")
        .query("dnd")
        .build()
    private val receiver = Receiver()
    var zenRuleId = ""
    var dndState = false
    var ruleEnabled = false
    
    private val zenPolicy = ZenPolicy.Builder()
        .disallowAllSounds()
        .allowAlarms(true)
        .allowCalls(ZenPolicy.PEOPLE_TYPE_STARRED)
        .showAllVisualEffects()
        .build()
    private val zenRule = AutomaticZenRule.Builder("Dolbom", zenUri)
        .setTriggerDescription(context.getString(R.string.dnd_description))
        .setType(AutomaticZenRule.TYPE_OTHER)
        .setManualInvocationAllowed(true)
        .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        .setEnabled(true)
        .setZenPolicy(zenPolicy)
        .setConfigurationActivity(ComponentName(context, ZenRuleActivity::class.java))
        .setIconResId(R.drawable.bom)
        .build()
    
    init {
        // TODO: move permission check into Permissions
        val filter = IntentFilter().apply {
            addAction(NotificationManager.ACTION_NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED)
            addAction(NotificationManager.ACTION_AUTOMATIC_ZEN_RULE_STATUS_CHANGED)
        }
        context.registerReceiver(receiver, filter,Context.RECEIVER_EXPORTED)
        
        if (!notiMan.isNotificationPolicyAccessGranted()) {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            )
        }
        
        for (rule in notiMan.automaticZenRules) {
            if (rule.value.name == "Dolbom") {
                notiMan.removeAutomaticZenRule(rule.key)
            }
        }

        if (zenRuleId == "") {
            try {
                zenRuleId = notiMan.addAutomaticZenRule(zenRule)
            } catch (_: Exception) {}
        } else {
            // notiMan.updateAutomaticZenRule(zenRuleId, zenRule)
            notiMan.setAutomaticZenRuleState(zenRuleId, makeCondition(false))
        }
        
        ruleEnabled = notiMan.getAutomaticZenRule(zenRuleId).isEnabled
        Pebble.dndEnabledFlow.value = ruleEnabled
    }
    
    fun deinit() {
        notiMan.removeAutomaticZenRule(zenRuleId)
        context.unregisterReceiver(receiver)
    }
    
    fun toggle() {
        val check = notiMan.getAutomaticZenRule(zenRuleId)
        if (!check.isEnabled) {
            notiMan.updateAutomaticZenRule(zenRuleId, zenRule)
            notiMan.setAutomaticZenRuleState(zenRuleId, makeCondition(dndState))
        }
        
        dndState = notiMan.getAutomaticZenRuleState(zenRuleId) != Condition.STATE_TRUE
        notiMan.setAutomaticZenRuleState(zenRuleId, makeCondition(dndState))
        dndState = notiMan.getAutomaticZenRuleState(zenRuleId) == Condition.STATE_TRUE
        Pebble.dndStateFlow.value = dndState
        sendToPebble()
    }
    
    fun read() {
        ruleEnabled = notiMan.getAutomaticZenRule(zenRuleId).isEnabled
        Pebble.dndEnabledFlow.value = ruleEnabled
        val ruleState = ruleEnabled &&
                notiMan.getAutomaticZenRuleState(zenRuleId) == Condition.STATE_TRUE
        if (dndState != ruleState) {
            dndState = ruleState
            Pebble.dndStateFlow.value = dndState
            sendToPebble()
        }
    }
    
    private fun sendToPebble() {
        Pebble.sendIntent(context, MsgType.PHONE_DND) {
            putExtra(Const.EXTRA_PHONE_DND, if (dndState) 1 else 0)
        }
    }
    
    fun refresh() {
        dndState = notiMan.getAutomaticZenRuleState(zenRuleId) == Condition.STATE_TRUE
        Pebble.dndStateFlow.value = dndState
        sendToPebble()
    }
    
    private fun makeCondition(state: Boolean): Condition {
        return Condition(
            zenUri,
            if (state) "Active" else "Inactive",
            if (state) Condition.STATE_TRUE else Condition.STATE_FALSE
        )
    }

    inner class Receiver: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                NotificationManager.ACTION_AUTOMATIC_ZEN_RULE_STATUS_CHANGED ->
                    read()
            }
        }
    }
}

@Composable
fun DndDialog(
    dndEnabled: Boolean,
    dndActive: Boolean,
    onClose: () -> Unit,
    onJump: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.modes_explanation),
                    fontSize = Const.textSize
                )
                
                Text(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    textAlign = TextAlign.Center,
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontSize = Const.textSize)) {
                            append("The rule is currently")
                        }
                        withStyle(SpanStyle(fontSize = Const.titleSize)) {
                            append("\n" +
                                    (if (dndEnabled) "Enabled" else "Disabled") +
                                    "\n"
                            )
                        }
                        withStyle(SpanStyle(fontSize = Const.textSize)) {
                            append("and")
                        }
                        withStyle(SpanStyle(fontSize = Const.titleSize)) {
                            append("\n"+
                                    (if (dndActive) "Active" else "Inactive")
                            )
                        }
                    }
                )
                
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Button(
                        onClick = onJump
                    ) {
                        Text(
                            text = stringResource(R.string.go_to_settings),
                            fontSize = Const.textSize
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun DndDialogPreview() {
    DndDialog(true, false, {}, {})
}
