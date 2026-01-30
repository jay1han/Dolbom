# Dolbom[^1] companion (Android app)

This app, together with the related watchface and watchapp,
is intended to replace the combination of Tasker, Canvas, and PebbleTasker
that I personally used on the original Pebble (and Pebble Time),

Compared to the previous combination, this version has much fewer customizability
(no Takser! no Canvas!) but corresponds to the specific configuration I used[^2],
add adds a few functions unavailable before.

[^1]: The name comes from "dol", meaning "small rock" in Korean,
and "bom", which means "seeing". Together, the word "dolbom" also means
"caring for".

[^2]: I *may* be convinced to add functionality at a later date.

Limitations:

- Only supports square watches
- Requires Android 15 or above
- No color support (only Pebble 2 Duo)
- Only English
- Timezone adjustment is done "by hand"

# Features

## Time and Date

- Current date and time (hey, it's a timepiece)
- One other timezone

The "other" timezone must be configured by hand, relative to the local timezone.
The time difference is expressed in signed decimal, fractional hours.
For example, if you live in London (UTC+0:00) and want to show Bengaluru's time
(UTC+5:30), the difference is `+5.5` hours.
Saint-John's in Newfoundland, Canada (UTC-3:30), is at `-3.5` hours,
while Kathmandu, Nepal (UTC+5:45), is at `+5.75` hours.

## Power management

- Pebble battery level and estimate of remaining battery life
- Phone battery level and plugged/charging status

The "plugged" status means something is plugged that's capable of charging the phone.
Depending on the phone's charging policy, it may or may not be actually charging the battery,
at any speed, so the battery level may go down while plugged.

The app maintains a summarized history of charging cycles, which can be backed up to a text file.

## Phone status

- Telephony mode (4G, 5G, no service)
- SIM card (slot 1 or 2) and whether we're roaming
- Connected Wifi SSID
- Connected Bluetooth device name and battery level (if available)
- Indicator when the BT device is actually producing audio (A2DP or Headset)
- Indicator when Internet is unreachable

## Notifications

- Summary of active notifications

The watch shows a one-letter **indicator** for any active (not cleared) notification on the phone.
This is useful when you forget there was a notification.
You can assign a letter to each app you care about, so you know at a glance that 
there's a pending notification for that app.
Some filtering can be done according to the channel ID, as well as text fields of the notification.

Any additional notification that's not in the indicator list is displayed as a `+`.
This can further be filtered out by selectively ignoring apps.

Normally, local-only and ongoing notifications are ignored. This can be reverted
as a per-indicator switch.

Some apps remove their notification even when the user hasn't read and cleared it.
To keep a reminder in such a case, an indicator can be made "sticky".
Later removing a sticky indicator can be done with the Watchapp or in the Phone app.

Some notifications aren't properly shown on the Watch through the Pebble app.
A notification can be "relayed", i.e. an additional notification is generated
by Dolbom that is shown on the Watch. That notification can be set to repeat
continuously, until the original app's notification is cleared[^3].

[^3]: This feature was prompted by voice calls in Kakaotalk, which aren't
shown by the Pebble app, causing me to miss them sometimes.
Setting a repeating relay, I'm sure that my Watch will notify me continuously
until I either accept or decline the call.

There's no limit to the number of apps (Android packages) you can register.
Note that the watch face does have a limit of 15 simultaneous indicators,
which would probably overflow the width of the display anyway.
If there are more than 15 indicators to show, the app clips the list arbitrarily.

The indicator settings can be backed up and restored through an external text file.

A "dump" features lets you look at the raw notification,
to decide which fields of a notification you'd like to use as a filter.
Please read [Notifications.md](Notifications.md) for more detail.

## Watch-app features

- Phone's Do-not-disturb state and control
- Find my phone
- Clear sticky indicators

Due to the recent changes in Android's DND functionality,
it's no longer possible to enable or disable a system-wide "Quiet mode".
Instead, the app registers a normal, time-based, "Zen rule"
that can be toggled from the watch-app.
The enabled state of that mode is shown on the watch.
This feature is independent of the watch's Quiet time.

*Find my phone* does everything it can to help you locate the phone:

- Ring a melody at maximum volume
- Activate the flashlight

The obnoxious ringing can be disabled from the phone's notification screen
or from the watch by dismissing the notification.

Note: the Watchapp can be invoked on the watch by assigning it to a Quick Launch button.

## Permissions

Several Android permissions are required.
For simplicity, the permissions are requested at the app's first startup,
in a batch, and the app refuses to continue until all the permissions are granted.

