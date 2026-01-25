# TODO

## Bugs

- Watchface: Quiet time not working
- Test repeated relay
- Watchface doesn't report battery sometimes

## Tests

- Test repeated relay

## Features

- Notify when fully charged
- Export/Import

## Sub-functional
  
- Add instruction to exclude background management (ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
- Move ZenRule permissions to Permissions.kt
  - Add Permissions overall help screen

## Internal

- Separate PING and PONG messages
- Manage acked/unacked data sent
  - Apply to FRESH response
- Manage disconnection
  - Use timer on Watch side before declaring connection closed

## Non-functional

- Use theme and colorize
- Remove hard-coded dimensions
- Move shared data to companion objects
- Review all names
