# TODO

## Bugs

- Watchface: Quiet time not working
- Move ZenRule permissions to Permissions.kt

## Features

- Add instruction to exclude background management (ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
- Relay notifications by Dolbom (Kakaovoice to send alert to Watch)
- Manager disconnection
  - Use timer on Watch side before declaring connection closed
  - Use timer on Phone to detect disconnection (PING)

## Non-functional

- Use theme and colorize
- Remove hard-coded dimensions
- Move shared data to companion objects
- Reduce calls to Notifications.updateAllList()
